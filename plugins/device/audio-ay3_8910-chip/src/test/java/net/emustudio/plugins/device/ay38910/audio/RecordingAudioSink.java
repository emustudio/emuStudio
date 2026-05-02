/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910.audio;

import java.util.Arrays;

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
