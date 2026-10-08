/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

import net.emustudio.emulib.runtime.audio.AudioSink;
import net.emustudio.plugins.device.ay38910.audio.RecordingAudioSink;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Ay38910ChipTest {
    private static final int CPU_CLOCK_KHZ = 3_500;
    private static final int CPU_CLOCK_HZ = CPU_CLOCK_KHZ * 1000;

    @Test
    public void testRegisterWritesAreMaskedAndReadable() {
        Ay38910Chip chip = silentChip();

        chip.write(Ay38910Chip.SELECT_REGISTER_PORT, (byte) 0x01);
        chip.write(Ay38910Chip.DATA_PORT, (byte) 0xFF);

        chip.write(Ay38910Chip.SELECT_REGISTER_PORT, (byte) 0x01);
        assertEquals(0x0F, chip.read(Ay38910Chip.SELECT_REGISTER_PORT) & 0xFF);
        // The data port (0xBFFD) is write-only; reading it must not return the register value.
        assertEquals(0xFF, chip.read(Ay38910Chip.DATA_PORT) & 0xFF);
    }

    @Test
    public void testRegisterSelectDataAndReadAliases() {
        Ay38910Chip chip = silentChip();
        int[] selectPorts = {0xFFFD, 0xFEFD, 0xDFFD, 0xC001};
        int[] dataPorts = {0xBFFD, 0xBEFD, 0x9FFD, 0x8001};
        for (int i = 0; i < selectPorts.length; i++) {
            chip.write(selectPorts[i], (byte) 1);
            chip.write(dataPorts[i], (byte) (0xF0 | i));
            for (int selectPort : selectPorts) {
                assertEquals(i, chip.read(selectPort) & 0xFF);
            }
            assertEquals(0xFF, chip.read(dataPorts[i]) & 0xFF);
        }
    }

    @Test
    public void testNonAyPortsDoNotSelectOrWriteRegisters() {
        Ay38910Chip chip = silentChip();
        writeRegister(chip, 1, 3);
        writeRegister(chip, 0, 0x12);
        for (int port : new int[]{0x7FFD, 0x3FFD, 0xFFFE, 0xBFFE, 0xFFFF, 0xBFFF}) {
            chip.write(port, (byte) 1);
            chip.write(port, (byte) 0x56);
            assertEquals(0x12, chip.read(Ay38910Chip.SELECT_REGISTER_PORT) & 0xFF);
            assertEquals(0xFF, chip.read(port) & 0xFF);
        }
        chip.write(Ay38910Chip.SELECT_REGISTER_PORT, (byte) 1);
        assertEquals(3, chip.read(Ay38910Chip.SELECT_REGISTER_PORT) & 0xFF);
    }

    @Test
    public void testOutdDataPortAliasProducesAudio() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Ay38910Chip chip = new Ay38910Chip(sink, Ay38910Chip.DEFAULT_SAMPLE_RATE, () -> CPU_CLOCK_KHZ);
        int[][] registers = {{0, 0x20}, {1, 0}, {7, 0x38}, {8, 0x0F}};
        for (int[] register : registers) {
            chip.write(Ay38910Chip.SELECT_REGISTER_PORT, (byte) register[0]);
            // OUTD decrements B before output: BC=BFFD becomes BEFD on the address bus.
            chip.write(0xBEFD, (byte) register[1]);
        }
        chip.passedCycles(100_000);
        chip.close();
        assertTrue(hasPositive(sink.toShortArray()));
        assertTrue(hasNegative(sink.toShortArray()));
    }

    @Test
    public void testToneGenerationProducesPositiveAndNegativeSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Ay38910Chip chip = new Ay38910Chip(sink, Ay38910Chip.DEFAULT_SAMPLE_RATE, () -> CPU_CLOCK_KHZ);

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
    public void testSampleRateMatchesConfiguredOutputRate() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Ay38910Chip chip = new Ay38910Chip(sink, Ay38910Chip.DEFAULT_SAMPLE_RATE, () -> CPU_CLOCK_KHZ);

        writeRegister(chip, 0, 0x20);
        writeRegister(chip, 7, 0b00111000);
        writeRegister(chip, 8, 0x0F);

        chip.passedCycles(CPU_CLOCK_HZ); // exactly one second of CPU cycles
        chip.close();

        int frames = sink.toShortArray().length / Ay38910Chip.CHANNELS;
        int expected = Ay38910Chip.DEFAULT_SAMPLE_RATE;
        assertTrue("Expected ~" + expected + " frames for one second but got " + frames,
                Math.abs(frames - expected) <= expected / 100);
    }

    @Test
    public void testEnvelopeModeChangesAmplitudeOverTime() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Ay38910Chip chip = new Ay38910Chip(sink, Ay38910Chip.DEFAULT_SAMPLE_RATE, () -> CPU_CLOCK_KHZ);

        writeRegister(chip, 0, 0x10);
        writeRegister(chip, 1, 0x00);
        writeRegister(chip, 7, 0b00111000);
        writeRegister(chip, 8, 0x10);
        writeRegister(chip, 11, 0x00);
        writeRegister(chip, 12, 0x06);
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
        Ay38910Chip chip = silentChip();

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

    private static Ay38910Chip silentChip() {
        return new Ay38910Chip(AudioSink.NULL, Ay38910Chip.DEFAULT_SAMPLE_RATE, () -> CPU_CLOCK_KHZ);
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
