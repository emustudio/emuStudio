/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Resamples the ZX Spectrum's EAR/MIC output line into host PCM audio.
 *
 * <p>The ULA exposes two output bits on port {@code 0xFE}: EAR/internal speaker on bit 4 and MIC
 * on bit 3 (active low). On real 48K hardware these are not independent digital channels; they
 * share the same analog node through resistors and therefore produce four observable voltage levels.
 * This class uses the Issue 3 levels from the hardware reference and converts them into signed PCM
 * amplitudes. That preserves the important property that MIC changes the loudness slightly, but it
 * does not invert the waveform or create a separate square wave.
 *
 * <p>The math is fixed-point time accumulation:
 * <pre>
 * accumulator += passedCycles * sampleRate
 * samplesToGenerate = accumulator / cpuFrequency
 * accumulator %= cpuFrequency
 * </pre>
 * {@code cpuFrequency} is the ZX Spectrum clock in T-states per second and {@code sampleRate} is
 * the host audio rate in frames per second. The ratio is not integral
 * ({@code 3_500_000 / 48_000 ~= 72.9167}), so most calls end in the middle of a host sample.
 * Carrying the remainder in {@code sampleTickRemainder} preserves long-term timing and avoids
 * drift.
 *
 * <p>Each generated frame writes the same signed 16-bit amplitude to left and right channels. The
 * amplitudes are derived once from the Issue 3 voltage table by centering the analog range around
 * its midpoint and scaling it into 16-bit PCM. The backing {@link ByteBuffer} is little-endian to
 * match the Java Sound format created by {@link SoundOutputSink}.
 *
 * <p>References:
 * <ul>
 * <li><a href="https://worldofspectrum.org/faq/reference/48kreference.htm">World of Spectrum:
 * 48K ZX Spectrum Technical Information</a></li>
 * <li><a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/ByteBuffer.html">Oracle:
 * ByteBuffer</a></li>
 * <li><a href="https://docs.oracle.com/javase/9/docs/api/javax/sound/sampled/AudioFormat.html">Oracle:
 * AudioFormat</a></li>
 * </ul>
 */
public class Beeper implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Beeper.class);

    static final int ZX_SPECTRUM_FREQUENCY = 3_500_000;
    static final int DEFAULT_SAMPLE_RATE = 48_000;
    private static final int DEFAULT_BATCH_FRAMES = 512;

    public static final int CHANNELS = 2;
    private static final int BYTES_PER_SAMPLE = 2;
    public static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE;
    private static final int MAX_SAMPLE_AMPLITUDE = (int) (Short.MAX_VALUE * 0.70);
    private static final double ISSUE_3_MIC_ONLY_VOLTAGE = 0.34;
    private static final double ISSUE_3_BOTH_OFF_VOLTAGE = 0.66;
    private static final double ISSUE_3_BOTH_ON_VOLTAGE = 3.56;
    private static final double ISSUE_3_EAR_ONLY_VOLTAGE = 3.70;
    private static final short[] ISSUE_3_PCM_LEVELS = createIssue3PcmLevels();

    private final AudioSink sink;
    private final int cpuFrequency;
    private final int sampleRate;
    private final ByteBuffer sampleBuffer;

    private short currentSampleValue;
    private boolean audioStarted;
    // Carries the fractional part of cycles -> samples conversion between calls.
    private long sampleTickRemainder;

    public static Beeper createDefault() {
        try {
            return new Beeper(
                    new SoundOutputSink(DEFAULT_SAMPLE_RATE, DEFAULT_BATCH_FRAMES),
                    ZX_SPECTRUM_FREQUENCY, DEFAULT_SAMPLE_RATE, DEFAULT_BATCH_FRAMES);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            LOGGER.warn("ZX Spectrum tone output is unavailable; continuing without sound", e);
            return silent();
        }
    }

    public static Beeper silent() {
        return new Beeper(AudioSink.NULL, ZX_SPECTRUM_FREQUENCY, DEFAULT_SAMPLE_RATE, DEFAULT_BATCH_FRAMES);
    }

    public Beeper(AudioSink sink, int cpuFrequency, int sampleRate, int batchFrames) {
        if (cpuFrequency <= 0) {
            throw new IllegalArgumentException("CPU frequency must be > 0");
        }
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be > 0");
        }
        this.sink = sink;
        this.cpuFrequency = cpuFrequency;
        this.sampleRate = sampleRate;
        this.sampleBuffer = ByteBuffer.allocate(batchFrames * FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void setLevel(boolean levelHigh) {
        setLevel(levelHigh, false);
    }

    /**
     * Sets the current EAR/MIC output state from port {@code 0xFE}.
     *
     * <p>{@code earOn} corresponds to bit 4, {@code micOn} corresponds to active-low bit 3. The
     * next PCM frames generated by {@link #passedCycles(long)} will use the mapped Issue 3 output
     * level until it changes again.
     */
    public void setLevel(boolean earOn, boolean micOn) {
        audioStarted |= earOn || micOn;
        currentSampleValue = audioStarted ? ISSUE_3_PCM_LEVELS[audioLevelIndex(earOn, micOn)] : 0;
    }

    /**
     * Converts elapsed CPU T-states into host PCM frames.
     *
     * <p>This method does not emit one audio frame per call. It emits
     * {@code floor((remainder + cycles * sampleRate) / cpuFrequency)} frames, then carries the
     * leftover fractional part into the next call.
     */
    public void passedCycles(long cycles) {
        if (cycles <= 0) {
            return;
        }

        sampleTickRemainder += cycles * sampleRate;
        long samplesToGenerate = sampleTickRemainder / cpuFrequency;
        sampleTickRemainder %= cpuFrequency;

        for (long i = 0; i < samplesToGenerate; i++) {
            writeSample(currentSampleValue);
        }
    }

    /**
     * Drops buffered PCM data and returns the beeper to a silent low level.
     */
    public void reset() {
        currentSampleValue = 0;
        audioStarted = false;
        sampleTickRemainder = 0;
        sampleBuffer.clear();
        sink.flush();
    }

    @Override
    public void close() {
        flushSamples();
        sink.close();
    }

    private void writeSample(short sampleValue) {
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
        sink.write(sampleBuffer.array(), length);
        sampleBuffer.clear();
    }

    private static int audioLevelIndex(boolean earOn, boolean micOn) {
        return (earOn ? 2 : 0) | (micOn ? 1 : 0);
    }

    private static short[] createIssue3PcmLevels() {
        double[] voltages = {
                ISSUE_3_BOTH_OFF_VOLTAGE,
                ISSUE_3_MIC_ONLY_VOLTAGE,
                ISSUE_3_EAR_ONLY_VOLTAGE,
                ISSUE_3_BOTH_ON_VOLTAGE
        };
        double midpoint = (ISSUE_3_MIC_ONLY_VOLTAGE + ISSUE_3_EAR_ONLY_VOLTAGE) / 2.0;
        double maxDeviation = ISSUE_3_EAR_ONLY_VOLTAGE - midpoint;
        short[] levels = new short[voltages.length];
        for (int i = 0; i < voltages.length; i++) {
            double normalized = (voltages[i] - midpoint) / maxDeviation;
            levels[i] = (short) Math.round(normalized * MAX_SAMPLE_AMPLITUDE);
        }
        return levels;
    }
}
