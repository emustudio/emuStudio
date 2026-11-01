/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.debug.*;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InstructionRecordingTest {
    private final InstructionRecording.Header header = new InstructionRecording.Header("word-cpu",
            List.of(new DebugCPU.Register("PC", 32), new DebugCPU.Register("ACC", 32)));

    private DebugCPU.Instruction event(long sequence) {
        return new DebugCPU.Instruction(sequence, 4, 2, "load =-123456", "LOAD -123456",
                Map.of("PC", 4L, "ACC", 0L), Map.of("PC", 6L, "ACC", 0xfffe1dc0L),
                List.of(new DebugCPU.Access(100, 2, -123456, true)), sequence % 3 != 0);
    }

    private Path destination() throws IOException {
        return Files.createTempDirectory("instruction-recording").resolve("run.emutrace");
    }

    @Test
    public void recordsWordValuesAndCapturedInstructionsWithBoundedOfflinePages() throws Exception {
        Path path = destination();
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(path, header)) {
            for (long i = 1; i <= 2505; i++) writer.append(event(i));
        }
        InstructionRecording.Reader.Cursor cursor;
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertEquals("word-cpu", reader.header().processor);
            assertEquals(32, reader.header().registers.get(1).bits);
            assertFalse(reader.complete());
            List<DebugCPU.Instruction> page = reader.nextPage(1024);
            assertEquals(1024, page.size());
            assertEquals(1, page.get(0).sequence);
            assertEquals("LOAD -123456", page.get(0).opcodes);
            assertEquals(-123456, page.get(0).accesses.get(0).value);
            assertEquals(2, page.get(0).accesses.get(0).bank);
            assertFalse(page.get(2).completed);
            cursor = reader.cursor();
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            reader.seek(cursor);
            assertEquals(1025, reader.nextPage(1024).get(0).sequence);
            List<DebugCPU.Instruction> tail = reader.nextPage(1024);
            assertEquals(457, tail.size());
            assertEquals(2505, tail.get(456).sequence);
            assertTrue(reader.complete());
            assertTrue(reader.finished());
            assertTrue(reader.nextPage(1024).isEmpty());
        }
    }

    @Test
    public void exactFullPageStillVerifiesFooterWithoutAnEmptyNextPage() throws Exception {
        Path path = destination();
        List<DebugCPU.Instruction> events = new ArrayList<>();
        for (int i = 1; i <= 1024; i++) events.add(event(i));
        InstructionRecording.write(path, header, events);
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertEquals(1024, reader.nextPage(1024).size());
            assertTrue(reader.complete());
            assertTrue(reader.finished());
        }
    }

    @Test
    public void retainsCompleteEventsAndReportsTruncatedOrMissingFooter() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(10), event(11)));
        byte[] full = Files.readAllBytes(path);
        for (int cut : new int[]{full.length - 17, full.length - 20}) {
            Path truncated = path.resolveSibling("cut-" + cut);
            Files.write(truncated, Arrays.copyOf(full, cut));
            try (InstructionRecording.Reader reader = new InstructionRecording.Reader(truncated)) {
                List<DebugCPU.Instruction> events = reader.nextPage(100);
                assertFalse(events.isEmpty());
                assertTrue(reader.finished());
                assertFalse(reader.complete());
                assertTrue(reader.status().contains("Incomplete"));
            }
        }
    }

    @Test
    public void rejectsChangedFileBetweenPagesAndUnknownVersion() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(1), event(2)));
        InstructionRecording.Reader.Cursor cursor;
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            reader.nextPage(1);
            cursor = reader.cursor();
        }
        Files.write(path, new byte[]{1}, StandardOpenOption.APPEND);
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertThrows(IOException.class, () -> reader.seek(cursor));
            assertThrows(IOException.class, () -> reader.nextPage(100)); // Trailing data invalidates footer.
        }
        byte[] bytes = Files.readAllBytes(path);
        bytes[7] = 99;
        Files.write(path, bytes);
        assertThrows(IOException.class, () -> new InstructionRecording.Reader(path));
    }

    @Test
    public void writerFailsWithoutClobberingExistingRecordingAndRejectsReorderedEvents() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(5)));
        byte[] original = Files.readAllBytes(path);
        assertThrows(FileAlreadyExistsException.class, () -> new InstructionRecording.Writer(path, header));
        assertArrayEquals(original, Files.readAllBytes(path));
        Path failed = path.resolveSibling("failed.emutrace");
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(failed, header)) {
            writer.append(event(2));
            assertThrows(IOException.class, () -> writer.append(event(1)));
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(failed)) {
            assertEquals(1, reader.nextPage(10).size());
            assertFalse(reader.complete());
        }
    }

    @Test
    public void fileFailurePropagatesAndCloseStillReleasesResource() throws Exception {
        class FailingOutput extends OutputStream {
            boolean fail, closed;
            @Override public void write(int value) throws IOException {
                if (fail) throw new IOException("disk full");
            }
            @Override public void close() { closed = true; }
        }
        FailingOutput output = new FailingOutput();
        InstructionRecording.Writer writer = new InstructionRecording.Writer(output, header);
        writer.append(event(1));
        output.fail = true;
        assertThrows(IOException.class, writer::close);
        assertTrue(output.closed);
        assertThrows(IOException.class, () -> writer.append(event(2)));
    }

    @Test
    public void wholeFileTextExportIncludesEveryPageAndItsCompleteness() throws Exception {
        Path source = destination(), text = source.resolveSibling("all.txt");
        List<DebugCPU.Instruction> events = new ArrayList<>();
        for (int i = 1; i <= 2049; i++) events.add(event(i));
        InstructionRecording.write(source, header, events);
        InstructionRecording.exportText(text, source);
        String content = Files.readString(text);
        assertTrue(content.contains("#1 |"));
        assertTrue(content.contains("#2049 |"));
        assertTrue(content.contains("Complete recording: 2049 instructions"));
        assertEquals(2049, content.lines().filter(line -> line.startsWith("#")).count());
        assertThrows(FileAlreadyExistsException.class, () -> InstructionRecording.exportText(source, source));
    }

    @Test
    public void readableExportIncludesAllCapturedState() throws Exception {
        Path path = destination();
        InstructionRecording.exportText(path, header, List.of(event(7)));
        String text = Files.readString(path);
        assertTrue(text.contains("word-cpu"));
        assertTrue(text.contains("#7 | bank=2 PC=0004 | LOAD -123456 | load =-123456"));
        assertTrue(text.contains("before: ACC=0 PC=4"));
        assertTrue(text.contains("after: ACC=FFFE1DC0 PC=6"));
        assertTrue(text.contains("WRITE bank=2:0064 value=FFFE1DC0"));
    }

    @Test
    public void sessionStreamsBeyondRecentBoundAndFlushesOnClose() throws Exception {
        DebugMemory memory = new DebugMemory(32, 1, 0);
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.disassemble(anyInt())).thenReturn(new DisassembledInstruction(0, "nop", "00"));
        DebugProbe probe = new DebugProbe("custom-cpu", memory, disassembler);
        probe.register("PC", 32, () -> 0, v -> {});
        CPU cpu = mock(CPU.class);
        when(cpu.getDebugger()).thenReturn(Optional.of(probe));
        Path path = destination();
        try (DebugSession session = new DebugSession(cpu, memory, null)) {
            session.record(2);
            session.recordTo(path);
            for (int i = 0; i < 20; i++) { probe.begin(0); probe.end(true); }
            assertEquals(2, session.history().size());
            session.record(0);
            assertEquals(2, session.history().size()); // Stopping does not erase evidence.
            assertTrue(probe.isActive()); // File capture remains independent of recent capture.
            for (int i = 0; i < 3; i++) { probe.begin(0); probe.end(true); }
        }
        assertFalse(probe.isActive());
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertEquals(23, reader.nextPage(100).size());
            assertTrue(reader.complete());
        }
    }
}
