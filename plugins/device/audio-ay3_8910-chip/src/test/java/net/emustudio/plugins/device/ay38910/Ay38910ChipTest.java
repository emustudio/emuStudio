/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

import net.emustudio.plugins.device.ay38910.audio.RecordingAudioSink;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ay38910ChipTest {
    private static final int CPU_CLOCK_HZ = 3_500_000;

    @Test
    public void testRegisterWritesAreMaskedAndReadable() {
        Ay38910Chip chip = Ay38910Chip.silent();

        chip.write(Ay38910Chip.SELECT_REGISTER_PORT, (byte) 0x01);
        chip.write(Ay38910Chip.DATA_PORT, (byte) 0xFF);

        chip.write(Ay38910Chip.SELECT_REGISTER_PORT, (byte) 0x01);
        assertEquals(0x0F, chip.read(Ay38910Chip.DATA_PORT) & 0xFF);
    }

    @Test
    public void testToneGenerationProducesPositiveAndNegativeSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Ay38910Chip chip = new Ay38910Chip(sink, Ay38910Chip.DEFAULT_SAMPLE_RATE);

        writeRegister(chip, 0, 0x20);
        writeRegister(chip, 1, 0x00);
        writeRegister(chip, 7, 0b00111000);
        writeRegister(chip, 8, 0x0F);

        chip.passedCycles(100_000);
        chip.close();

        short[] samples = sink.toShortArray();
        assertTrue(hasPositive(samples));
        assertTrue(hasNegative(samples));
    }

    @Test
    public void testEnvelopeModeChangesAmplitudeOverTime() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Ay38910Chip chip = new Ay38910Chip(sink, Ay38910Chip.DEFAULT_SAMPLE_RATE);

        writeRegister(chip, 0, 0x10);
        writeRegister(chip, 1, 0x00);
        writeRegister(chip, 7, 0b00111000);
        writeRegister(chip, 8, 0x10);
        writeRegister(chip, 11, 0x01);
        writeRegister(chip, 12, 0x00);
        writeRegister(chip, 13, 0x0D);

        chip.passedCycles(CPU_CLOCK_HZ / 5);
        chip.close();

        short[] samples = leftChannel(sink.toShortArray());
        int firstPeak = maxAbs(samples, 0, Math.min(32, samples.length));
        int laterPeak = maxAbs(samples, Math.max(0, samples.length / 2), samples.length);

        assertTrue("Envelope should increase amplitude over time", laterPeak > firstPeak);
    }

    @Test
    public void testWaveformBufferTracksGeneratedSamplesAndVolumeIsClamped() {
        Ay38910Chip chip = Ay38910Chip.silent();

        chip.setVolumePercent(150);
        assertEquals(100, chip.getVolumePercent());

        writeRegister(chip, 0, 0x20);
        writeRegister(chip, 1, 0x00);
        writeRegister(chip, 7, 0b00111000);
        writeRegister(chip, 8, 0x0F);

        chip.passedCycles(50_000);

        assertTrue(chip.copyRecentWaveform().length > 0);
    }

    private static void writeRegister(Ay38910Chip chip, int register, int value) {
        chip.write(Ay38910Chip.SELECT_REGISTER_PORT, (byte) register);
        chip.write(Ay38910Chip.DATA_PORT, (byte) value);
    }

    private static boolean hasPositive(short[] samples) {
        for (short sample : samples) {
            if (sample > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNegative(short[] samples) {
        for (short sample : samples) {
            if (sample < 0) {
                return true;
            }
        }
        return false;
    }

    private static short[] leftChannel(short[] interleavedSamples) {
        short[] output = new short[interleavedSamples.length / 2];
        for (int i = 0; i < output.length; i++) {
            output[i] = interleavedSamples[i * 2];
        }
        return output;
    }

    private static int maxAbs(short[] samples, int fromInclusive, int toExclusive) {
        int result = 0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            result = Math.max(result, Math.abs(samples[i]));
        }
        return result;
    }
}
