/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import org.junit.Test;

import java.util.function.Consumer;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.ZX_48K_CPU_FREQUENCY;
import static net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper.AUDIO_DEFAULT_BATCH_FRAMES;
import static org.junit.Assert.*;

public class BeeperTest {
    private static final int TEST_SAMPLE_RATE = 100;
    private static final long CYCLES_PER_SAMPLE = ZX_48K_CPU_FREQUENCY / TEST_SAMPLE_RATE;

    @Test
    public void constructorAndAccessorsValidateInputs() {
        assertThrows(IllegalArgumentException.class, () -> new Beeper(AudioSink.NULL, 0));
        assertThrows(IllegalArgumentException.class, () -> new Beeper(AudioSink.NULL, -1));
        assertThrows(NullPointerException.class, () -> new Beeper(null, TEST_SAMPLE_RATE));
        Beeper beeper = new Beeper(AudioSink.NULL, 44_100);
        assertEquals(44_100, beeper.getSampleRate());
        assertEquals(100, beeper.getVolumePercent());
        beeper.setVolumePercent(-50);
        assertEquals(0, beeper.getVolumePercent());
        beeper.setVolumePercent(200);
        assertEquals(100, beeper.getVolumePercent());
        Beeper silent = Beeper.silent();
        silent.setLevel(true, false, false);
        silent.passedCycles(CYCLES_PER_SAMPLE);
        silent.close();
    }

    @Test
    public void startupAndLevelsProduceExpectedSamples() {
        assertTrue(containsNonZero(capture(b -> {
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
        })));
        assertFalse(containsNonZero(capture(b -> b.passedCycles(CYCLES_PER_SAMPLE))));
        assertFalse(containsNonZero(capture(b -> {
            b.setLevel(false, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
        })));
        short[] transition = capture(b -> {
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.setLevel(false, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
        });
        assertEquals(6, transition.length);
        assertEquals(0, transition[0]);
        assertEquals(0, transition[1]);
        assertTrue(transition[2] > 0);
        assertTrue(transition[4] < 0);
        assertStereoFrames(transition);
    }

    @Test
    public void issue3LevelsTapeInputAndVolumeScalingBehaveMonotonically() {
        short bothOff = sample(false, false, false), micOnly = sample(false, true, false), earOnly = sample(true, false, false), bothOn = sample(true, true, false);
        assertTrue(bothOff < micOnly && micOnly < earOnly && earOnly < bothOn);
        assertTrue(bothOff < 0);
        assertTrue(earOnly > 0);
        assertNotEquals(bothOff, micOnly);
        assertNotEquals(micOnly, earOnly);
        assertNotEquals(earOnly, bothOn);
        assertTrue(sample(true, false, true) > earOnly);
        assertTrue(containsNonZero(capture(b -> {
            b.setLevel(false, false, true);
            b.passedCycles(CYCLES_PER_SAMPLE);
        })));
        short full = firstNonZero(atVolume(100)), half = firstNonZero(atVolume(50));
        assertTrue(full != 0 && half != 0);
        assertTrue(Math.abs((full / 2) - half) <= 1);
        assertFalse(containsNonZero(atVolume(0)));
        short[] changed = capture(b -> {
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.setVolumePercent(0);
            b.passedCycles(CYCLES_PER_SAMPLE);
        });
        assertEquals(4, changed.length);
        assertTrue(Math.abs(changed[2]) < Math.abs(changed[0]) / 10);
        assertTrue(Math.abs(changed[3]) < Math.abs(changed[1]) / 10);
    }

    @Test
    public void passedCyclesHandlesZeroNegativeFractionalAndBatchLengths() {
        assertEquals(0, capture(b -> {
            b.setLevel(true, false, false);
            b.passedCycles(0);
        }).length);
        assertEquals(0, capture(b -> {
            b.setLevel(true, false, false);
            b.passedCycles(-100);
        }).length);
        assertEquals(2, capture(b -> {
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
        }).length);
        assertEquals(10, capture(b -> {
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE * 5);
        }).length);
        long fraction = CYCLES_PER_SAMPLE / 3;
        short[] accumulated = capture(b -> {
            b.setLevel(true, false, false);
            for (int i = 0; i < 4; i++) b.passedCycles(fraction);
        });
        assertTrue(accumulated.length >= 2);
        assertTrue(containsNonZero(accumulated));
    }

    @Test
    public void resetAndFlushSemanticsPreserveExpectedAudioState() {
        assertFalse(containsNonZero(captureSink(sink -> {
            Beeper b = new Beeper(sink, TEST_SAMPLE_RATE);
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.reset();
            b.passedCycles(CYCLES_PER_SAMPLE * 5);
            b.close();
        })));
        assertFalse(containsNonZero(captureSink(sink -> {
            Beeper b = new Beeper(sink, TEST_SAMPLE_RATE);
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.reset();
            b.setLevel(false, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.close();
        })));
        assertTrue(containsNonZero(captureSink(sink -> {
            Beeper b = new Beeper(sink, TEST_SAMPLE_RATE);
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.reset();
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
            b.close();
        })));
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * 3);
        assertEquals(0, sink.toShortArray().length);
        beeper.close();
        assertEquals(6, sink.toShortArray().length);
        sink = new RecordingAudioSink();
        beeper = new Beeper(sink, TEST_SAMPLE_RATE);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * AUDIO_DEFAULT_BATCH_FRAMES);
        assertEquals(AUDIO_DEFAULT_BATCH_FRAMES * 2, sink.toShortArray().length);
        beeper.close();
    }

