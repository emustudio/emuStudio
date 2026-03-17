package net.emustudio.plugins.device.zxspectrum.ula.audio;

import java.util.Arrays;

/**
 * In-memory {@link AudioSink} used by tests to inspect the exact PCM stream produced by
 * {@link Beeper}.
 */
public final class RecordingAudioSink implements AudioSink {
    private byte[] samples = new byte[256];
    private int size;

    @Override
    public void accept(byte[] pcmSamples, int length) {
        ensureCapacity(size + length);
        System.arraycopy(pcmSamples, 0, samples, size, length);
        size += length;
    }

    @Override
    public void flushAudio() {
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
