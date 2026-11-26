/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.debugger.NavLink;
import net.emustudio.emulib.plugin.debugger.PackedStep;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ViewRow;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retained execution events and their versioned file format. File readers retain only one page.
 */
public final class InstructionRecording implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstructionRecording.class);
    private static final int MAGIC = 0x454D5554, VERSION = 5, PRESENTATION_VERSION = 4;
    private static final int EVENT = 1, PACKED_EVENT = 2, END = 0;
    private static final int MAX_TEXT = 65536, MAX_ROWS = 4096, MAX_ITEMS = 256;
    private static final int MAX_EVENT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_PAGE_BYTES = 32 * 1024 * 1024;

    public static final int DEFAULT_LIMIT = 1_000;
    public static final long LATEST_PAGE = -1;
    public static final int PAGE_SIZE = 250;

    private final List<Recording> recordings = new ArrayList<>();
    private final boolean supported;
    private final Supplier<String> processor;
    private final IntSupplier addressSpaceSize;
    private long nextSequence, nextRecording;
    private Recording current;
    private boolean active, paused;
    private Writer writer;
    private Path path;
    private String error = "";

    public enum Mode { FIRST, LAST }

    public static final class AddressRange {
        public final int from, through;

        public AddressRange(int from, int through) {
            this.from = from;
            this.through = through;
        }
    }

    public static final class Options {
        public final Mode mode;
        public final int limit;
        public final AddressRange range;

        public Options(Mode mode, int limit, AddressRange range) {
            this.mode = Objects.requireNonNull(mode);
            this.limit = limit;
            this.range = range;
        }
    }

    /** Session-owned capture. Swing holds its identity and reads events through pages. */
    public static final class Recording {
        private final long id;
        private volatile String name = "Unnamed";
        private final Options options;
        private volatile RecordingFile file;
        private final AddressRange range;
        private final List<Event> events = new ArrayList<>();
        private long first;
        private volatile boolean complete;
        private volatile boolean pending;
        private Path destination;

        private Recording(long id, Options options, RecordingFile file, AddressRange range) {
            this.id = id;
            this.options = options;
            this.file = file;
            this.range = range;
        }

        public String name() {
            return name;
        }

        public Options options() {
            return options;
        }

        public AddressRange range() {
            return range;
        }

        public boolean isPending() {
            return pending;
        }

        public boolean isComplete() {
            return file == null ? complete : file.complete();
        }

        public boolean isFile() {
            return file != null;
        }

        public boolean isFileFinished() {
            return file != null && file.isFinished();
        }

        public String error() {
            return file == null ? "" : file.error();
        }

        @Override public String toString() {
            return id + " | " + name;
        }

        private long end() {
            return first + events.size();
        }

        private Event event(long index) {
            return events.get((int) (index % events.size()));
        }

        private void append(Event event) {
            if (options.mode == Mode.LAST && events.size() == options.limit) {
                events.set((int) (first % events.size()), event);
                first++;
            } else {
                events.add(event);
            }
        }

        private List<Event> copy(long start, int count) {
            List<Event> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) result.add(event(start + i));
            return Collections.unmodifiableList(result);
        }
    }

    public static final class Page {
        public final List<Event> events;
        public final long start, previous, next;

        Page(List<Event> events, long start, long previous, long next) {
            this.events = events;
            this.start = start;
            this.previous = previous;
            this.next = next;
        }
    }

    InstructionRecording(boolean supported, Supplier<String> processor, IntSupplier addressSpaceSize) {
        this.supported = supported;
        this.processor = processor;
        this.addressSpaceSize = addressSpaceSize;
    }

    public synchronized boolean isRecording() {
        return active && (current.file == null || current.file.error().isEmpty());
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    synchronized void setPaused(boolean paused) {
        if (!isRecording()) throw new IllegalStateException("Start recording first");
        this.paused = paused;
    }

    public synchronized Recording current() {
        return current;
    }

    public synchronized List<Recording> recordings() {
        return List.copyOf(recordings);
    }

    public synchronized void addRecording(List<Event> events) {
        Recording recording = new Recording(++nextRecording, null, null, null);
        recording.events.addAll(events);
        recordings.add(recording);
    }

    public synchronized void rename(Recording recording, String name) {
        if (recordings.contains(recording) && !name.isBlank()) recording.name = name.strip();
    }

    public synchronized void delete(Recording recording) {
        if (!canDelete(recording)) return;
        recordings.remove(recording);
        if (recording == current) {
            stop();
            current = null;
        }
    }

    public synchronized boolean canDelete(Recording recording) {
        return !(recording == current && isRecording()) && (recording.file == null || recording.file.isFinished());
    }

    public Page page(Recording recording, long start) throws IOException {
        synchronized (this) {
            if (!recordings.contains(recording)) return new Page(List.of(), 0, -1, -1);
            if (recording.file == null) return memoryPage(recording, start);
        }
        return recording.file.page(start);
    }

    private Page memoryPage(Recording recording, long start) {
        long first = recording.first;
        long end = recording.end();
        long anchor = start == LATEST_PAGE ? end - 1 : Math.min(start, end - 1);
        long boundary = Math.max(first, anchor) / PAGE_SIZE * PAGE_SIZE;
        start = Math.max(first, boundary);
        long pageEnd = Math.min(end, boundary + PAGE_SIZE);
        int count = (int) (pageEnd - start);
        long previous = start > first ? Math.max(first, boundary - PAGE_SIZE) : -1;
        long next = pageEnd < end ? pageEnd : -1;
        return new Page(recording.copy(start, count), start, previous, next);
    }

    public synchronized List<Event> events() {
        return current == null ? List.of() : current.copy(current.first, current.events.size());
    }

    synchronized void stop() {
        active = false;
        paused = false;
        if (current != null && current.file != null) current.file.finish();
    }

    synchronized void start(Options options) {
        begin(prepare(options));
    }

    public synchronized Recording prepare(Options options) {
        if (options.limit <= 0 || options.limit > 1_000_000)
            throw new IllegalArgumentException("Recording limit must be 1..1000000");
        return prepare(options, options.range, null);
    }

    public synchronized void prepare(Path path, AddressRange range) {
        prepare(null, range, Objects.requireNonNull(path));
    }

    private Recording prepare(Options options, AddressRange range, Path destination) {
        requireSupported();
        validateRange(range);
        Recording recording = new Recording(++nextRecording, options, null, range);
        recording.pending = true;
        recording.destination = destination;
        recordings.add(recording);
        return recording;
    }

    synchronized void startPrepared(Recording recording) throws IOException {
        if (!recordings.contains(recording) || !recording.pending)
            throw new IllegalArgumentException("Select a recording that has not started");
        if (recording.destination != null) {
            recording.file = new RecordingFile(recording.destination, header());
            recording.destination = null;
        }
        begin(recording);
    }

    synchronized void start(Path path, AddressRange range) throws IOException {
        validateRange(range);
        RecordingFile file = new RecordingFile(path, header());
        Recording recording = new Recording(++nextRecording, null, file, range);
        recordings.add(recording);
        begin(recording);
    }

    private void validateRange(AddressRange range) {
        if (range != null && (range.from < 0 || range.through < range.from || range.through >= addressSpaceSize.getAsInt()))
            throw new IllegalArgumentException("Recording range must be within CPU addresses, with From ≤ To");
    }

    private void begin(Recording recording) {
        stop();
        current = recording;
        recording.pending = false;
        active = true;
        paused = false;
        error = "";
    }

    synchronized boolean requiresFullCapture() {
        return (isRecording() && !paused) || writer != null;
    }

    public synchronized Path path() { return path; }
    public synchronized String error() { return error; }

    synchronized void recordTo(Path path) throws IOException {
        if (writer != null) throw new IllegalStateException("Stop the current file recording first");
        try {
            writer = new Writer(path, header());
        } catch (IOException failure) {
            failFile("Could not start recording: " + failure.getMessage());
            throw failure;
        }
        this.path = path.toAbsolutePath().normalize();
        error = "";
    }

    synchronized void stopFile() throws IOException {
        Writer current = writer;
        writer = null;
        path = null;
        try {
            if (current != null) current.close();
        } catch (IOException failure) {
            error = "Incomplete recording: " + failure.getMessage();
            throw failure;
        }
    }

    public void saveHistory(Path path, boolean text) throws IOException {
        save(current(), path, text);
    }

    public void open(Path path) throws IOException {
        RecordingFile file = new RecordingFile(path);
        synchronized (this) {
            Recording recording = new Recording(++nextRecording, null, file, null);
            recording.name = path.getFileName().toString();
            recordings.add(recording);
        }
    }

    public void save(Recording recording, Path path, boolean text) throws IOException {
        RecordingFile file;
        List<Event> captured;
        synchronized (this) {
            if (recording != null && (!recordings.contains(recording) || recording.pending))
                throw new IOException("Select a recording that has started");
            file = recording == null ? null : recording.file;
            captured = recording != null && file == null
                    ? recording.copy(recording.first, recording.events.size()) : List.of();
        }
        if (file != null) {
            file.save(path, text);
            return;
        }
        if (text) exportText(path, header(), captured);
        else write(path, header(), captured);
    }

    synchronized void capture(boolean completed, Optional<StepView> view) {
        long sequence = ++nextSequence;
        if (view.isEmpty()) return;
        Event event = new Event(sequence, completed, view.get());
        try {
            retain(event);
        } catch (IOException failure) {
            fail("History capture failed: " + failure.getMessage());
            return;
        }
        if (writer != null) {
            try {
                writer.append(event);
            } catch (IOException failure) {
                failFile("Incomplete recording: " + failure.getMessage());
            }
        }
    }

    synchronized void fail(String message) {
        if (active && current.file != null) current.file.fail(message);
        stop();
        failFile(message);
    }

    @Override public synchronized void close() throws IOException {
        stop();
        try {
            stopFile();
        } finally {
            for (Recording recording : recordings) {
                if (recording.file != null) recording.file.awaitFinished();
            }
        }
    }

    private Header header() {
        requireSupported();
        return new Header(processor.get());
    }

    private void requireSupported() {
        if (!supported) throw new IllegalStateException("Instruction recording unsupported by this CPU");
    }

    private void failFile(String message) {
        error = message;
        Writer current = writer;
        writer = null;
        path = null;
        if (current != null) {
            try {
                current.abort();
            } catch (IOException failure) {
                LOGGER.warn("Could not close failed recording", failure);
            }
        }
        LOGGER.warn(message);
    }

    private void retain(Event event) throws IOException {
        if (!isRecording() || paused) return;
        AddressRange range = current.range;
        if (range != null) {
            int address = event.view.programLocation.orElse(-1);
            if (address < range.from || address > range.through) return;
        }
        int size = sizeOf(event); // Reject steps the recording format cannot encode.
        if (current.file != null) {
            current.file.append(event, size);
            return;
        }
        Options options = current.options;
        current.append(event);
        if (options.mode == Mode.FIRST && current.events.size() == options.limit) {
            current.complete = true;
            stop();
        }
    }

    public static final class Event {
        public final long sequence;
        public final boolean completed;
        public final StepView view;
        private volatile int encodedSize = -1;

        public Event(long sequence, boolean completed, StepView view) {
            if (sequence < 1) throw new IllegalArgumentException("Invalid recording sequence");
            this.sequence = sequence;
            this.completed = completed;
            this.view = Objects.requireNonNull(view);
        }
    }

    public static final class Header {
        public final String processor;

        public Header(String processor) {
            this.processor = Objects.requireNonNull(processor);
            if (processor.isBlank() || processor.length() > MAX_TEXT)
                throw new IllegalArgumentException("Invalid recording processor");
        }
    }

    public static final class Writer implements AutoCloseable {
        private final DataOutputStream out;
        private ByteBuffer frame = ByteBuffer.allocate(4_096);
        private long count, lastSequence, position;
        private boolean closed, failed;

        public Writer(Path path, Header header) throws IOException {
            this(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), header);
        }

        // Stream seam lets tests exercise failures without depending on a particular filesystem.
        Writer(OutputStream stream, Header header) throws IOException {
            out = new DataOutputStream(new BufferedOutputStream(stream, 65536));
            try {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                byte[] processor = header.processor.getBytes(StandardCharsets.UTF_8);
                out.writeInt(processor.length);
                out.write(processor);
                out.flush();
                position = 12L + processor.length;
            } catch (IOException | RuntimeException e) {
                try {
                    out.close();
                } catch (IOException closing) {
                    e.addSuppressed(closing);
                }
                throw e;
            }
        }

        public synchronized void append(Event event) throws IOException {
            if (closed || failed) throw new IOException("Recording is closed or failed");
            try {
                if (event.sequence <= lastSequence) throw new IOException("Recording sequence is not increasing");
                int size = sizeOf(event);
                int frameSize = 13 + size;
                if (frame.capacity() < frameSize) frame = ByteBuffer.allocate(Math.max(frameSize, frame.capacity() * 2));
                frame.clear();
                PackedStep packed = event.view.packed();
                frame.put((byte) (packed == null ? EVENT : PACKED_EVENT)).putLong(event.sequence).putInt(size);
                if (packed == null) writeEvent(frame, event);
                else writePackedEvent(frame, event, packed);
                out.write(frame.array(), 0, frame.position());
                count++;
                lastSequence = event.sequence;
                position += frameSize;
            } catch (IOException | RuntimeException failure) {
                failed = true;
                if (failure instanceof IOException) throw (IOException) failure;
                throw new IOException("Invalid recording event", failure);
            }
        }

        long position() {
            return position;
        }

        void flush() throws IOException {
            out.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            if (closed) return;
            closed = true;
            try (out) {
                if (!failed) {
                    out.writeByte(END);
                    out.writeLong(count);
                    out.writeLong(lastSequence);
                }
            }
        }

        /**
         * Preserve the readable prefix without claiming a failed capture is complete.
         */
        public synchronized void abort() throws IOException {
            failed = true;
            close();
        }
    }

    public static final class Reader implements AutoCloseable {
        private static final String UNVERIFIED = "Recording completeness unverified until its end is read";
        private final RandomAccessFile in;
        private final Path path;
        private final long modified, length, firstEvent;
        private final Header header;
        private final int version;
        private long count, lastSequence;
        private boolean complete, finished;
        private String status = UNVERIFIED;

        public Reader(Path path) throws IOException {
            this.path = path.toAbsolutePath().normalize();
            modified = Files.getLastModifiedTime(path).toMillis();
            in = new RandomAccessFile(path.toFile(), "r");
            length = in.length();
            try {
                if (in.readInt() != MAGIC) throw new IOException("Unsupported instruction recording format/version");
                version = in.readInt();
                if (version != PRESENTATION_VERSION && version != VERSION)
                    throw new IOException("Unsupported instruction recording format/version");
                header = new Header(readText(in));
                firstEvent = in.getFilePointer();
            } catch (IOException | IllegalArgumentException e) {
                try {
                    in.close();
                } catch (IOException closing) {
                    e.addSuppressed(closing);
                }
                throw new IOException("Invalid or incomplete recording header", e);
            }
        }

        public static final class Cursor {
            private final Path path;
            private final long position, count, sequence, modified, length;

            private Cursor(Reader reader) throws IOException {
                path = reader.path;
                position = reader.in.getFilePointer();
                count = reader.count;
                sequence = reader.lastSequence;
                modified = reader.modified;
                length = reader.length;
            }
        }

        public Cursor cursor() throws IOException {
            return new Cursor(this);
        }

        public void seek(Cursor cursor) throws IOException {
            if (!path.equals(cursor.path) || length != cursor.length || modified != cursor.modified || cursor.position < firstEvent || cursor.position > length)
                throw new IOException("Recording changed; reopen it from the first page");
            in.seek(cursor.position);
            count = cursor.count;
            lastSequence = cursor.sequence;
            finished = complete = false;
            status = UNVERIFIED;
        }

        public Header header() {
            return header;
        }

        long position() throws IOException {
            return in.getFilePointer();
        }

        public boolean complete() {
            return complete;
        }

        public boolean finished() {
            return finished;
        }

        public String status() {
            return status;
        }

        public List<Event> nextPage(int limit) throws IOException {
            return nextPage(limit, event -> true);
        }

        public List<Event> nextPage(int limit, Predicate<Event> matches) throws IOException {
            if (limit < 1 || limit > 10000) throw new IllegalArgumentException("Page size must be 1..10000");
            Objects.requireNonNull(matches);
            List<Event> page = new ArrayList<>();
            int pageBytes = 0;
            while (!finished) {
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedIOException("Recording scan interrupted");
                try {
                    long position = in.getFilePointer();
                    int marker = in.readUnsignedByte();
                    if (marker == END) {
                        readFooter();
                    } else if (marker == EVENT || (marker == PACKED_EVENT && version == VERSION)) {
                        long previousSequence = lastSequence;
                        Event event = readNextEvent(marker);
                        int size = event.encodedSize;
                        count++;
                        lastSequence = event.sequence;
                        if (matches.test(event)) {
                            if (page.size() == limit || (!page.isEmpty() && pageBytes + size > MAX_PAGE_BYTES)) {
                                // Skipped events belong to this page; the next match belongs to the next page.
                                in.seek(position);
                                count--;
                                lastSequence = previousSequence;
                                break;
                            }
                            page.add(event);
                            pageBytes += size;
                        }
                    } else throw new IOException("Invalid recording event marker");
                } catch (EOFException e) {
                    finished = true;
                    status = "Incomplete recording: missing footer or truncated instruction after " + count + " instructions";
                } catch (IOException e) {
                    finished = true;
                    status = "Invalid recording: " + e.getMessage();
                    throw e;
                }
            }
            return List.copyOf(page);
        }

        private Event readNextEvent(int marker) throws IOException {
            Event event = readEventPayload(in, marker);
            if (event.sequence <= lastSequence) throw new IOException("Invalid recording sequence");
            return event;
        }

        private void readFooter() throws IOException {
            if (in.readLong() != count || in.readLong() != lastSequence || in.read() != -1)
                throw new IOException("Recording footer does not match its events");
            complete = true;
            finished = true;
            status = "Complete recording: " + count + " instructions";
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    static Event readFramedEvent(DataInput in) throws IOException {
        int marker = in.readUnsignedByte();
        if (marker != EVENT && marker != PACKED_EVENT) throw new IOException("Invalid recording event marker");
        return readEventPayload(in, marker);
    }

    private static Event readEventPayload(DataInput in, int marker) throws IOException {
        long sequence = in.readLong();
        int size = readCount(in, MAX_EVENT_BYTES, "event bytes");
        byte[] encoded = new byte[size];
        in.readFully(encoded);
        return decode(encoded, sequence, marker);
    }

    public static void write(Path path, Header header, List<Event> events) throws IOException {
        try (Writer writer = new Writer(path, header)) {
            for (Event event : events) writer.append(event);
        }
    }

    public static void exportText(Path path, Header header, List<Event> events) throws IOException {
        try (BufferedWriter out = newTextFile(path)) {
            writeTextHeader(out, header);
            writeTextEvents(out, events);
        }
    }

    /**
     * Export an entire saved recording using bounded pages, including its completeness status.
     */
    public static void exportText(Path destination, Path source) throws IOException {
        try (Reader reader = new Reader(source); BufferedWriter out = newTextFile(destination)) {
            writeTextHeader(out, reader.header());
            while (!reader.finished()) {
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedIOException("Recording export cancelled");
                writeTextEvents(out, reader.nextPage(1024));
            }
            out.write(reader.status());
            out.newLine();
        }
    }

    private static BufferedWriter newTextFile(Path path) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static void writeTextHeader(BufferedWriter out, Header header) throws IOException {
        out.write("emuStudio instruction recording v" + VERSION + " | " + header.processor);
        out.newLine();
    }

    private static void writeTextEvents(BufferedWriter out, List<Event> events) throws IOException {
        for (Event event : events) {
            out.write("#" + event.sequence + " | " + (event.view.programLocation.isPresent() ? String.format(Locale.ROOT, "PC=%04X | ", event.view.programLocation.getAsInt()) : "") + event.view.summary + " | " + (event.completed ? "completed" : "failed"));
            out.newLine();
            String group = "";
            for (ViewRow row : event.view.rows) {
                if (!row.group.equals(group)) {
                    group = row.group;
                    out.write("  " + group);
                    out.newLine();
                }
                out.write("    " + row.label + ": " + row.before + (row.before.equals(row.after) ? "" : " → " + row.after) + (row.changed ? " [changed]" : ""));
                out.newLine();
                for (String tag : row.tags) {
                    out.write("      tag: " + tag);
                    out.newLine();
                }
                for (NavLink link : row.links) {
                    out.write("      " + link.label + " -> earlier " + link.tag);
                    out.newLine();
                }
            }
        }
    }

    /**
     * Encoded presentation size; also validates the individual-record byte limit.
     */
    public static int sizeOf(Event event) throws IOException {
        if (event.encodedSize >= 0) return event.encodedSize;
        StepView view = event.view;
        if (view.programLocation == null || (view.programLocation.isPresent() && view.programLocation.getAsInt() < 0))
            throw new IOException("Invalid program location");
        PackedStep packed = view.packed();
        if (packed != null) {
            int size = 5 + textSize(view.summary) + packed.encodedSize();
            if (size > MAX_EVENT_BYTES) throw new IOException("Recording event exceeds byte limit");
            event.encodedSize = size;
            return event.encodedSize;
        }
        long size = 5L + textSize(view.summary) + textSize(view.foldKey)
                + countSize(view.rows.size(), MAX_ROWS, "presentation rows");
        for (int i = 0; i < view.rows.size(); i++) {
            ViewRow row = view.rows.get(i);
            size += 1L + textSize(row.group) + textSize(row.label) + textSize(row.before) + textSize(row.after)
                    + countSize(row.tags.size(), MAX_ITEMS, "search tags")
                    + countSize(row.links.size(), MAX_ITEMS, "navigation links");
            for (int j = 0; j < row.tags.size(); j++) size += textSize(row.tags.get(j));
            for (int j = 0; j < row.links.size(); j++) {
                NavLink link = row.links.get(j);
                size += textSize(link.label) + textSize(link.tag);
            }
            if (size > MAX_EVENT_BYTES) throw new IOException("Recording event exceeds byte limit");
        }
        event.encodedSize = (int) size;
        return event.encodedSize;
    }

    /**
     * Resolve a recorded link using only retained passive presentation, without loading a plugin.
     */
    public static OptionalLong resolveEvent(List<Event> history, long ownerOrdinal, NavLink link) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Event event = history.get(i);
            if (event.sequence >= ownerOrdinal) continue;
            PackedStep packed = event.view.packed();
            if (packed == null ? event.view.rows.stream().anyMatch(row -> row.tags.contains(link.tag)) : packed.hasWrite(link.tag))
                return OptionalLong.of(event.sequence);
        }
        return OptionalLong.empty();
    }

    private static void writePackedEvent(ByteBuffer out, Event event, PackedStep packed) {
        out.put((byte) (event.completed ? 1 : 0));
        writeText(out, event.view.summary);
        out.putInt(event.view.programLocation.orElse(-1));
        packed.write(out);
    }

    private static void writeEvent(ByteBuffer out, Event event) {
        out.put((byte) (event.completed ? 1 : 0));
        writeText(out, event.view.summary);
        writeText(out, event.view.foldKey);
        out.putInt(event.view.programLocation.orElse(-1));
        List<ViewRow> rows = event.view.rows;
        out.putInt(rows.size());
        for (int i = 0; i < rows.size(); i++) writeRow(out, rows.get(i));
    }

    private static void writeRow(ByteBuffer out, ViewRow row) {
        writeText(out, row.group);
        writeText(out, row.label);
        writeText(out, row.before);
        writeText(out, row.after);
        out.put((byte) (row.changed ? 1 : 0));
        out.putInt(row.tags.size());
        for (int i = 0; i < row.tags.size(); i++) writeText(out, row.tags.get(i));
        out.putInt(row.links.size());
        for (int i = 0; i < row.links.size(); i++) {
            NavLink link = row.links.get(i);
            writeText(out, link.label);
            writeText(out, link.tag);
        }
    }

    private static Event decode(byte[] encoded, long sequence, int marker) throws IOException {
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(encoded))) {
            Event event = marker == PACKED_EVENT ? readPackedEvent(data, sequence) : readEvent(data, sequence);
            event.encodedSize = encoded.length;
            if (data.read() != -1) throw new IOException("Unexpected event data");
            return event;
        } catch (IllegalArgumentException failure) {
            throw new IOException("Invalid recorded presentation", failure);
        }
    }

    private static Event readPackedEvent(DataInputStream in, long sequence) throws IOException {
        boolean completed = in.readBoolean();
        String summary = readText(in);
        int location = in.readInt();
        if (location < -1) throw new IOException("Invalid program location");
        StepView view = PackedStep.read(summary, location < 0 ? OptionalInt.empty() : OptionalInt.of(location), in);
        return new Event(sequence, completed, view);
    }

    private static Event readEvent(DataInputStream in, long sequence) throws IOException {
        boolean completed = in.readBoolean();
        String summary = readText(in);
        String foldKey = readText(in);
        int location = in.readInt();
        if (location < -1) throw new IOException("Invalid program location");
        int size = readCount(in, MAX_ROWS, "presentation rows");
        List<ViewRow> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) rows.add(readRow(in));
        return new Event(sequence, completed, new StepView(summary, foldKey, location < 0 ? OptionalInt.empty() : OptionalInt.of(location), rows));
    }

    private static ViewRow readRow(DataInput in) throws IOException {
        String group = readText(in);
        String label = readText(in);
        String before = readText(in);
        String after = readText(in);
        boolean changed = in.readBoolean();
        int tagsCount = readCount(in, MAX_ITEMS, "search tags");
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < tagsCount; i++) tags.add(readText(in));
        int linksCount = readCount(in, MAX_ITEMS, "navigation links");
        List<NavLink> links = new ArrayList<>();
        for (int i = 0; i < linksCount; i++) links.add(new NavLink(readText(in), readText(in)));
        return new ViewRow(group, label, before, after, changed, tags, links);
    }

    private static void writeText(ByteBuffer out, String text) {
        int start = out.position() + Integer.BYTES;
        byte[] bytes = out.array();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 128) {
                byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
                out.putInt(encoded.length).put(encoded);
                return;
            }
            bytes[start + i] = (byte) c;
        }
        out.putInt(text.length());
        out.position(start + text.length());
    }

    private static String readText(DataInput in) throws IOException {
        int length = readCount(in, MAX_TEXT * 4, "text bytes");
        byte[] encoded = new byte[length];
        in.readFully(encoded);
        String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(encoded)).toString();
        if (text.length() > MAX_TEXT) throw new IOException("Recording text exceeds limit");
        return text;
    }

    private static int textSize(String text) throws IOException {
        if (text == null || text.length() > MAX_TEXT) throw new IOException("Invalid or oversized recording text");
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) >= 128) return 4 + text.getBytes(StandardCharsets.UTF_8).length;
        }
        return 4 + text.length();
    }

    private static int countSize(int count, int maximum, String what) throws IOException {
        if (count < 0 || count > maximum) throw new IOException("Invalid count of " + what);
        return 4;
    }

    private static int readCount(DataInput in, int maximum, String what) throws IOException {
        int count = in.readInt();
        countSize(count, maximum, what);
        return count;
    }
}