    @Test
    public void recordingSinkLifecycleAndStereoOutputStayConsistent() {
        RecordingAudioSink primary = new RecordingAudioSink(), recording = new RecordingAudioSink();
        Beeper beeper = new Beeper(primary, TEST_SAMPLE_RATE);
        beeper.setRecordingSink(recording);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * 3);
        beeper.flushRecordingBuffer();
        assertArrayEquals(primary.toShortArray(), recording.toShortArray());
        int recorded = recording.toShortArray().length;
        beeper.setRecordingSink(AudioSink.NULL);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();
        assertEquals(recorded, recording.toShortArray().length);
        primary = new RecordingAudioSink();
        recording = new RecordingAudioSink();
        beeper = new Beeper(primary, TEST_SAMPLE_RATE);
        beeper.setRecordingSink(recording);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * AUDIO_DEFAULT_BATCH_FRAMES);
        assertTrue(primary.toShortArray().length > 0);
        assertTrue(recording.toShortArray().length > 0);
        beeper.reset();
        assertEquals(0, primary.toShortArray().length);
        assertEquals(0, recording.toShortArray().length);
        beeper.flushRecordingBuffer();
        beeper.close();
        assertThrows(NullPointerException.class, () -> new Beeper(AudioSink.NULL, TEST_SAMPLE_RATE).setRecordingSink(null));
        assertStereoFrames(capture(BeeperTest::playPattern));
        short[] melody = capture(Beeper.DEFAULT_SAMPLE_RATE, BeeperTest::playMelody);
        assertTrue(containsPositive(melody));
        assertTrue(containsNegative(melody));
        assertStereoFrames(melody);
    }

    private static void playPattern(Beeper b) {
        b.passedCycles(CYCLES_PER_SAMPLE);
        b.setLevel(true, false, false);
        b.passedCycles(CYCLES_PER_SAMPLE);
        b.setLevel(false, false, false);
        b.passedCycles(CYCLES_PER_SAMPLE);
        b.setLevel(true, false, false);
        b.passedCycles(CYCLES_PER_SAMPLE);
    }

    private static void playMelody(Beeper b) {
        int[] hz = {262, 294, 330, 392}, ms = {120, 120, 120, 240};
        boolean level = false;
        for (int i = 0; i < hz.length; i++) {
            level = playNote(b, level, hz[i], ms[i]);
            if (i + 1 < hz.length) {
                level = false;
                b.setLevel(false, false, false);
                b.passedCycles(toCycles(20));
            }
        }
    }

    private static boolean playNote(Beeper b, boolean level, int hz, int ms) {
        long noteCycles = toCycles(ms), halfPeriod = Math.max(1L, Math.round(ZX_48K_CPU_FREQUENCY / (hz * 2.0)));
        for (long played = 0; played < noteCycles; ) {
            level = !level;
            b.setLevel(level, false, false);
            long chunk = Math.min(halfPeriod, noteCycles - played);
            b.passedCycles(chunk);
            played += chunk;
        }
        return level;
    }

    private short sample(boolean ear, boolean mic, boolean tape) {
        return capture(b -> {
            b.setLevel(true, false, false);
            b.setLevel(ear, mic, tape);
            b.passedCycles(CYCLES_PER_SAMPLE);
        })[0];
    }

    private short[] atVolume(int volume) {
        return capture(b -> {
            b.setVolumePercent(volume);
            b.setLevel(true, false, false);
            b.passedCycles(CYCLES_PER_SAMPLE);
        });
    }

    private static short[] capture(Consumer<Beeper> script) {
        return capture(TEST_SAMPLE_RATE, script);
    }

    private static short[] capture(int rate, Consumer<Beeper> script) {
        RecordingAudioSink sink = new RecordingAudioSink();
        try (Beeper beeper = new Beeper(sink, rate)) {
            script.accept(beeper);
        }
        return sink.toShortArray();
    }

    private static short[] captureSink(Consumer<RecordingAudioSink> script) {
        RecordingAudioSink sink = new RecordingAudioSink();
        script.accept(sink);
        return sink.toShortArray();
    }

    private static boolean containsNonZero(short[] samples) {
        for (short sample : samples) if (sample != 0) return true;
        return false;
    }

    private static boolean containsPositive(short[] samples) {
        for (short sample : samples) if (sample > 0) return true;
        return false;
    }

    private static boolean containsNegative(short[] samples) {
        for (short sample : samples) if (sample < 0) return true;
        return false;
    }

    private static short firstNonZero(short[] samples) {
        for (short sample : samples) if (sample != 0) return sample;
        return 0;
    }

    private static void assertStereoFrames(short[] samples) {
        assertEquals(0, samples.length % 2);
        for (int i = 0; i < samples.length; i += 2) assertEquals(samples[i], samples[i + 1]);
    }

    private static long toCycles(int ms) {
        return Math.round(ZX_48K_CPU_FREQUENCY * (ms / 1000.0));
    }
}
