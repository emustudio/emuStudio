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
    public void filteredPagesFindMatchesBeyondFirstRawPageAndPreserveSeekCursors() throws Exception {
        Path path = destination();
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(path, header)) {
            for (int i = 1; i <= 2052; i++) writer.append(event(i));
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            InstructionRecording.Reader.Cursor start = reader.cursor();
            List<DebugCPU.Instruction> first = reader.nextPage(2, event -> event.sequence > 2048);
            assertEquals(2, first.size());
            assertEquals(2049, first.get(0).sequence);
            assertEquals(2050, first.get(1).sequence);
            assertFalse(reader.finished());
            InstructionRecording.Reader.Cursor next = reader.cursor();

            reader.seek(start);
            assertEquals(2049, reader.nextPage(2, event -> event.sequence > 2048).get(0).sequence);
            reader.seek(next);
            List<DebugCPU.Instruction> last = reader.nextPage(2, event -> event.sequence > 2048);
            assertEquals(2, last.size());
            assertEquals(2051, last.get(0).sequence);
            assertEquals(2052, last.get(1).sequence);
            assertTrue(reader.complete());
            assertTrue(reader.finished());
            assertEquals("Complete recording: 2052 instructions", reader.status());
        }
    }

    @Test
    public void noMatchesScanWholeRecordingAndValidateAllEventCounts() throws Exception {
        Path path = destination();
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(path, header)) {
            for (int i = 1; i <= 2052; i++) writer.append(event(i));
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertTrue(reader.nextPage(1, event -> false).isEmpty());
            assertTrue(reader.complete());
            assertTrue(reader.finished());
            assertEquals("Complete recording: 2052 instructions", reader.status());
        }
    }

    @Test
    public void fullFilteredPageScansTrailingNonmatchesAndVerifiesFooter() throws Exception {
        Path path = destination();
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(path, header)) {
            for (int i = 1; i <= 2052; i++) writer.append(event(i));
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            List<DebugCPU.Instruction> page = reader.nextPage(1024, event -> event.sequence <= 1024);
            assertEquals(1024, page.size());
            assertEquals(1024, page.get(1023).sequence);
            assertTrue(reader.complete());
            assertTrue(reader.finished());
            assertEquals("Complete recording: 2052 instructions", reader.status());
        }
    }

    @Test
    public void fullFilteredPageLeavesCursorBeforeNextMatchAfterSkippedEvents() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(1), event(2), event(3), event(4), event(5)));
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertEquals(1, reader.nextPage(1, event -> event.sequence == 1 || event.sequence == 4).get(0).sequence);
            assertFalse(reader.finished());
            InstructionRecording.Reader.Cursor next = reader.cursor();
            assertEquals(4, reader.nextPage(1).get(0).sequence);
            reader.seek(next);
            assertEquals(4, reader.nextPage(1, event -> event.sequence == 4).get(0).sequence);
            assertTrue(reader.complete());
            assertTrue(reader.finished());
            assertEquals("Complete recording: 5 instructions", reader.status());
        }
    }

    @Test
    public void noMatchesStillReportsMissingFooter() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(1), event(2)));
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
            file.setLength(file.length() - 17);
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertTrue(reader.nextPage(1, event -> false).isEmpty());
            assertTrue(reader.finished());
            assertFalse(reader.complete());
            assertTrue(reader.status().contains("Incomplete"));
        }
    }

    @Test
    public void filteredScanCancellationPreservesCursorAndCompletenessState() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(1), event(2), event(3)));
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            try {
                assertThrows(InterruptedIOException.class, () -> reader.nextPage(1, event -> {
                    Thread.currentThread().interrupt();
                    return false;
                }));
                assertTrue(Thread.currentThread().isInterrupted());
                assertFalse(reader.finished());
                assertFalse(reader.complete());
                assertTrue(reader.status().contains("unverified"));
            } finally {
                Thread.interrupted();
            }
            assertEquals(2, reader.nextPage(1).get(0).sequence);
            assertEquals(3, reader.nextPage(1).get(0).sequence);
            assertTrue(reader.complete());
        }
    }

    @Test
    public void filteredScanPropagatesPredicateFailuresWithoutInvalidatingRecording() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(1), event(2)));
        IllegalStateException failure = new IllegalStateException("filter failed");
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> reader.nextPage(1, event -> { throw failure; })));
            assertFalse(reader.finished());
            assertEquals(2, reader.nextPage(1).get(0).sequence);
            assertTrue(reader.complete());
        }
    }

    @Test
    public void filteredScanRejectsInvalidSkippedSequenceAndMismatchedFooter() throws Exception {
        Path path = destination();
        InstructionRecording.write(path, header, List.of(event(1), event(2)));
        byte[] valid = Files.readAllBytes(path);
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
            file.readInt();
            file.readInt();
            file.readUTF();
            int registers = file.readInt();
            for (int i = 0; i < registers; i++) {
                file.readUTF();
                file.readInt();
            }
            assertEquals(1, file.readUnsignedByte());
            file.writeLong(0);
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertThrows(IOException.class, () -> reader.nextPage(1, event -> false));
            assertFalse(reader.complete());
            assertTrue(reader.status().contains("sequence"));
        }
        Files.write(path, valid);
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
            file.seek(file.length() - 16);
            file.writeLong(1);
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertThrows(IOException.class, () -> reader.nextPage(1, event -> false));
            assertFalse(reader.complete());
            assertTrue(reader.status().contains("footer"));
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
