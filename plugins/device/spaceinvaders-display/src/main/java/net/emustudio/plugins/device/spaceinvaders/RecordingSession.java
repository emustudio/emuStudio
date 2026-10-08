/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Encodes captured frames on a worker, then finalizes off the Swing event thread. */
final class RecordingSession {
    static final int FRAME_RATE = 60;
    static final int AUDIO_FRAMES_PER_VIDEO_FRAME = SampleSoundOutput.RECORDING_SAMPLE_RATE / FRAME_RATE;
    private static final int QUEUE_CAPACITY = FRAME_RATE * 2;
    private static final Runnable STOP = () -> { };
    final int width;
    final int height;
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final VideoRecorder recorder;
    private final Thread worker;
    private boolean accepting = true;
    private boolean stopping;
    private volatile IOException failure;

    RecordingSession(int width, int height) throws IOException {
        this.width = width;
        this.height = height;
        recorder = new VideoRecorder(width, height, 1, FRAME_RATE, SampleSoundOutput.RECORDING_SAMPLE_RATE);
        worker = new Thread(this::drainQueue, "space-invaders-recording");
        worker.setDaemon(true);
        worker.start();
    }

    synchronized boolean capture(BufferedImage frame, byte[] audio) {
        if (!accepting || failure != null) {
            return false;
        }
        boolean queued = queue.offer(() -> {
            recorder.captureVideo(frame);
            recorder.captureAudio(audio);
            IOException captureFailure = recorder.getCaptureFailure();
            if (captureFailure != null) {
                failure = captureFailure;
            }
        });
        if (!queued) {
            failure = new IOException("Video encoder could not keep up with recording");
        }
        return queued;
    }

    IOException getFailure() {
        return failure;
    }

    void stop(Path target) throws IOException {
        synchronized (this) {
            if (stopping) {
                return;
            }
            stopping = true;
            accepting = false;
            if (target == null || failure != null) {
                queue.clear();
            }
        }
        try {
            queue.put(STOP);
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while stopping Space Invaders recording", e);
        }
        if (failure != null) {
            recorder.stop(null);
            if (target != null) {
                throw failure;
            }
        } else {
            recorder.stop(target);
        }
    }

    private void drainQueue() {
        try {
            Runnable task;
            while ((task = queue.take()) != STOP) {
                task.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
