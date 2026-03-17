/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import org.junit.Test;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.ZX_48K_CPU_FREQUENCY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeperTest {
    private static final int[] MELODY_HZ = {262, 294, 330, 392};
    private static final int[] NOTE_MILLIS = {120, 120, 120, 240};
    private static final int GAP_MILLIS = 20;

    // With sampleRate=100, exactly 35_000 CPU cycles produce one PCM frame.
    private static final int TEST_SAMPLE_RATE = 100;
    private static final long CYCLES_PER_SAMPLE = ZX_48K_CPU_FREQUENCY / TEST_SAMPLE_RATE; // 35_000

    @Test
    public void testHighLevelProducesStereoSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        short[] samples = sink.toShortArray();
        assertTrue(containsNonZeroSample(samples));
        assertStereoFrames(samples);
    }

    @Test
    public void testLevelChangesAreResampledIntoSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(true);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        short[] samples = sink.toShortArray();
        assertEquals(6, samples.length);
        assertEquals(0, samples[0]);
        assertEquals(0, samples[1]);
        assertTrue(samples[2] > 0);
        assertEquals(samples[2], samples[3]);
        assertTrue(samples[4] < 0);
        assertEquals(samples[4], samples[5]);
    }

    @Test
    public void testLowLevelProducesSilence() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertFalse(containsNonZeroSample(sink.toShortArray()));
    }

    @Test
    public void testMelodyRecordedSinkContainsPositiveAndNegativeSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, Beeper.DEFAULT_SAMPLE_RATE);
        boolean level = false;

        for (int i = 0; i < MELODY_HZ.length; i++) {
            level = playNote(beeper, level, MELODY_HZ[i], NOTE_MILLIS[i]);
            if (i + 1 < MELODY_HZ.length) {
                level = false;
                beeper.setLevel(false);
                beeper.passedCycles(toCycles(GAP_MILLIS));
            }
        }
        beeper.close();

        short[] samples = sink.toShortArray();
        assertTrue(containsPositiveSample(samples));
        assertTrue(containsNegativeSample(samples));
        assertStereoFrames(samples);
    }

    @Test
    public void testResetSilencesPendingTone() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(true);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * 6);
        beeper.reset();
        beeper.passedCycles(CYCLES_PER_SAMPLE * 12);
        beeper.close();

        assertFalse(containsNonZeroSample(sink.toShortArray()));
    }

    @Test
    public void testVolumePercentScalesAmplitude() {
        RecordingAudioSink fullSink = new RecordingAudioSink();
        Beeper fullVolume = new Beeper(fullSink, TEST_SAMPLE_RATE);

        fullVolume.setLevel(true);
        fullVolume.passedCycles(CYCLES_PER_SAMPLE);
        fullVolume.close();

        RecordingAudioSink halfSink = new RecordingAudioSink();
        Beeper halfVolume = new Beeper(halfSink, TEST_SAMPLE_RATE);
        halfVolume.setVolumePercent(50);
        halfVolume.setLevel(true);
        halfVolume.passedCycles(CYCLES_PER_SAMPLE);
        halfVolume.close();

        short fullAmplitude = firstNonZeroSample(fullSink.toShortArray());
        short halfAmplitude = firstNonZeroSample(halfSink.toShortArray());

        assertTrue(Math.abs((fullAmplitude / 2) - halfAmplitude) <= 1);
    }

    @Test
    public void testZeroVolumeMutesTone() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setVolumePercent(0);
        beeper.setLevel(true);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertFalse(containsNonZeroSample(sink.toShortArray()));
    }

    private static boolean containsNonZeroSample(short[] samples) {
        for (short sample : samples) {
            if (sample != 0) {
                return true;
            }
        }
        return false;
    }

    private static void assertStereoFrames(short[] samples) {
        assertEquals(0, samples.length % 2);
        for (int i = 0; i < samples.length; i += 2) {
            assertEquals(samples[i], samples[i + 1]);
        }
    }

    private static boolean containsPositiveSample(short[] samples) {
        for (short sample : samples) {
            if (sample > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNegativeSample(short[] samples) {
        for (short sample : samples) {
            if (sample < 0) {
                return true;
            }
        }
        return false;
    }

    private static short firstNonZeroSample(short[] samples) {
        for (short sample : samples) {
            if (sample != 0) {
                return sample;
            }
        }
        return 0;
    }

    private static boolean playNote(Beeper beeper, boolean level, int frequencyHz, int durationMillis) {
        long noteCycles = toCycles(durationMillis);
        long halfPeriodCycles = Math.max(1L, Math.round(ZX_48K_CPU_FREQUENCY / (frequencyHz * 2.0)));
        long playedCycles = 0;

        while (playedCycles < noteCycles) {
            level = !level;
            beeper.setLevel(level);

            long chunkCycles = Math.min(halfPeriodCycles, noteCycles - playedCycles);
            beeper.passedCycles(chunkCycles);
            playedCycles += chunkCycles;
        }
        return level;
    }

    private static long toCycles(int durationMillis) {
        return Math.round(ZX_48K_CPU_FREQUENCY * (durationMillis / 1000.0));
    }
}
