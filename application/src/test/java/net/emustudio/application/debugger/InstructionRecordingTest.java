/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class InstructionRecordingTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void savedInstructionRetainsPhysicalLocationRegistersAccessesAndFailureState() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("failed-write.emutrace");
        InstructionRecording.Header header = new InstructionRecording.Header(
                "Z80", List.of(new DebugCPU.Register("PC", 16), new DebugCPU.Register("A", 8)));
        DebugCPU.Instruction failedWrite = new DebugCPU.Instruction(7, 0x1234, "LD (HL),A", "77", Map.of("PC", 0x1234L, "A", 0xffL), Map.of("PC", 0x1235L, "A", 0xffL), List.of(new DebugCPU.Access(0x4000, 0xff, true),
                        new DebugCPU.Access(0x1234, 0x77, false)), false);

        InstructionRecording.write(recording, header, List.of(failedWrite));

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertEquals("Z80", reader.header().processor);
            assertEquals(2, reader.header().registers.size());
            assertEquals("PC", reader.header().registers.get(0).name);
            assertEquals(16, reader.header().registers.get(0).bits);
            assertFalse(reader.complete());
            assertFalse(reader.finished());

            List<DebugCPU.Instruction> page = reader.nextPage(10);

            assertEquals(1, page.size());
            DebugCPU.Instruction restored = page.get(0);
            assertEquals(7, restored.sequence);
            assertEquals(0x1234, restored.address);

            assertEquals("LD (HL),A", restored.mnemonic);
            assertEquals("77", restored.opcodes);
            assertEquals(Map.of("PC", 0x1234L, "A", 0xffL), restored.before);
            assertEquals(Map.of("PC", 0x1235L, "A", 0xffL), restored.after);
            assertFalse(restored.completed);
            assertEquals(2, restored.accesses.size());
            DebugCPU.Access write = restored.accesses.get(0);
            assertEquals(0x4000, write.address);

            assertEquals(0xff, write.value);
            assertTrue(write.write);
            DebugCPU.Access fetch = restored.accesses.get(1);
            assertEquals(0x1234, fetch.address);

            assertEquals(0x77, fetch.value);
            assertFalse(fetch.write);
            assertTrue(reader.complete());
            assertTrue(reader.finished());
            assertEquals("Complete recording: 1 instructions", reader.status());
            assertTrue(reader.nextPage(10).isEmpty());
        }
    }

    @Test
    public void filteredPagesCanBeRevisitedWithoutLosingSkippedInstructionsFromFooterCount() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("pages.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("8080", List.of()),
                List.of(instruction(1, 0x10), instruction(2, 0x20), instruction(3, 0x10),
                        instruction(4, 0x20), instruction(5, 0x10)));

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            List<DebugCPU.Instruction> firstPage = reader.nextPage(1, event -> event.address == 0x10);
            InstructionRecording.Reader.Cursor secondPage = reader.cursor();
            assertEquals(1, firstPage.get(0).sequence);
            assertFalse(reader.finished());

            assertEquals(3, reader.nextPage(1, event -> event.address == 0x10).get(0).sequence);
            assertEquals(5, reader.nextPage(1, event -> event.address == 0x10).get(0).sequence);
            assertTrue(reader.complete());
            assertEquals("Complete recording: 5 instructions", reader.status());

            reader.seek(secondPage);

            assertFalse(reader.finished());
            assertFalse(reader.complete());
            List<DebugCPU.Instruction> remaining = reader.nextPage(10);
            assertEquals(3, remaining.size());
            assertEquals(3, remaining.get(0).sequence);
            assertEquals(4, remaining.get(1).sequence);
            assertEquals(5, remaining.get(2).sequence);
            assertTrue(reader.complete());
        }
    }

    @Test
    public void emptyRecordingBecomesCompleteAfterReadingItsFooter() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("empty.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM", List.of()), List.of());

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertTrue(reader.nextPage(1).isEmpty());
            assertTrue(reader.complete());
            assertEquals("Complete recording: 0 instructions", reader.status());
        }
    }

    @Test
    public void cursorFromAnotherRecordingIsRejected() throws Exception {
        Path first = temporary.getRoot().toPath().resolve("first.emutrace");
        Path second = temporary.getRoot().toPath().resolve("second.emutrace");
        InstructionRecording.Header header = new InstructionRecording.Header("RAM", List.of());
        InstructionRecording.write(first, header, List.of());
        InstructionRecording.write(second, header, List.of());

        try (InstructionRecording.Reader firstReader = new InstructionRecording.Reader(first);
             InstructionRecording.Reader secondReader = new InstructionRecording.Reader(second)) {
            IOException failure = assertThrows(IOException.class, () -> secondReader.seek(firstReader.cursor()));

            assertTrue(failure.getMessage().contains("Recording changed"));
        }
    }

    @Test
    public void missingFooterPreservesReadableInstructionsButReportsIncompleteRecording() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("missing-footer.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM", List.of()),
                List.of(instruction(1, 0x10), instruction(2, 0x20)));
        try (RandomAccessFile file = new RandomAccessFile(recording.toFile(), "rw")) {
            // Remove only the completion marker and its two long counters.
            file.setLength(file.length() - 17);
        }

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertEquals(2, reader.nextPage(10).size());
            assertTrue(reader.finished());
            assertFalse(reader.complete());
            assertTrue(reader.status().contains("Incomplete recording"));
            assertTrue(reader.status().contains("after 2 instructions"));
        }
    }

    @Test
    public void truncatedLastInstructionDoesNotHideEarlierCompleteInstructions() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("truncated-event.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM", List.of()),
                List.of(instruction(1, 0x10), instruction(2, 0x20)));
        try (RandomAccessFile file = new RandomAccessFile(recording.toFile(), "rw")) {
            // Remove footer and the final instruction's access-count field.
            file.setLength(file.length() - 21);
        }

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            List<DebugCPU.Instruction> readable = reader.nextPage(10);

            assertEquals(1, readable.size());
            assertEquals(1, readable.get(0).sequence);
            assertTrue(reader.finished());
            assertFalse(reader.complete());
            assertTrue(reader.status().contains("after 1 instructions"));
        }
    }

    @Test
    public void incorrectFooterCountersAreReportedAsInvalid() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("bad-footer.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM", List.of()),
                List.of(instruction(1, 0x10)));
        try (RandomAccessFile file = new RandomAccessFile(recording.toFile(), "rw")) {
            file.seek(file.length() - 16);
            file.writeLong(2);
        }

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            IOException failure = assertThrows(IOException.class, () -> reader.nextPage(10));

            assertTrue(failure.getMessage().contains("footer does not match"));
            assertTrue(reader.finished());
            assertFalse(reader.complete());
            assertTrue(reader.status().startsWith("Invalid recording:"));
        }
    }

    @Test
    public void truncatedHeaderIsRejectedWhenOpening() throws Exception {
        Path recording = temporary.newFile("bad-header.emutrace").toPath();
        Files.write(recording, new byte[]{0x45, 0x4d});

        IOException failure = assertThrows(IOException.class, () -> new InstructionRecording.Reader(recording));

        assertEquals("Invalid or incomplete recording header", failure.getMessage());
    }

    @Test
    public void repeatedSequenceFailsWriterAndLeavesReadablePrefixIncomplete() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("duplicate-sequence.emutrace");
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(
                recording, new InstructionRecording.Header("RAM", List.of()))) {
            writer.append(instruction(4, 0x10));

            IOException duplicate = assertThrows(IOException.class, () -> writer.append(instruction(4, 0x20)));
            IOException failedWriter = assertThrows(IOException.class, () -> writer.append(instruction(5, 0x30)));

            assertTrue(duplicate.getMessage().contains("sequence is not increasing"));
            assertEquals("Recording is closed or failed", failedWriter.getMessage());
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertEquals(1, reader.nextPage(10).size());
            assertTrue(reader.finished());
            assertFalse(reader.complete());
        }
    }

    @Test
    public void closingWriterTwiceDoesNotAddAnotherFooterAndPreventsFurtherAppends() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("closed.emutrace");
        InstructionRecording.Writer writer = new InstructionRecording.Writer(
                recording, new InstructionRecording.Header("RAM", List.of()));
        writer.close();

        writer.close();
        assertThrows(IOException.class, () -> writer.append(instruction(1, 0x10)));

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertTrue(reader.nextPage(1).isEmpty());
            assertTrue(reader.complete());
        }
    }

    @Test
    public void writerNeverOverwritesAnExistingFile() throws Exception {
        Path recording = temporary.newFile("existing.emutrace").toPath();
        Files.writeString(recording, "keep this recording");

        assertThrows(IOException.class, () -> new InstructionRecording.Writer(
                recording, new InstructionRecording.Header("RAM", List.of())));

        assertEquals("keep this recording", Files.readString(recording));
    }

    @Test
    public void outputFailureOnCloseIsPropagatedAndStreamIsClosed() throws Exception {
        FailingOutputStream output = new FailingOutputStream();
        InstructionRecording.Writer writer = new InstructionRecording.Writer(
                output, new InstructionRecording.Header("RAM", List.of()));
        writer.append(instruction(1, 0x10));
        output.failWrites = true;

        IOException failure = assertThrows(IOException.class, writer::close);

        assertEquals("disk full", failure.getMessage());
        assertTrue(output.closed);
    }

    @Test
    public void headerRejectsMissingProcessorDuplicateNamesAndInvalidWidths() {
        assertThrows(IllegalArgumentException.class,
                () -> new InstructionRecording.Header(" ", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new InstructionRecording.Header("Z80", List.of(
                        new DebugCPU.Register("A", 8), new DebugCPU.Register("A", 8))));
        assertThrows(IllegalArgumentException.class,
                () -> new InstructionRecording.Header("Z80", List.of(new DebugCPU.Register("A", 0))));
        assertThrows(IllegalArgumentException.class,
                () -> new InstructionRecording.Header("Z80", List.of(new DebugCPU.Register("A", 65))));
    }

    @Test
    public void pageSizeMustRemainWithinSupportedBounds() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("page-size.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM", List.of()), List.of());

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertThrows(IllegalArgumentException.class, () -> reader.nextPage(0));
            assertThrows(IllegalArgumentException.class, () -> reader.nextPage(10001));
            assertTrue(reader.nextPage(1).isEmpty());
            assertTrue(reader.complete());
        }
    }

    @Test
    public void interruptedScanLeavesReaderAvailableForLaterUse() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("interrupted.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM", List.of()),
                List.of(instruction(1, 0x10)));

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            Thread.currentThread().interrupt();
            try {
                assertThrows(InterruptedIOException.class, () -> reader.nextPage(10));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
            assertEquals(1, reader.nextPage(10).size());
            assertTrue(reader.complete());
        }
    }

    @Test
    public void textExportIncludesEventsRegistersAccessesAndRecordingCompleteness() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("export.emutrace");
        Path text = temporary.getRoot().toPath().resolve("export.txt");
        DebugCPU.Instruction write = new DebugCPU.Instruction(1, 0x20, "STORE", "AA", Map.of("B", 2L, "A", 1L), Map.of("B", 3L, "A", 1L), List.of(new DebugCPU.Access(0x30, 0xff, true)), true);
        InstructionRecording.write(recording,
                new InstructionRecording.Header("test-cpu", List.of(new DebugCPU.Register("A", 8))),
                List.of(write));

        InstructionRecording.exportText(text, recording);

        String exported = Files.readString(text);
        assertTrue(exported.contains("emuStudio instruction recording v2 | test-cpu"));
        assertTrue(exported.contains("A:8"));
        assertTrue(exported.contains("#1 | PC=0020 | AA | STORE | completed"));
        assertTrue(exported.contains("before: A=1 B=2"));
        assertTrue(exported.contains("after: A=1 B=3"));
        assertTrue(exported.contains("WRITE 0030 value=FF"));
        assertTrue(exported.contains("Complete recording: 1 instructions"));
    }

    @Test
    public void textExportNeverOverwritesExistingOutput() throws Exception {
        Path text = temporary.newFile("existing.txt").toPath();
        Files.writeString(text, "keep this export");

        assertThrows(IOException.class, () -> InstructionRecording.exportText(text,
                new InstructionRecording.Header("RAM", List.of()), List.of()));

        assertEquals("keep this export", Files.readString(text));
    }

    private static DebugCPU.Instruction instruction(long sequence, int address) {
        return new DebugCPU.Instruction(sequence, address, "NOP", "00", Map.of(), Map.of(), List.of(), true);
    }

    private static final class FailingOutputStream extends OutputStream {
        private boolean failWrites;
        private boolean closed;

        @Override
        public void write(int value) throws IOException {
            if (failWrites) throw new IOException("disk full");
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
