/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.ay38910.audio.AudioSink;
import net.emustudio.plugins.device.ay38910.audio.SoundAudioSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * AY-3-8910 Programmable Sound Generator connected through ZX Spectrum style ports.
 *
 * <p>The implementation models the three tone channels, shared noise generator, shared envelope
 * generator, and the 16 visible PSG registers. Register writes are timed from CPU T-states via
 * {@link CPUContext.PassedCyclesListener}, then resampled into host PCM audio at a fixed rate.
 */
public class Ay38910Chip implements Context8080.CpuPortDevice, CPUContext.PassedCyclesListener, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ay38910Chip.class);

    public static final int SELECT_REGISTER_PORT = 0xFFFD;
    public static final int DATA_PORT = 0xBFFD;

    public static final int DEFAULT_SAMPLE_RATE = 48_000;
    public static final int CHANNELS = 2;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE;

    private static final int REGISTERS_COUNT = 16;
    private static final int DEFAULT_VOLUME = 100;
    private static final int MAX_SAMPLE_AMPLITUDE = (int) (Short.MAX_VALUE * 0.18);
    private static final int DEFAULT_CPU_CLOCK_HZ = 3_500_000;
    private static final int WAVEFORM_SAMPLES = 2048;

    private static final int[] REGISTER_MASKS = {
            0xFF, 0x0F, 0xFF, 0x0F, 0xFF, 0x0F, 0x1F, 0xFF,
            0x1F, 0x1F, 0x1F, 0xFF, 0xFF, 0x0F, 0xFF, 0xFF
    };

    // Common 16-step amplitude approximation used by AY/YM emulators.
    private static final double[] AMPLITUDE_LEVELS = {
            0.000000, 0.004654, 0.007721, 0.010956,
            0.016998, 0.025150, 0.037519, 0.055364,
            0.080867, 0.117825, 0.170744, 0.246387,
            0.354376, 0.516394, 0.732772, 1.000000
    };

    private final AudioSink primarySink;
    private final int sampleRate;
    private final ByteBuffer sampleBuffer;
    private final short[] waveformBuffer = new short[WAVEFORM_SAMPLES];

    private final int[] registers = new int[REGISTERS_COUNT];
    private final long[] toneCounters = new long[3];
    private final boolean[] toneOutputs = new boolean[]{true, true, true};

    private long noiseCounter;
    private int noiseLfsr = 0x1FFFF;
    private boolean noiseOutput = true;

    private long envelopeCounter;
    private int envelopeVolume;
    private int envelopeStep;
    private int envelopeDirection;
    private boolean envelopeHolding;

    private long samplePhase;
    private int selectedRegister;
    private int volumePercent = DEFAULT_VOLUME;
    private int waveformWriteIndex;
    private boolean waveformBufferFilled;
    private int cpuClockHz = DEFAULT_CPU_CLOCK_HZ;

    public static Ay38910Chip createDefault() {
        try {
            return new Ay38910Chip(new SoundAudioSink(DEFAULT_SAMPLE_RATE), DEFAULT_SAMPLE_RATE);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            LOGGER.warn("AY-3-8910 tone output is unavailable; continuing without sound", e);
            return silent();
        }
    }

    public static Ay38910Chip silent() {
        return new Ay38910Chip(AudioSink.NULL, DEFAULT_SAMPLE_RATE);
    }

    Ay38910Chip(AudioSink sink, int sampleRate) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be > 0");
        }
        this.primarySink = Objects.requireNonNull(sink);
        this.sampleRate = sampleRate;
        this.sampleBuffer = ByteBuffer.allocate(Constants.AUDIO_DEFAULT_BATCH_FRAMES * FRAME_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        reset();
    }

    @Override
    public synchronized byte read(int portAddress) {
        if (portAddress == DATA_PORT) {
            return (byte) registers[selectedRegister];
        }
        return (byte) 0xFF;
    }

    @Override
    public synchronized void write(int portAddress, byte data) {
        if (portAddress == SELECT_REGISTER_PORT) {
            selectedRegister = data & 0x0F;
            return;
        }
        if (portAddress != DATA_PORT) {
            return;
        }

        int value = data & REGISTER_MASKS[selectedRegister];
        registers[selectedRegister] = value;

        if (selectedRegister == 13) {
            restartEnvelope();
        }
    }

    @Override
    public String getName() {
        return "AY-3-8910";
    }

    @Override
    public synchronized void passedCycles(long cycles) {
        if (cycles <= 0) {
            return;
        }

        long remaining = cycles;
        while (remaining > 0) {
            long cyclesUntilSample = cyclesUntilNextSample();
            long step = Math.min(remaining, cyclesUntilSample);
            advanceGenerators(step);
            remaining -= step;

            samplePhase += step * sampleRate;
            if (samplePhase >= cpuClockHz) {
                samplePhase -= cpuClockHz;
                writeSample(mixSample());
            }
        }
    }

    public synchronized void reset() {
        Arrays.fill(registers, 0);
        Arrays.fill(toneCounters, 0);
        Arrays.fill(toneOutputs, true);
        Arrays.fill(waveformBuffer, (short) 0);
        noiseCounter = 0;
        noiseLfsr = 0x1FFFF;
        noiseOutput = true;
        envelopeCounter = 0;
        envelopeVolume = 0;
        envelopeStep = 0;
        envelopeDirection = -1;
        envelopeHolding = false;
        samplePhase = 0;
        selectedRegister = 0;
        waveformWriteIndex = 0;
        waveformBufferFilled = false;
        sampleBuffer.clear();
        primarySink.flushAudio();
    }

    public synchronized int getSampleRate() {
        return sampleRate;
    }

    public synchronized int getVolumePercent() {
        return volumePercent;
    }

    public synchronized void setVolumePercent(int volumePercent) {
        this.volumePercent = Math.max(0, Math.min(100, volumePercent));
    }

    synchronized void setCpuFrequencyKHz(int cpuFrequencyKHz) {
        if (cpuFrequencyKHz <= 0) {
            throw new IllegalArgumentException("CPU frequency must be > 0 kHz");
        }
        this.cpuClockHz = cpuFrequencyKHz * 1000;
    }

    synchronized int getCpuClockHz() {
        return cpuClockHz;
    }

    public synchronized short[] copyRecentWaveform() {
        int size = waveformBufferFilled ? waveformBuffer.length : waveformWriteIndex;
        short[] output = new short[size];
        if (size == 0) {
            return output;
        }
        if (!waveformBufferFilled) {
            System.arraycopy(waveformBuffer, 0, output, 0, size);
            return output;
        }

        int tail = waveformBuffer.length - waveformWriteIndex;
        System.arraycopy(waveformBuffer, waveformWriteIndex, output, 0, tail);
        System.arraycopy(waveformBuffer, 0, output, tail, waveformWriteIndex);
        return output;
    }

    @Override
    public synchronized void close() {
        flushSamples();
        primarySink.close();
    }

    private long cyclesUntilNextSample() {
        long remainingPhase = cpuClockHz - samplePhase;
        return Math.max(1L, (remainingPhase + sampleRate - 1) / sampleRate);
    }

    private void advanceGenerators(long cpuCycles) {
        for (int channel = 0; channel < 3; channel++) {
            advanceTone(channel, cpuCycles);
        }
        advanceNoise(cpuCycles);
        advanceEnvelope(cpuCycles);
    }

    private void advanceTone(int channel, long cpuCycles) {
        long halfPeriod = 16L * Math.max(1, tonePeriod(channel));
        toneCounters[channel] += cpuCycles;
        long toggles = toneCounters[channel] / halfPeriod;
        if (toggles > 0) {
            toneCounters[channel] %= halfPeriod;
            if ((toggles & 1L) != 0) {
                toneOutputs[channel] = !toneOutputs[channel];
            }
        }
    }

    private void advanceNoise(long cpuCycles) {
        long stepPeriod = 32L * Math.max(1, registers[6] & 0x1F);
        noiseCounter += cpuCycles;
        long steps = noiseCounter / stepPeriod;
        if (steps == 0) {
            return;
        }
        noiseCounter %= stepPeriod;
        while (steps-- > 0) {
            int feedback = (noiseLfsr ^ (noiseLfsr >> 3)) & 0x01;
            noiseLfsr = (noiseLfsr >> 1) | (feedback << 16);
            noiseOutput = (noiseLfsr & 0x01) != 0;
        }
    }

    private void advanceEnvelope(long cpuCycles) {
        long stepPeriod = 512L * Math.max(1, envelopePeriod());
        envelopeCounter += cpuCycles;
        long steps = envelopeCounter / stepPeriod;
        if (steps == 0) {
            return;
        }
        envelopeCounter %= stepPeriod;
        while (steps-- > 0) {
            advanceEnvelopeStep();
        }
    }

    private void advanceEnvelopeStep() {
        if (envelopeHolding) {
            return;
        }

        if (envelopeStep < 15) {
            envelopeStep++;
            envelopeVolume = clampEnvelopeVolume(envelopeVolume + envelopeDirection);
            return;
        }

        int shape = registers[13] & 0x0F;
        boolean continueFlag = (shape & 0x08) != 0;
        boolean alternateFlag = (shape & 0x02) != 0;
        boolean holdFlag = (shape & 0x01) != 0;

        if (!continueFlag) {
            envelopeHolding = true;
            envelopeVolume = 0;
            return;
        }

        if (holdFlag) {
            envelopeHolding = true;
            envelopeVolume = alternateFlag ? initialEnvelopeVolume(shape) : finalEnvelopeVolume(shape);
            return;
        }

        if (alternateFlag) {
            envelopeDirection = -envelopeDirection;
        }

        envelopeStep = 0;
        envelopeVolume = envelopeDirection > 0 ? 0 : 15;
    }

    private void restartEnvelope() {
        envelopeCounter = 0;
        envelopeStep = 0;
        envelopeHolding = false;
        envelopeDirection = ((registers[13] & 0x04) != 0) ? 1 : -1;
        envelopeVolume = initialEnvelopeVolume(registers[13]);
    }

    private int initialEnvelopeVolume(int shape) {
        return ((shape & 0x04) != 0) ? 0 : 15;
    }

    private int finalEnvelopeVolume(int shape) {
        return ((shape & 0x04) != 0) ? 15 : 0;
    }

    private int clampEnvelopeVolume(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 15);
    }

    private int tonePeriod(int channel) {
        int fine = registers[channel * 2];
        int coarse = registers[channel * 2 + 1] & 0x0F;
        return fine | (coarse << 8);
    }

    private int envelopePeriod() {
        return registers[11] | (registers[12] << 8);
    }

    private short mixSample() {
        double mixed = 0.0;
        for (int channel = 0; channel < 3; channel++) {
            mixed += channelLevel(channel);
        }
        mixed /= 3.0;

        int pcm = (int) Math.round(mixed * MAX_SAMPLE_AMPLITUDE * volumePercent / 100.0);
        if (pcm > Short.MAX_VALUE) {
            pcm = Short.MAX_VALUE;
        } else if (pcm < Short.MIN_VALUE) {
            pcm = Short.MIN_VALUE;
        }
        return (short) pcm;
    }

    private double channelLevel(int channel) {
        int amplitudeRegister = registers[8 + channel];
        int amplitude = (amplitudeRegister & 0x10) != 0 ? envelopeVolume : (amplitudeRegister & 0x0F);
        if (amplitude == 0) {
            return 0.0;
        }

        boolean toneEnabled = (registers[7] & (1 << channel)) == 0;
        boolean noiseEnabled = (registers[7] & (1 << (channel + 3))) == 0;
        if (!toneEnabled && !noiseEnabled) {
            return 0.0;
        }

        boolean gateHigh = (!toneEnabled || toneOutputs[channel]) && (!noiseEnabled || noiseOutput);
        double level = AMPLITUDE_LEVELS[amplitude];
        return gateHigh ? level : -level;
    }

    private void writeSample(short sampleValue) {
        waveformBuffer[waveformWriteIndex] = sampleValue;
        waveformWriteIndex = (waveformWriteIndex + 1) % waveformBuffer.length;
        if (waveformWriteIndex == 0) {
            waveformBufferFilled = true;
        }
        sampleBuffer.putShort(sampleValue);
        sampleBuffer.putShort(sampleValue);
        if (!sampleBuffer.hasRemaining()) {
            flushSamples();
        }
    }

    private void flushSamples() {
        int length = sampleBuffer.position();
        if (length == 0) {
            return;
        }
        primarySink.accept(sampleBuffer.array(), length);
        sampleBuffer.clear();
    }
}
