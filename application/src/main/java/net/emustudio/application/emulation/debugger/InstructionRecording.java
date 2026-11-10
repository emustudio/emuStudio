/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.debugger.NavLink;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ViewRow;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Versioned execution recordings. Buffered writes apply backpressure; readers retain only one page. */
public final class InstructionRecording {
    private static final int MAGIC = 0x454D5554, VERSION = 4;
    private static final int EVENT = 1, END = 0;
    private static final int MAX_TEXT = 65536, MAX_ROWS = 4096, MAX_ITEMS = 256;
    private static final int MAX_EVENT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_PAGE_BYTES = 32 * 1024 * 1024;

    private InstructionRecording() {}

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
        private long count, lastSequence;
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
                writeText(out, header.processor);
                out.flush();
            } catch (IOException | RuntimeException e) {
                try { out.close(); } catch (IOException closing) { e.addSuppressed(closing); }
                throw e;
            }
        }

        public synchronized void append(Event event) throws IOException {
            if (closed || failed) throw new IOException("Recording is closed or failed");
            try {
                if (event.sequence <= lastSequence) throw new IOException("Recording sequence is not increasing");
                byte[] encoded = encode(event);
                out.writeByte(EVENT);
                out.writeLong(event.sequence);
                out.writeInt(encoded.length);
                out.write(encoded);
                count++;
                lastSequence = event.sequence;
            } catch (IOException | RuntimeException failure) {
                failed = true;
                if (failure instanceof IOException) throw (IOException) failure;
                throw new IOException("Invalid recording event", failure);
            }
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

        /** Preserve the readable prefix without claiming a failed capture is complete. */
        public synchronized void abort() throws IOException {
            failed = true;
            close();
        }
    }

    public static final class Reader implements AutoCloseable {
        private final RandomAccessFile in;
        private final Path path;
        private final long modified, length, firstEvent;
        private final Header header;
        private long count, lastSequence;
        private boolean complete, finished;
        private String status = "Recording completeness unverified until its end is read";

        public Reader(Path path) throws IOException {
            this.path = path.toAbsolutePath().normalize();
            modified = Files.getLastModifiedTime(path).toMillis();
            in = new RandomAccessFile(path.toFile(), "r");
            length = in.length();
            try {
                if (in.readInt() != MAGIC || in.readInt() != VERSION)
                    throw new IOException("Unsupported instruction recording format/version");
                header = new Header(readText(in));
                firstEvent = in.getFilePointer();
            } catch (IOException | IllegalArgumentException e) {
                try { in.close(); } catch (IOException closing) { e.addSuppressed(closing); }
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

        public Cursor cursor() throws IOException { return new Cursor(this); }

        public void seek(Cursor cursor) throws IOException {
            if (!path.equals(cursor.path) || length != cursor.length || modified != cursor.modified
                    || cursor.position < firstEvent || cursor.position > length)
                throw new IOException("Recording changed; reopen it from the first page");
            in.seek(cursor.position);
            count = cursor.count;
            lastSequence = cursor.sequence;
            finished = complete = false;
            status = "Recording completeness unverified until its end is read";
        }

        public Header header() { return header; }
        public boolean complete() { return complete; }
        public boolean finished() { return finished; }
        public String status() { return status; }

        public List<Event> nextPage(int limit) throws IOException {
            return nextPage(limit, event -> true);
        }

        public List<Event> nextPage(int limit, Predicate<Event> matches) throws IOException {
            if (limit < 1 || limit > 10000) throw new IllegalArgumentException("Page size must be 1..10000");
            Objects.requireNonNull(matches);
            List<Event> page = new ArrayList<>();
            int pageBytes = 0;
            while (!finished) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Recording scan interrupted");
                try {
                    long position = in.getFilePointer();
                    int marker = in.readUnsignedByte();
                    if (marker == END) {
                        if (in.readLong() != count || in.readLong() != lastSequence || in.read() != -1)
                            throw new IOException("Recording footer does not match its events");
                        complete = true;
                        finished = true;
                        status = "Complete recording: " + count + " instructions";
                    } else if (marker == EVENT) {
                        long previousSequence = lastSequence;
                        long sequence = in.readLong();
                        int size = readCount(in, MAX_EVENT_BYTES, "event bytes");
                        byte[] encoded = new byte[size];
                        in.readFully(encoded);
                        Event event;
                        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(encoded))) {
                            event = readEvent(data, sequence);
                            event.encodedSize = size;
                            if (data.read() != -1) throw new IOException("Unexpected event data");
                        } catch (IllegalArgumentException failure) {
                            throw new IOException("Invalid recorded presentation", failure);
                        }
                        if (sequence <= lastSequence)
                            throw new IOException("Invalid recording sequence");
                        count++;
                        lastSequence = sequence;
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

        @Override public void close() throws IOException { in.close(); }
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

    /** Export an entire saved recording using bounded pages, including its completeness status. */
    public static void exportText(Path destination, Path source) throws IOException {
        try (Reader reader = new Reader(source); BufferedWriter out = newTextFile(destination)) {
            writeTextHeader(out, reader.header());
            while (!reader.finished()) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Recording export cancelled");
                writeTextEvents(out, reader.nextPage(1024));
            }
            out.write(reader.status());
            out.newLine();
        }
    }

    private static BufferedWriter newTextFile(Path path) throws IOException {
        return Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static void writeTextHeader(BufferedWriter out, Header header) throws IOException {
        out.write("emuStudio instruction recording v" + VERSION + " | " + header.processor);
        out.newLine();
    }

    private static void writeTextEvents(BufferedWriter out, List<Event> events) throws IOException {
        for (Event event : events) {
            out.write("#" + event.sequence + " | "
                    + (event.view.programLocation.isPresent()
                    ? String.format(Locale.ROOT, "PC=%04X | ", event.view.programLocation.getAsInt()) : "")
                    + event.view.summary + " | " + (event.completed ? "completed" : "failed"));
            out.newLine();
            String group = "";
            for (ViewRow row : event.view.rows) {
                if (!row.group.equals(group)) {
                    group = row.group;
                    out.write("  " + group);
                    out.newLine();
                }
                out.write("    " + row.label + ": " + row.before
                        + (row.before.equals(row.after) ? "" : " → " + row.after)
                        + (row.changed ? " [changed]" : ""));
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

    /** Encoded presentation size; also validates the individual-record byte limit. */
    public static int sizeOf(Event event) throws IOException {
        int size = event.encodedSize;
        return size >= 0 ? size : encode(event).length;
    }

    /** Resolve a recorded link using only retained passive presentation, without loading a plugin. */
    public static OptionalLong resolveEvent(List<Event> history, long ownerOrdinal, NavLink link) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Event event = history.get(i);
            if (event.sequence < ownerOrdinal
                    && event.view.rows.stream().anyMatch(row -> row.tags.contains(link.tag)))
                return OptionalLong.of(event.sequence);
        }
        return OptionalLong.empty();
    }

    private static byte[] encode(Event event) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream bounded = new FilterOutputStream(bytes) {
            private int size;
            @Override public void write(int value) throws IOException {
                if (++size > MAX_EVENT_BYTES) throw new IOException("Recording event exceeds byte limit");
                out.write(value);
            }
            @Override public void write(byte[] values, int offset, int length) throws IOException {
                if (length > MAX_EVENT_BYTES - size) throw new IOException("Recording event exceeds byte limit");
                size += length;
                out.write(values, offset, length);
            }
        };
        try (DataOutputStream data = new DataOutputStream(bounded)) {
            data.writeBoolean(event.completed);
            writeText(data, event.view.summary);
            writeText(data, event.view.foldKey);
            if (event.view.programLocation == null
                    || (event.view.programLocation.isPresent() && event.view.programLocation.getAsInt() < 0))
                throw new IOException("Invalid program location");
            data.writeInt(event.view.programLocation.orElse(-1));
            writeCount(data, event.view.rows.size(), MAX_ROWS, "presentation rows");
            for (ViewRow row : event.view.rows) {
                writeText(data, row.group);
                writeText(data, row.label);
                writeText(data, row.before);
                writeText(data, row.after);
                data.writeBoolean(row.changed);
                writeCount(data, row.tags.size(), MAX_ITEMS, "search tags");
                for (String tag : row.tags) writeText(data, tag);
                writeCount(data, row.links.size(), MAX_ITEMS, "navigation links");
                for (NavLink link : row.links) {
                    writeText(data, link.label);
                    writeText(data, link.tag);
                }
            }
        }
        byte[] encoded = bytes.toByteArray();
        event.encodedSize = encoded.length;
        return encoded;
    }

    private static Event readEvent(DataInputStream in, long sequence) throws IOException {
        boolean completed = in.readBoolean();
        String summary = readText(in), foldKey = readText(in);
        int location = in.readInt();
        if (location < -1) throw new IOException("Invalid program location");
        int size = readCount(in, MAX_ROWS, "presentation rows");
        List<ViewRow> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String group = readText(in), label = readText(in),
                    before = readText(in), after = readText(in);
            boolean changed = in.readBoolean();
            int tagsCount = readCount(in, MAX_ITEMS, "search tags");
            List<String> tags = new ArrayList<>();
            for (int j = 0; j < tagsCount; j++) tags.add(readText(in));
            int linksCount = readCount(in, MAX_ITEMS, "navigation links");
            List<NavLink> links = new ArrayList<>();
            for (int j = 0; j < linksCount; j++) {
                links.add(new NavLink(readText(in), readText(in)));
            }
            rows.add(new ViewRow(group, label, before, after, changed, tags, links));
        }
        return new Event(sequence, completed,
                new StepView(summary, foldKey, location < 0 ? OptionalInt.empty() : OptionalInt.of(location), rows));
    }

    private static void writeText(DataOutput out, String text) throws IOException {
        if (text == null || text.length() > MAX_TEXT) throw new IOException("Invalid or oversized recording text");
        byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
        out.writeInt(encoded.length);
        out.write(encoded);
    }

    private static String readText(DataInput in) throws IOException {
        int length = readCount(in, MAX_TEXT * 4, "text bytes");
        byte[] encoded = new byte[length];
        in.readFully(encoded);
        String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(encoded)).toString();
        if (text.length() > MAX_TEXT) throw new IOException("Recording text exceeds limit");
        return text;
    }

    private static void writeCount(DataOutput out, int count, int maximum, String what) throws IOException {
        if (count > maximum) throw new IOException("Invalid count of " + what);
        out.writeInt(count);
    }

    private static int readCount(DataInput in, int maximum, String what) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > maximum) throw new IOException("Invalid count of " + what);
        return count;
    }
}
