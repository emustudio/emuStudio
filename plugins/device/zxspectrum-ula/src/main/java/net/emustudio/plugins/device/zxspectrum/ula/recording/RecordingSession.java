/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.recording;

import net.emustudio.plugins.device.zxspectrum.ula.audio.AudioSink;
import net.jcip.annotations.ThreadSafe;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import static net.emustudio.plugins.device.zxspectrum.ula.Constants.QUEUE_POLL_TIMEOUT_MS;
import static net.emustudio.plugins.device.zxspectrum.ula.Constants.THREAD_NAME_PREFIX;

/**
 * Asynchronous bridge between the emulator threads and {@link VideoRecorder} (thread safe).
 *
 * <p>Display frames and PCM audio are copied into a single FIFO queue so image conversion and file I/O
 * happen on a dedicated worker thread instead of the emulator callback path.
 */
@ThreadSafe
public final class RecordingSession implements Consumer<BufferedImage>, AudioSink {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final VideoRecorder recorder;
    private final Thread worker;

    private volatile boolean accepting = true;

    public RecordingSession(int width, int height, int videoScale, int videoRate, int audioSampleRate) throws IOException {
        this.recorder = new VideoRecorder(width, height, videoScale, videoRate, audioSampleRate);
        this.worker = new Thread(this::drainQueue, THREAD_NAME_PREFIX + "RecordingSession");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void accept(BufferedImage frame) {
        if (accepting && frame != null) {
            queue.add(() -> recorder.captureVideo(frame));
        }
    }

    @Override
    public void accept(byte[] pcmSamples, int length) {
        if (!accepting || length <= 0) {
            return;
        }
        byte[] copy = Arrays.copyOf(pcmSamples, length);
        queue.add(() -> recorder.captureAudio(copy));
    }

    public void stop(Path target) throws IOException {
        accepting = false;
        stopWorker();
        recorder.stop(target);
    }

    private void stopWorker() {
        worker.interrupt();
        try {
            worker.join(QUEUE_POLL_TIMEOUT_MS * 2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void drainQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Process any remaining queued items so no captured frames are lost during stop
        Runnable remaining;
        while ((remaining = queue.poll()) != null) {
            remaining.run();
        }
    }
}
