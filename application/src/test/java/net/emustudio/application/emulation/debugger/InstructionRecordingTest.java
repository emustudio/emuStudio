/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.debugger.NavLink;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ViewRow;
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
import java.util.OptionalInt;

import static org.junit.Assert.*;

public class InstructionRecordingTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void savedStepRetainsSymbolicValuesGroupsChangesNavigationAndFailureState() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("failed-write.emutrace");
        InstructionRecording.Event failed = new InstructionRecording.Event(7, false,
                new StepView("STORE input", "store/input", OptionalInt.of(0x1234), List.of(
                        new ViewRow("Tape", "Current cell", "hello", "world", true,
                                List.of("ram/write/cell"), List.of(new NavLink("Find earlier write", "ram/write/cell"))),
                        new ViewRow("Machine", "Mode", "symbolic", "symbolic", false, List.of(), List.of()))));

        InstructionRecording.write(recording, new InstructionRecording.Header("Abstract RAM"), List.of(failed));

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertEquals("Abstract RAM", reader.header().processor);
            assertFalse(reader.complete());
            assertFalse(reader.finished());
            List<InstructionRecording.Event> page = reader.nextPage(10);
            assertEquals(1, page.size());
            InstructionRecording.Event restored = page.get(0);
            assertEquals(7, restored.sequence);
            assertFalse(restored.completed);
            assertEquals("STORE input", restored.view.summary);
            assertEquals("store/input", restored.view.foldKey);
            assertEquals(0x1234, restored.view.programLocation.getAsInt());
            assertEquals(2, restored.view.rows.size());
            ViewRow cell = restored.view.rows.get(0);
            assertEquals("Tape", cell.group);
            assertEquals("Current cell", cell.label);
            assertEquals("hello", cell.before);
            assertEquals("world", cell.after);
            assertTrue(cell.changed);
            assertEquals(List.of("ram/write/cell"), cell.tags);
            assertEquals("Find earlier write", cell.links.get(0).label);
            assertEquals("ram/write/cell", cell.links.get(0).tag);
            assertFalse(restored.view.rows.get(1).changed);
            assertTrue(reader.complete());
            assertTrue(reader.finished());
            assertEquals("Complete recording: 1 instructions", reader.status());
            assertTrue(reader.nextPage(10).isEmpty());
        }
    }

    @Test
    public void filteredPagesCanBeRevisitedWithoutLosingSkippedInstructionsFromFooterCount() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("pages.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("8080"),
                List.of(instruction(1, 0x10), instruction(2, 0x20), instruction(3, 0x10),
                        instruction(4, 0x20), instruction(5, 0x10)));

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            List<InstructionRecording.Event> firstPage = reader.nextPage(1, event -> event.view.programLocation.orElse(-1) == 0x10);
            InstructionRecording.Reader.Cursor secondPage = reader.cursor();
            assertEquals(1, firstPage.get(0).sequence);
            assertFalse(reader.finished());

            assertEquals(3, reader.nextPage(1, event -> event.view.programLocation.orElse(-1) == 0x10).get(0).sequence);
            assertEquals(5, reader.nextPage(1, event -> event.view.programLocation.orElse(-1) == 0x10).get(0).sequence);
            assertTrue(reader.complete());
            assertEquals("Complete recording: 5 instructions", reader.status());

            reader.seek(secondPage);

            assertFalse(reader.finished());
            assertFalse(reader.complete());
            List<InstructionRecording.Event> remaining = reader.nextPage(10);
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
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"), List.of());

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
        InstructionRecording.Header header = new InstructionRecording.Header("RAM");
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
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"),
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
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"),
                List.of(instruction(1, 0x10), instruction(2, 0x20)));
        try (RandomAccessFile file = new RandomAccessFile(recording.toFile(), "rw")) {
            // Remove footer and the final instruction's access-count field.
            file.setLength(file.length() - 21);
        }

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            List<InstructionRecording.Event> readable = reader.nextPage(10);

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
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"),
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
                recording, new InstructionRecording.Header("RAM"))) {
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
                recording, new InstructionRecording.Header("RAM"));
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
                recording, new InstructionRecording.Header("RAM")));

        assertEquals("keep this recording", Files.readString(recording));
    }

    @Test
    public void outputFailureOnCloseIsPropagatedAndStreamIsClosed() throws Exception {
        FailingOutputStream output = new FailingOutputStream();
        InstructionRecording.Writer writer = new InstructionRecording.Writer(
                output, new InstructionRecording.Header("RAM"));
        writer.append(instruction(1, 0x10));
        output.failWrites = true;

        IOException failure = assertThrows(IOException.class, writer::close);

        assertEquals("disk full", failure.getMessage());
        assertTrue(output.closed);
    }

    @Test
    public void abortKeepsCompleteEventsButNeverWritesACompletionFooter() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("capture-failed.emutrace");
        InstructionRecording.Writer writer = new InstructionRecording.Writer(recording, new InstructionRecording.Header("RAM"));
        writer.append(instruction(1, 0));

        writer.abort();
        writer.close();

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertEquals(1, reader.nextPage(10).size());
            assertTrue(reader.finished());
            assertFalse(reader.complete());
            assertTrue(reader.status().contains("Incomplete recording"));
        }
    }

    @Test
    public void headerRejectsMissingOrOversizedProcessor() {
        assertThrows(IllegalArgumentException.class, () -> new InstructionRecording.Header(" "));
        assertThrows(IllegalArgumentException.class,
                () -> new InstructionRecording.Header("a".repeat(65536 + 1)));
    }

    @Test
    public void pageSizeMustRemainWithinSupportedBounds() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("page-size.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"), List.of());

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
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"),
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
    public void textExportIncludesSymbolicRowsNavigationAndCompleteness() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("export.emutrace");
        Path text = temporary.getRoot().toPath().resolve("export.txt");
        InstructionRecording.Event write = new InstructionRecording.Event(1, true,
                new StepView("STORE", "store", OptionalInt.of(0x20), List.of(
                        new ViewRow("Tape", "Current cell", "hello", "world", true,
                                List.of("write/current"), List.of(new NavLink("Earlier", "write/current"))))));
        InstructionRecording.write(recording, new InstructionRecording.Header("test-cpu"), List.of(write));

        InstructionRecording.exportText(text, recording);

        String exported = Files.readString(text);
        assertTrue(exported.contains("emuStudio instruction recording v4 | test-cpu"));
        assertTrue(exported.contains("#1 | PC=0020 | STORE | completed"));
        assertTrue(exported.contains("Tape"));
        assertTrue(exported.contains("Current cell: hello → world [changed]"));
        assertTrue(exported.contains("tag: write/current"));
        assertTrue(exported.contains("Earlier -> earlier write/current"));
        assertTrue(exported.contains("Complete recording: 1 instructions"));
    }

    @Test
    public void offlineNavigationUsesTheOwningOrdinalAndOpaqueTags() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("navigation.emutrace");
        ViewRow write = new ViewRow("Tape", "Write", "one", "two", true,
                List.of("symbol/write/current"), List.of(new NavLink("Earlier", "symbol/write/current")));
        InstructionRecording.write(recording, new InstructionRecording.Header("Uninstalled symbolic machine"), List.of(
                new InstructionRecording.Event(10, true, new StepView("STORE", "same", OptionalInt.empty(), List.of(write))),
                new InstructionRecording.Event(11, true, new StepView("STORE", "same", OptionalInt.empty(), List.of(write)))));

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            List<InstructionRecording.Event> events = reader.nextPage(10);
            NavLink secondLink = events.get(1).view.rows.get(0).links.get(0);
            assertEquals(10, InstructionRecording.resolveEvent(events, 11, secondLink).getAsLong());
            assertTrue(InstructionRecording.resolveEvent(events, 10, secondLink).isEmpty());
            assertTrue(InstructionRecording.resolveEvent(events, 11, new NavLink("Missing", "other/tag")).isEmpty());
        }
    }

    @Test
    public void oversizedEventFailsBeforeWritingAnUnboundedRecord() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("oversized.emutrace");
        ViewRow large = new ViewRow("Details", "Large value", "a".repeat(65536),
                "b".repeat(65536), true, List.of(), List.of());
        InstructionRecording.Event event = new InstructionRecording.Event(1, true,
                new StepView("Large", "", OptionalInt.empty(), java.util.Collections.nCopies(100, large)));
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(recording, new InstructionRecording.Header("RAM"))) {
            IOException failure = assertThrows(IOException.class, () -> writer.append(event));
            assertTrue(failure.getMessage().contains("byte limit"));
        }
        assertTrue(Files.size(recording) < 1024);
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertTrue(reader.nextPage(1).isEmpty());
            assertFalse(reader.complete());
        }
    }

    @Test
    public void oversizedEncodedRecordIsRejectedBeforeAllocatingItsPayload() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("bad-size.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"), List.of(instruction(1, 0)));
        try (RandomAccessFile file = new RandomAccessFile(recording.toFile(), "rw")) {
            file.seek(8);
            file.skipBytes(4 + 3); // Length-prefixed processor text.
            file.skipBytes(1 + 8); // Event marker and sequence.
            file.writeInt(Integer.MAX_VALUE);
        }

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            IOException failure = assertThrows(IOException.class, () -> reader.nextPage(1));
            assertTrue(failure.getMessage().contains("event bytes"));
        }
    }

    @Test
    public void codecRejectsTooManyRowsOnWriteAndRead() throws Exception {
        ViewRow row = new ViewRow("State", "Value", "a", "b", true, List.of(), List.of());
        InstructionRecording.Event oversized = new InstructionRecording.Event(1, true,
                new StepView("Step", "", OptionalInt.empty(), java.util.Collections.nCopies(4097, row)));

        assertTrue(assertThrows(IOException.class, () -> InstructionRecording.sizeOf(oversized))
                .getMessage().contains("presentation rows"));

        Path recording = temporary.getRoot().toPath().resolve("bad-row-count.emutrace");
        InstructionRecording.write(recording, new InstructionRecording.Header("RAM"), List.of(instruction(1, 0)));
        try (RandomAccessFile file = new RandomAccessFile(recording.toFile(), "rw")) {
            file.seek(file.length() - 17 - 4);
            file.writeInt(4097);
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            assertTrue(assertThrows(IOException.class, () -> reader.nextPage(1))
                    .getMessage().contains("presentation rows"));
        }
    }

    @Test
    public void codecRejectsTooManyTags() {
        ViewRow row = new ViewRow("State", "Value", "a", "b", true,
                java.util.Collections.nCopies(257, "tag"), List.of());
        InstructionRecording.Event event = new InstructionRecording.Event(1, true,
                new StepView("Step", "", OptionalInt.empty(), List.of(row)));

        assertTrue(assertThrows(IOException.class, () -> InstructionRecording.sizeOf(event))
                .getMessage().contains("search tags"));
    }

    @Test
    public void codecRejectsTooManyLinks() {
        ViewRow row = new ViewRow("State", "Value", "a", "b", true, List.of(),
                java.util.Collections.nCopies(257, new NavLink("Earlier", "tag")));
        InstructionRecording.Event event = new InstructionRecording.Event(1, true,
                new StepView("Step", "", OptionalInt.empty(), List.of(row)));

        assertTrue(assertThrows(IOException.class, () -> InstructionRecording.sizeOf(event))
                .getMessage().contains("navigation links"));
    }

    @Test
    public void codecRejectsOversizedTextAndInvalidLocationsFromLiveCapture() {
        InstructionRecording.Event text = new InstructionRecording.Event(1, true,
                new StepView("x".repeat(65537), "", OptionalInt.empty(), List.of()));
        InstructionRecording.Event location = new InstructionRecording.Event(1, true,
                new StepView("Step", "", OptionalInt.of(-1), List.of()));

        assertThrows(IOException.class, () -> InstructionRecording.sizeOf(text));
        assertThrows(IOException.class, () -> InstructionRecording.sizeOf(location));
    }

    @Test
    public void pageByteLimitAppliesEvenWhenTheRequestedCountIsLarge() throws Exception {
        Path recording = temporary.getRoot().toPath().resolve("large-pages.emutrace");
        ViewRow large = new ViewRow("Details", "Value", "a".repeat(65536),
                "b".repeat(65536), true, List.of(), List.of());
        StepView view = new StepView("Large step", "", OptionalInt.empty(), java.util.Collections.nCopies(49, large));
        try (InstructionRecording.Writer writer = new InstructionRecording.Writer(recording, new InstructionRecording.Header("RAM"))) {
            for (int i = 1; i <= 6; i++) writer.append(new InstructionRecording.Event(i, true, view));
        }

        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(recording)) {
            List<InstructionRecording.Event> first = reader.nextPage(10000);
            assertEquals(5, first.size());
            assertFalse(reader.finished());
            List<InstructionRecording.Event> second = reader.nextPage(10000);
            assertEquals(1, second.size());
            assertEquals(6, second.get(0).sequence);
            assertTrue(reader.complete());
        }
    }

    @Test
    public void textExportNeverOverwritesExistingOutput() throws Exception {
        Path text = temporary.newFile("existing.txt").toPath();
        Files.writeString(text, "keep this export");

        assertThrows(IOException.class, () -> InstructionRecording.exportText(text,
                new InstructionRecording.Header("RAM"), List.of()));

        assertEquals("keep this export", Files.readString(text));
    }

    private static InstructionRecording.Event instruction(long sequence, int address) {
        return new InstructionRecording.Event(sequence, true,
                new StepView("NOP", "00/NOP", OptionalInt.of(address), List.of()));
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
