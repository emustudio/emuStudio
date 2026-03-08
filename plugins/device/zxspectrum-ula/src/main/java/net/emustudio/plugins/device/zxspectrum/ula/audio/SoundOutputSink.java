package net.emustudio.plugins.device.zxspectrum.ula.audio;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link AudioSink} implementation that forwards PCM batches to a Java Sound
 * {@link SourceDataLine}.
 *
 * <p>{@link SourceDataLine#write(byte[], int, int)} may block until the mixer has room for more
 * frames. Emulator timing must not block on the host audio device, so this sink copies each beeper
 * batch into a bounded queue and lets a dedicated daemon thread perform the blocking writes. The
 * queue is intentionally lossy: when it is full, the oldest chunk is discarded so audio stays close
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
final class SoundOutputSink implements AudioSink {
    private static final int QUEUE_CAPACITY = 32;
    private static final long QUEUE_POLL_TIMEOUT_MS = 10;

    private final SourceDataLine line;
    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread writerThread;

    SoundOutputSink(int sampleRate, int batchFrames) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(sampleRate, 16, Beeper.CHANNELS, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);
        int lineBufferSize = Math.max(batchFrames * Beeper.FRAME_SIZE * 16, sampleRate * Beeper.FRAME_SIZE / 2);
        this.line.open(format, lineBufferSize);
        this.line.start();
        this.writerThread = new Thread(this::drainQueue, "ZX-Spectrum-48K-SoundOutputSink");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    @Override
    public void write(byte[] samples, int length) {
        byte[] chunk = Arrays.copyOf(samples, length);
        if (!queue.offer(chunk)) {
            queue.poll();
            queue.offer(chunk);
        }
    }

    @Override
    public void flush() {
        queue.clear();
        line.flush();
    }

    @Override
    public void close() {
        writerThread.interrupt();
        queue.clear();
        line.stop();
        line.flush();
        try {
            writerThread.join(QUEUE_POLL_TIMEOUT_MS * 2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        line.close();
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
