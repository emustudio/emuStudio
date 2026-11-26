/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.debugger.NavLink;
import net.emustudio.emulib.plugin.debugger.PackedCapture;
import net.emustudio.emulib.plugin.debugger.PackedStep;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ViewRow;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class InstructionRecordingTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void packedFramesRoundTripWithDerivedRegistersAndOrderedAccesses() throws Exception {
        PackedCapture capture = new PackedCapture("8080", PackedStep.AccessFormat.BYTE);
        AtomicInteger b = new AtomicInteger(1);
        capture.register("B", 1, b::get);
        capture.register("C", 1, () -> 2);
        capture.registerPair("BC", "B", "C");
        capture.register("SP", 2, () -> 0xFFFF);
        capture.before();
        b.set(2);
        capture.access(0x1234, 0xFF, true);
        capture.access(0x1234, 0xFF, false);
        StepView view = capture.after(0x100, true, "INR B", "04");
        capture.clearAccesses();
        capture.access(0, 0, false);
        b.set(0);
        for (int i = 0; i < 2000; i++) {
            capture.before();
            capture.after(0x100, true, "INR B", "04");
        }
        assertEquals("258", view.rows.get(3).before);
        InstructionRecording.Event first = new InstructionRecording.Event(1, true, view);
        InstructionRecording.Event second = new InstructionRecording.Event(2, false, view);
        Path path = temporary.getRoot().toPath().resolve("packed.emutrace");
        InstructionRecording.write(path, new InstructionRecording.Header("8080"), List.of(first, second));
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            List<InstructionRecording.Event> events = reader.nextPage(10);
            assertTrue(reader.complete());
            StepView restored = events.get(0).view;
            assertNotNull(restored.packed());
            assertEquals("INR B", restored.summary);
            assertEquals(view.foldKey, restored.foldKey);
            assertEquals(OptionalInt.of(0x100), restored.programLocation);
            assertEquals("258", restored.rows.get(3).before);
            assertEquals("514", restored.rows.get(3).after);
            assertEquals("65535", restored.rows.get(4).after);
            assertEquals("Write @1234", restored.rows.get(5).label);
            assertEquals("255", restored.rows.get(5).after);
            assertEquals("Read @1234", restored.rows.get(6).label);
            assertEquals(1L, InstructionRecording.resolveEvent(events, 2, restored.rows.get(6).links.get(0)).orElseThrow());
            assertFalse(events.get(1).completed);
        }
    }

    @Test
    public void packedFramesPreserveWideAddressesSignedWordsAndTapeValues() throws Exception {
        PackedCapture bytes = new PackedCapture("BrainDuck", PackedStep.AccessFormat.WIDE_BYTE);
        bytes.before();
        bytes.access(0x10002, 255, true);
        StepView wide = bytes.after(null, 0x10000);

        PackedCapture words = new PackedCapture("RASP", PackedStep.AccessFormat.WORD);
        words.before();
        words.access(-4, Integer.MIN_VALUE, true);
        StepView word = words.after(null, 0);

        PackedCapture tapes = new PackedCapture("RAM", List.of("input", "storage"));
        AtomicReference<Object> symbol = new AtomicReference<>(25);
        tapes.inspect("R0", symbol::get);
        tapes.before();
        symbol.set("25");
        String text = "λ".repeat(9_000);
        tapes.tapeAccess("storage", 2, -12, text, true, true);
        tapes.tapeAccess("input", 2, text, text, false, false);
        StepView tape = tapes.after(null, 1);

        Path path = temporary.getRoot().toPath().resolve("packed-values.emutrace");
        InstructionRecording.write(path, new InstructionRecording.Header("test"),
                List.of(new InstructionRecording.Event(1, true, wide), new InstructionRecording.Event(2, true, word),
                        new InstructionRecording.Event(3, true, tape)));
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            List<InstructionRecording.Event> events = reader.nextPage(10);
            assertTrue(reader.complete());
            assertEquals("Write @10002", events.get(0).view.rows.get(1).label);
            assertEquals("255", events.get(0).view.rows.get(1).after);
            assertEquals("Write @-004", events.get(1).view.rows.get(1).label);
            assertEquals(Integer.toString(Integer.MIN_VALUE), events.get(1).view.rows.get(1).after);
            assertEquals(1L, InstructionRecording.resolveEvent(events, 4,
                    new NavLink("Earlier", "BrainDuck/write/65538")).orElseThrow());
            assertEquals(2L, InstructionRecording.resolveEvent(events, 4,
                    new NavLink("Earlier", "RASP/write/-4")).orElseThrow());
            List<ViewRow> rows = events.get(2).view.rows;
            assertEquals("25", rows.get(1).before);
            assertEquals("25", rows.get(1).after);
            assertTrue(rows.get(1).changed);
            assertEquals("Write storage[2]", rows.get(2).label);
            assertEquals("-12", rows.get(2).before);
            assertEquals(text, rows.get(2).after);
            assertEquals("Read input[2]", rows.get(3).label);
            assertEquals(text, rows.get(3).before);
            assertFalse(rows.get(3).changed);
            assertEquals(3L, InstructionRecording.resolveEvent(events, 4,
                    new NavLink("Earlier", "RAM/storage/write/2")).orElseThrow());
            assertTrue(InstructionRecording.resolveEvent(events, 4, rows.get(3).links.get(0)).isEmpty());
        }
    }

    @Test
    public void readsExistingVersionFourRecordings() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("version-four.emutrace");
        InstructionRecording.Event event = instruction(1, 0x10);
        InstructionRecording.write(path, new InstructionRecording.Header("RAM"), List.of(event));
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
            file.seek(Integer.BYTES);
            file.writeInt(4);
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertEquals(event.view.summary, reader.nextPage(10).get(0).view.summary);
            assertTrue(reader.complete());
        }
    }

    @Test
    public void pagesPreserveRetainedOrderAfterWrapAndStartingAnotherRecording() throws Exception {
        try (InstructionRecording recording = new InstructionRecording(true, () -> "RAM", () -> 256)) {
            recording.start(new InstructionRecording.Options(InstructionRecording.Mode.LAST, 300, null));
            InstructionRecording.Recording first = recording.current();
            for (int i = 0; i < 400; i++) recording.capture(true, Optional.of(instruction(i + 1, 0).view));

            recording.start(new InstructionRecording.Options(InstructionRecording.Mode.FIRST, 1, null));
            assertEquals(List.of(first, recording.current()), recording.recordings());
            InstructionRecording.Page latest = recording.page(first, InstructionRecording.LATEST_PAGE);
            InstructionRecording.Page older = recording.page(first, latest.previous);
            assertEquals(101L, older.events.get(0).sequence);
            assertEquals(250L, older.events.get(older.events.size() - 1).sequence);
            assertEquals(251L, latest.events.get(0).sequence);
            assertEquals(400L, latest.events.get(latest.events.size() - 1).sequence);
            assertEquals(latest.start, older.next);
            assertEquals(-1, older.previous);
            assertEquals(-1, latest.next);
        }
    }

    @Test
    public void continuousRecordingPagesTheFileAndHonorsRangeAndPause() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("continuous.emutrace");
        InstructionRecording recording = new InstructionRecording(true, () -> "RAM", () -> 256);
        InstructionRecording.Recording saved;
        try (recording) {
            recording.prepare(path, new InstructionRecording.AddressRange(10, 20));
            saved = recording.recordings().get(0);
            assertTrue(saved.isPending());
            assertFalse(recording.isRecording());
            assertFalse(Files.exists(path));
            recording.startPrepared(saved);
            assertSame(saved, recording.current());
            recording.capture(true, Optional.of(instruction(1, 0).view));
            recording.setPaused(true);
            recording.capture(true, Optional.of(instruction(1, 10).view));
            recording.setPaused(false);
            for (int i = 0; i < 251; i++) recording.capture(true, Optional.of(instruction(1, 20).view));
            assertTrue(recording.events().isEmpty());
        }
        assertTrue(saved.isComplete());
        InstructionRecording.Page latest = recording.page(saved, InstructionRecording.LATEST_PAGE);
        assertEquals(253L, latest.events.get(0).sequence);
        assertEquals(3L, recording.page(saved, latest.previous).events.get(0).sequence);
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            assertEquals(251, reader.nextPage(1000).size());
            assertTrue(reader.complete());
        }
        try (InstructionRecording library = new InstructionRecording(false, () -> "Other CPU", () -> 0)) {
            library.open(path);
            InstructionRecording.Recording reopened = library.recordings().get(0);
            assertTrue(reopened.isComplete());
            assertFalse(library.isRecording());
            InstructionRecording.Page reopenedLatest = library.page(reopened, InstructionRecording.LATEST_PAGE);
            assertEquals(253L, reopenedLatest.events.get(0).sequence);
            assertEquals(3L, library.page(reopened, reopenedLatest.previous).events.get(0).sequence);
            Path copy = path.resolveSibling("copy.emutrace");
            library.save(reopened, copy, false);
            assertArrayEquals(Files.readAllBytes(path), Files.readAllBytes(copy));
        }
    }

    @Test
    public void fileCaptureContinuesAfterHistoryStopsAndKeepsObserverSequenceNumbers() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("independent-lifetimes.emutrace");
        try (InstructionRecording recording = new InstructionRecording(true, () -> "RAM", () -> 256)) {
            recording.start(new InstructionRecording.Options(InstructionRecording.Mode.LAST, 2, null));
            recording.capture(true, Optional.empty());
            recording.capture(true, Optional.of(instruction(1, 0x10).view));
            recording.recordTo(path);
            recording.capture(false, Optional.of(instruction(1, 0x20).view));
            recording.stop();
            recording.capture(true, Optional.of(instruction(1, 0x30).view));
            assertTrue(recording.requiresFullCapture());

            recording.stopFile();

            assertFalse(recording.requiresFullCapture());
            assertNull(recording.path());
            assertEquals(List.of(2L, 3L), recording.events().stream()
                    .map(event -> event.sequence).collect(java.util.stream.Collectors.toList()));
        }
        try (InstructionRecording.Reader reader = new InstructionRecording.Reader(path)) {
            List<InstructionRecording.Event> saved = reader.nextPage(10);
            assertEquals(2, saved.size());
            assertEquals(3, saved.get(0).sequence);
            assertFalse(saved.get(0).completed);
            assertEquals(4, saved.get(1).sequence);
            assertEquals(0x30, saved.get(1).view.programLocation.getAsInt());
            assertTrue(reader.complete());
        }
    }

    @Test
    public void failedFileStartPreservesHistoryAndFreshCaptureClearsItsError() throws Exception {
        Path existing = temporary.newFile("keep.emutrace").toPath();
        Files.writeString(existing, "keep existing recording");
        try (InstructionRecording recording = new InstructionRecording(true, () -> "RAM", () -> 256)) {
            recording.start(new InstructionRecording.Options(InstructionRecording.Mode.LAST, 2, null));
            recording.capture(true, Optional.of(instruction(1, 0x10).view));

            assertThrows(IOException.class, () -> recording.recordTo(existing));

            assertTrue(recording.isRecording());
            assertNull(recording.path());
            assertTrue(recording.error().startsWith("Could not start recording:"));
            assertEquals("keep existing recording", Files.readString(existing));
            recording.capture(true, Optional.of(instruction(1, 0x20).view));
            assertEquals(2, recording.events().size());

            recording.start(new InstructionRecording.Options(InstructionRecording.Mode.FIRST, 1, null));
            recording.capture(true, Optional.of(instruction(1, 0x30).view));

            assertEquals("", recording.error());
            assertEquals(1, recording.events().size());
            assertEquals(3, recording.events().get(0).sequence);
        }
    }

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
    public void measuredSizesMatchFileFramesForAsciiUnicodeAndMalformedSurrogates() throws Exception {
        for (String text : List.of("", "ASCII\0", "é", "€", "\uD83D\uDE42", "\uD800", "\uDC00", "A\uD800B")) {
            ViewRow row = new ViewRow(text, text, text, text, true,
                    List.of(text), List.of(new NavLink(text, text)));
            InstructionRecording.Event event = new InstructionRecording.Event(1, true,
                    new StepView(text, text, OptionalInt.of(0), List.of(row)));
            int size = InstructionRecording.sizeOf(event);
            Path file = temporary.newFolder().toPath().resolve("utf8.emutrace");

            InstructionRecording.write(file, new InstructionRecording.Header("8080"), List.of(event));

            // Header: 16 bytes; event framing: 13; completion footer: 17.
            assertEquals(size + 46L, Files.size(file));
            try (InstructionRecording.Reader reader = new InstructionRecording.Reader(file)) {
                InstructionRecording.Event restored = reader.nextPage(1).get(0);
                assertEquals(size, InstructionRecording.sizeOf(restored));
                assertEquals(new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), restored.view.summary);
                assertTrue(reader.complete());
            }
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
        assertTrue(exported.contains("emuStudio instruction recording v5 | test-cpu"));
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
