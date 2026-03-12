/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.recording;

import net.emustudio.plugins.device.zxspectrum.ula.audio.AudioSink;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Asynchronous bridge between the emulator threads and {@link MjpegAviRecorder}.
 *
 * <p>Display frames and PCM audio are copied into a single FIFO queue so the AVI writer sees the
 * same ordering in which the emulator produced them, while image encoding and file I/O happen on a
 * dedicated worker thread instead of the emulator callback path.
 */
public final class DisplayRecordingSession implements Consumer<BufferedImage>, AudioSink {
    private static final QueueEntry POISON = recorder -> {
    };

    private final BlockingQueue<QueueEntry> queue = new LinkedBlockingQueue<>();
    private final MjpegAviRecorder recorder;
    private final Thread worker;

    private volatile boolean accepting = true;

    public DisplayRecordingSession(int width, int height, int videoScale, int videoRate, int audioSampleRate) throws IOException {
        this.recorder = new MjpegAviRecorder(width, height, videoScale, videoRate, audioSampleRate);
        this.worker = new Thread(this::drainQueue, "ZX-Spectrum-48K-Recording");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void accept(BufferedImage frame) {
        if (!accepting || frame == null) {
            return;
        }
        queue.offer(writer -> writer.recordVideoFrame(frame));
    }

    @Override
    public void write(byte[] samples, int length) {
        if (!accepting || length <= 0) {
            return;
        }
        byte[] copy = Arrays.copyOf(samples, length);
        queue.offer(writer -> writer.recordAudio(copy, copy.length));
    }

    public Path moveTo(Path target) throws IOException {
        Objects.requireNonNull(target);
        finish();
        return recorder.moveTo(target);
    }

    public void discard() throws IOException {
        accepting = false;
        queue.clear();
        stopWorker();
        recorder.discard();
    }

    private void finish() throws IOException {
        accepting = false;
        stopWorker();
        recorder.close();
    }

    private void stopWorker() throws IOException {
        queue.offer(POISON);
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while finishing recording", e);
        }
    }

    private void drainQueue() {
        try {
            while (true) {
                QueueEntry entry = queue.take();
                if (entry == POISON) {
                    break;
                }
                entry.write(recorder);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface QueueEntry {
        void write(MjpegAviRecorder recorder);
    }
}
