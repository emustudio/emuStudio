/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910.audio;

import net.emustudio.plugins.device.ay38910.Ay38910Chip;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static net.emustudio.plugins.device.ay38910.Constants.THREAD_NAME_PREFIX;

public final class SoundAudioSink implements AudioSink {
    private static final int QUEUE_CAPACITY = 32;
    private static final long QUEUE_POLL_TIMEOUT_MS = 10;

    private final SourceDataLine line;
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread worker;
    private volatile boolean accepting = true;

    public SoundAudioSink(int sampleRate) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
                sampleRate,
                Ay38910Chip.BYTES_PER_SAMPLE * 8,
                Ay38910Chip.CHANNELS,
                true,
                false
        );
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        int lineBufferSize = Math.max(
                Ay38910Chip.AUDIO_DEFAULT_BATCH_FRAMES * Ay38910Chip.FRAME_SIZE * 16,
                sampleRate * Ay38910Chip.FRAME_SIZE / 2
        );
        this.line.open(format, lineBufferSize);

        byte[] empty = new byte[lineBufferSize];
        Arrays.fill(empty, (byte) 0x00);
        this.line.write(empty, 0, empty.length);

        this.line.start();
        this.worker = new Thread(this::drainQueue, THREAD_NAME_PREFIX + "SoundOutputSink");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void accept(byte[] pcmSamples, int length) {
        if (accepting) {
            queue.offer(Arrays.copyOf(pcmSamples, length));
        }
    }

    @Override
    public void flushAudio() {
        queue.clear();
        line.flush();
    }

    @Override
    public void close() {
        accepting = false;
        worker.interrupt();
        queue.clear();
        try {
            worker.join(QUEUE_POLL_TIMEOUT_MS * 2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            line.stop();
            line.flush();
            line.close();
        }
    }

    private void drainQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] chunk = queue.poll(QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (chunk != null) {
                    line.write(chunk, 0, chunk.length);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
