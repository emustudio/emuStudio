package net.emustudio.plugins.device.zxspectrum.ula.audio;

import java.util.Arrays;

/**
 * In-memory {@link AudioSink} used by tests to inspect the exact PCM stream produced by
 * {@link Beeper}.
 *
 * <p>It stores raw bytes in a growable primitive array instead of a {@code List<Byte>}. That
 * avoids boxing, repeated per-byte allocations, and per-element bounds checks in tight test loops.
 * The stored bytes are identical to what {@link SoundOutputSink} would receive, so assertions can
 * inspect the emulator output without depending on an actual sound device.
 *
 * <p>{@link #toShortArray()} decodes the captured stream as little-endian signed 16-bit PCM. That
 * matches the format configured by {@link SoundOutputSink} and the {@link java.nio.ByteBuffer}
 * writes performed by {@link Beeper}.
 *
 * <p>References:
 * <ul>
 * <li><a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/ByteBuffer.html">Oracle:
 * ByteBuffer</a></li>
 * <li><a href="https://docs.oracle.com/javase/9/docs/api/javax/sound/sampled/AudioFormat.html">Oracle:
 * AudioFormat</a></li>
 * </ul>
 */
public final class RecordingSink implements AudioSink {
    private byte[] samples = new byte[256];
    private int size;

    @Override
    public void write(byte[] data, int length) {
        ensureCapacity(size + length);
        System.arraycopy(data, 0, samples, size, length);
        size += length;
    }

    @Override
    public void flush() {
        size = 0;
    }

    /**
     * Decodes the captured byte stream into signed 16-bit PCM values.
     *
     * <p>The returned array is still interleaved by channel: index {@code 0} is left frame 0,
     * index {@code 1} is right frame 0, index {@code 2} is left frame 1, and so on.
     */
    public short[] toShortArray() {
        short[] output = new short[size / 2];
        for (int i = 0; i < output.length; i++) {
            int low = samples[i * 2] & 0xFF;
            int high = samples[i * 2 + 1] & 0xFF;
            output[i] = (short) (low | (high << 8));
        }
        return output;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= samples.length) {
            return;
        }
        samples = Arrays.copyOf(samples, Math.max(samples.length * 2, minCapacity));
    }
}
