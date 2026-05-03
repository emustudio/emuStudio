/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import net.jcip.annotations.ThreadSafe;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static net.emustudio.plugins.device.zxspectrum.ula.Constants.*;

/**
 * {@link AudioSink} implementation that forwards PCM batches to a Java Sound {@link SourceDataLine}.
 * It's a one-time usable sink that opens the line on construction and closes it on {@link #close()}. Once it is closed,
 * it won't accept any new samples.
 *
 * <p>{@link SourceDataLine#write(byte[], int, int)} may block until the mixer has room for more
 * frames. Emulator timing must not block on the host audio device, so this sink copies each beeper
 * batch into a bounded queue and lets a dedicated daemon thread perform the blocking writes. The
 * queue is intentionally lossy: when it is full, the latest chunk is discarded so audio stays close
 * to real time instead of building unbounded latency.
 *
 * <p>The line format is 16-bit signed stereo PCM, little-endian:
 * {@code new AudioFormat(sampleRate, 16, 2, true, false)}. The line buffer size is computed in
 * bytes and always represents an integral number of frames, which is required by
 * {@link SourceDataLine#write(byte[], int, int)}.
 *
 * <p>The writer thread polls the queue every 10 ms. That gives shutdown a bounded reaction time
 * after {@link Thread#interrupt()} without using a sentinel object, while still sleeping when the
 * emulator is silent.
 *
 * <p>References:
 * <ul>
 * <li><a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/sound/sampled/SourceDataLine.html">Oracle:
 * SourceDataLine</a></li>
 * <li><a href="https://docs.oracle.com/javase/9/docs/api/javax/sound/sampled/AudioFormat.html">Oracle:
 * AudioFormat</a></li>
 * </ul>
 */
@ThreadSafe
final class SoundAudioSink implements AudioSink {
    private static final int QUEUE_CAPACITY = 32;

    private final SourceDataLine line;
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread worker;
    private volatile boolean accepting = true;

    SoundAudioSink(int sampleRate) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
                sampleRate,
                Beeper.BYTES_PER_SAMPLE * 8,
                Beeper.CHANNELS,
                true,
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        // The line buffer must be large enough to avoid underruns while the writer thread drains the queue.
        // We pick the larger of two heuristics (both in bytes):
        //   - 16 × one batch:  batchFrames * FRAME_SIZE * 16  – keeps ~16 batches in the mixer buffer
        //   - half a second:   sampleRate  * FRAME_SIZE / 2   – guarantees a minimum duration of buffered audio
        int lineBufferSize = Math.max(
                Beeper.AUDIO_DEFAULT_BATCH_FRAMES * Beeper.FRAME_SIZE * 16, sampleRate * Beeper.FRAME_SIZE / 2);
        this.line.open(format, lineBufferSize);

        byte[] empty = new byte[lineBufferSize];
        Arrays.fill(empty, (byte) 0xFF);
        this.line.write(empty, 0, empty.length);  // Make initial sound "quiet"

        this.line.start();
        this.worker = new Thread(this::drainQueue, THREAD_NAME_PREFIX + "SoundOutputSink");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void accept(byte[] pcmSamples, int length) {
        if (accepting) {
            byte[] chunk = Arrays.copyOf(pcmSamples, length);
            if (!queue.offer(chunk)) {
                // Drop the newest chunk when the queue is full to keep latency bounded.
            }
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
                if (chunk == null) {
                    continue;
                }
                line.write(chunk, 0, chunk.length);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
