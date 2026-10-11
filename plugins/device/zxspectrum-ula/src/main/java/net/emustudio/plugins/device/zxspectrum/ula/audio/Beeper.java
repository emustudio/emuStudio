/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import net.emustudio.emulib.runtime.audio.AudioSink;
import net.emustudio.emulib.runtime.helpers.ReadWriteLockSupport;
import net.emustudio.plugins.device.zxspectrum.bus.api.ModelProfile;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.LongSupplier;


/**
 * Resamples the ZX Spectrum's EAR/MIC output line into host PCM audio.
 *
 * <p>The ULA exposes two output bits on port {@code 0xFE}: EAR/internal speaker on bit 4 and MIC
 * on bit 3 (active low). On real 48K hardware these are not independent digital channels; they
 * share the same analog node through resistors and therefore produce four observable voltage levels.
 * The model profile supplies the hardware levels; this class converts them into signed PCM
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
 * amplitudes are derived once from the normalized model levels and scaled into 16-bit PCM. The
 * backing {@link ByteBuffer} is little-endian to match the PCM format accepted by {@link AudioSink}.
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
@ThreadSafe
public class Beeper implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Beeper.class);

    // Average output sample period at 48 kHz = 3,500,000 / 48,000 ≈ 72.9 CPU cycles per sample
    public static final int DEFAULT_SAMPLE_RATE = 48_000;

    private static final int DEFAULT_VOLUME = 100;

    public static final int CHANNELS = 2;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int FRAME_SIZE = CHANNELS * BYTES_PER_SAMPLE;
    public static final int AUDIO_DEFAULT_BATCH_FRAMES = 512;

    // Peak PCM amplitude used when mapping hardware voltages to 16-bit signed samples.
    // Set to 10% of Short.MAX_VALUE — square waves are perceived as louder than sine waves at equal
    // amplitude due to their rich harmonic content, so a low fraction keeps the beeper comfortable.
    private static final int MAX_SAMPLE_AMPLITUDE = (int) (Short.MAX_VALUE * 0.10);

    // Model hardware levels converted to host PCM once, indexed by EAR/MIC state.
    private final short[] pcmLevels = new short[4];

    // Live output sink (host audio).  Never null.
    private final AudioSink primarySink;
    // Optional recording sink, swapped atomically when recording starts/stops.
    // Defaults to AudioSink.NULL which silently discards all calls.
    private volatile AudioSink recordingSink = AudioSink.NULL;

    private final int sampleRate;
    private final LongSupplier cpuFrequencyHzSupplier;
    @GuardedBy("rwl")
    private final ByteBuffer sampleBuffer;
    // Guards all access to sampleBuffer, sampleTickRemainder, and the sample-level fields.
    // Required because the CPU thread drives passedCycles()/setLevel() during emulation while
    // the AWT thread can call setVolumePercent(), setRecordingSink(), flushRecordingBuffer(),
    // reset() and close() from the GUI at any time.
    private final ReadWriteLockSupport rwl = new ReadWriteLockSupport();

    @GuardedBy("rwl")
    private short currentSampleValue;
    @GuardedBy("rwl")
    private short rawSampleValue;
    @GuardedBy("rwl")
    private boolean audioStarted;
    private volatile int volumePercent = DEFAULT_VOLUME;
    // Carries the fractional part of cycles -> samples conversion between calls.
    @GuardedBy("rwl")
    private long sampleTickRemainder;

    public static Beeper createDefault(ModelProfile profile, LongSupplier cpuFrequencyHzSupplier) {
        try {
            return new Beeper(profile, AudioSink.open(DEFAULT_SAMPLE_RATE, CHANNELS), DEFAULT_SAMPLE_RATE, cpuFrequencyHzSupplier);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            LOGGER.warn("ZX Spectrum tone output is unavailable; continuing without sound", e);
            return silent(profile, cpuFrequencyHzSupplier);
        }
    }

    public static Beeper silent(ModelProfile profile, LongSupplier cpuFrequencyHzSupplier) {
        return new Beeper(profile, AudioSink.NULL, DEFAULT_SAMPLE_RATE, cpuFrequencyHzSupplier);
    }


    public Beeper(ModelProfile profile, AudioSink sink, int sampleRate, LongSupplier cpuFrequencyHzSupplier) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be > 0");
        }
        this.primarySink = java.util.Objects.requireNonNull(sink);
        this.sampleRate = sampleRate;
        this.cpuFrequencyHzSupplier = java.util.Objects.requireNonNull(cpuFrequencyHzSupplier);
        java.util.Objects.requireNonNull(profile);
        for (int i = 0; i < pcmLevels.length; i++) {
            pcmLevels[i] = (short) Math.round(profile.beeperLevel((i & 2) != 0, (i & 1) != 0) * MAX_SAMPLE_AMPLITUDE);
        }
        this.sampleBuffer = ByteBuffer.allocate(AUDIO_DEFAULT_BATCH_FRAMES * FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Sets the current EAR/MIC output state from port {@code 0xFE}.
     *
     * <p>{@code earOn} corresponds to bit 4, {@code micOn} corresponds to active-low bit 3. The
     * next PCM frames generated by {@link #passedCycles(long)} will use the mapped model output
     * level until it changes again.
     *
     * <p>The tape input does not drive the speaker on real hardware. When {@code tapeIn} is
     * {@code true}, a small additional amplitude is mixed in so that the familiar loading sounds
     * are audible through the emulated speaker output.
     *
     * @param earOn  true if EAR bit is on (bit 4 = 1), false if off (bit 4 = 0)
     * @param micOn  true if MIC bit is on (bit 3 = 0), false if off (bit 3 = 1)
     * @param tapeIn true if the tape input signal is high, false otherwise
     */
    public void setLevel(boolean earOn, boolean micOn, boolean tapeIn) {
        int index = (earOn ? 2 : 0) | (micOn ? 1 : 0);
        short pcm = pcmLevels[index];

        // Tape input does not drive the speaker on real hardware, but mixing it in at
        // reduced amplitude reproduces the familiar loading sounds heard through the TV speaker.
        if (tapeIn) {
            pcm = (short) Math.min(Short.MAX_VALUE, pcm + (MAX_SAMPLE_AMPLITUDE / 25));
        }

        short finalPcm = pcm;
        rwl.lockWrite(() -> {
            audioStarted |= earOn || micOn || tapeIn;
            rawSampleValue = audioStarted ? finalPcm : 0;
            currentSampleValue = scaleSample(rawSampleValue);
        });
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    public void setVolumePercent(int volumePercent) {
        this.volumePercent = Math.max(0, Math.min(100, volumePercent));
        rwl.lockWrite(() -> {
            currentSampleValue = scaleSample(rawSampleValue);
        });
    }

    public void setRecordingSink(AudioSink recordingSink) {
        this.recordingSink = java.util.Objects.requireNonNull(recordingSink);
    }

    /**
     * Flushes any buffered PCM samples to both sinks (live output and recording).
     *
     * <p>Call this before disconnecting the recording sink to ensure no audio data is lost
     * at the tail end of a recording.
     */
    public void flushRecordingBuffer() {
        rwl.lockWrite(this::flushSamples);
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

        rwl.lockWrite(() -> {
            sampleTickRemainder += cycles * sampleRate;
            long cpuFrequency = Math.max(1L, cpuFrequencyHzSupplier.getAsLong());
            long samplesToGenerate = sampleTickRemainder / cpuFrequency;
            sampleTickRemainder %= cpuFrequency;

            for (long i = 0; i < samplesToGenerate; i++) {
                writeSample(currentSampleValue);
            }
        });
    }

    /**
     * Drops buffered PCM data and returns the beeper to a silent low level.
     */
    public void reset() {
        rwl.lockWrite(() -> {
            rawSampleValue = 0;
            currentSampleValue = 0;
            audioStarted = false;
            sampleTickRemainder = 0;
            sampleBuffer.clear();
        });
        primarySink.flushAudio();
        recordingSink.flushAudio();
    }

    @Override
    public void close() {
        rwl.lockWrite(this::flushSamples);
        primarySink.close();
        recordingSink.close();
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
        byte[] array = sampleBuffer.array();
        primarySink.accept(array, length);
        recordingSink.accept(array, length);
        sampleBuffer.clear();
    }

    private short scaleSample(short sampleValue) {
        return (short) ((sampleValue * volumePercent) / 100);
    }

}
