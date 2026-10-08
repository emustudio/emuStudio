/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Writes captured frames on a worker, then exports off the Swing event thread. */
final class RecordingSession {
    static final int FRAME_RATE = 60;
    static final int AUDIO_FRAMES_PER_VIDEO_FRAME = SampleSoundOutput.RECORDING_SAMPLE_RATE / FRAME_RATE;
    private static final Runnable STOP = () -> { };
    final int width;
    final int height;
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final VideoRecorder recorder;
    private final Thread worker;
    private boolean accepting = true;

    RecordingSession(int width, int height) throws IOException {
        this.width = width;
        this.height = height;
        recorder = new VideoRecorder(width, height, 1, FRAME_RATE, SampleSoundOutput.RECORDING_SAMPLE_RATE);
        worker = new Thread(this::drainQueue, "space-invaders-recording");
        worker.setDaemon(true);
        worker.start();
    }

    synchronized void capture(BufferedImage frame, byte[] audio) {
        if (accepting) {
            queue.add(() -> {
                recorder.captureVideo(frame);
                recorder.captureAudio(audio);
            });
        }
    }

    void stop(Path target) throws IOException {
        synchronized (this) {
            if (!accepting) {
                return;
            }
            accepting = false;
            queue.add(STOP);
        }
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while stopping Space Invaders recording", e);
        }
        recorder.stop(target);
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
