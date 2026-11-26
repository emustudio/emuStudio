/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.emustudio.application.emulation.debugger.InstructionRecording.*;

/** Writes a bounded queue and publishes page offsets only after flushing their contents. */
final class RecordingFile {
    private static final int QUEUE_EVENTS = 16_384;
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Path path;
    private final Writer writer;
    private final ArrayDeque<Event> queue = new ArrayDeque<>();
    private final List<Long> offsets = new ArrayList<>();
    private final CompletableFuture<Void> finished = new CompletableFuture<>();
    private long queuedBytes, publishedEnd;
    private boolean finishing;
    private volatile String error = "";

    RecordingFile(Path path, Header header) throws IOException {
        this.path = path.toAbsolutePath().normalize();
        writer = new Writer(this.path, header);
        publishedEnd = writer.position();
        Thread worker = new Thread(this::write, "instruction-recording-writer");
        worker.setDaemon(true);
        worker.start();
    }

    RecordingFile(Path path) throws IOException {
        this.path = path.toAbsolutePath().normalize();
        writer = null;
        try (Reader reader = new Reader(this.path)) {
            while (!reader.finished()) {
                long start = reader.position();
                List<Event> events = reader.nextPage(PAGE_SIZE);
                if (events.isEmpty()) break;
                offsets.add(start);
                publishedEnd = start;
                for (Event event : events) publishedEnd += 13L + sizeOf(event);
            }
            if (!reader.complete()) error = reader.status();
        }
        finishing = true;
        finished.complete(null);
    }

    synchronized void append(Event event, int size) {
        if (finishing) return;
        if (queue.size() >= QUEUE_EVENTS || queuedBytes + size > MAX_BYTES) {
            fail("File writer fell behind");
            return;
        }
        boolean wasEmpty = queue.isEmpty();
        queue.addLast(event);
        queuedBytes += size;
        if (wasEmpty) notifyAll();
    }

    synchronized void finish() {
        finishing = true;
        notifyAll();
    }

    synchronized void fail(String message) {
        error = "Incomplete recording. " + message;
        finish();
    }

    String error() {
        return error;
    }

    boolean complete() {
        return isFinished() && error.isEmpty();
    }

    boolean isFinished() {
        return finished.isDone();
    }

    void awaitFinished() {
        finished.join();
    }

    void save(Path destination, boolean text) throws IOException {
        if (!isFinished()) throw new IOException("Stop the recording before exporting it");
        if (text) exportText(destination, path);
        else Files.copy(path, destination);
    }

    Page page(long requested) throws IOException {
        long start, end;
        int index, pages;
        synchronized (this) {
            pages = offsets.size();
            if (pages == 0) return new Page(List.of(), 0, -1, -1);
            index = requested == LATEST_PAGE ? pages - 1 : (int) Math.max(0, Math.min(requested, pages - 1));
            start = offsets.get(index);
            end = index + 1 < pages ? offsets.get(index + 1) : publishedEnd;
        }
        List<Event> events = new ArrayList<>(PAGE_SIZE);
        try (FileChannel channel = FileChannel.open(path)) {
            channel.position(start);
            try (DataInputStream input = new DataInputStream(new BufferedInputStream(Channels.newInputStream(channel)))) {
                while (start < end) {
                    if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException();
                    Event event = readFramedEvent(input);
                    events.add(event);
                    start += 13L + sizeOf(event);
                }
            }
        }
        return new Page(List.copyOf(events), index, index > 0 ? index - 1 : -1,
                index + 1 < pages ? index + 1 : -1);
    }

    private void write() {
        List<Event> batch = new ArrayList<>(PAGE_SIZE);
        List<Long> newOffsets = new ArrayList<>();
        int pageEvents = 0;
        long pageBytes = 0;
        long lastFlush = System.nanoTime();
        try {
            while (take(batch, writer.position() > publishedEnd)) {
                for (Event event : batch) {
                    long bytes = 13L + sizeOf(event);
                    if (pageEvents == 0 || pageEvents == PAGE_SIZE || pageBytes + bytes > MAX_BYTES) {
                        newOffsets.add(writer.position());
                        pageEvents = 0;
                        pageBytes = 0;
                    }
                    writer.append(event);
                    pageEvents++;
                    pageBytes += bytes;
                }
                if (System.nanoTime() - lastFlush >= 50_000_000L) {
                    publish(newOffsets);
                    lastFlush = System.nanoTime();
                }
                batch.clear();
            }
            publish(newOffsets);
            if (error.isEmpty()) writer.close();
            else writer.abort();
        } catch (IOException | RuntimeException | InterruptedException failure) {
            fail(failure.toString());
            try {
                writer.abort();
            } catch (IOException closing) {
                fail(failure + " (" + closing + ")");
            }
        } finally {
            synchronized (this) {
                queue.clear();
                queuedBytes = 0;
            }
            finished.complete(null);
        }
    }

    private synchronized boolean take(List<Event> batch, boolean unflushed) throws IOException, InterruptedException {
        if (unflushed && queue.isEmpty() && !finishing) wait(50);
        else while (queue.isEmpty() && !finishing) wait();
        while (!queue.isEmpty() && batch.size() < PAGE_SIZE) {
            Event event = queue.removeFirst();
            queuedBytes -= sizeOf(event);
            batch.add(event);
        }
        return !batch.isEmpty() || !finishing;
    }

    private void publish(List<Long> newOffsets) throws IOException {
        writer.flush();
        synchronized (this) {
            offsets.addAll(newOffsets);
            publishedEnd = writer.position();
        }
        newOffsets.clear();
    }
}
