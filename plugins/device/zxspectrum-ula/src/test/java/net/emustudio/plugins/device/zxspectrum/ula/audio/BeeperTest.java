/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeperTest {
    private static final int[] MELODY_HZ = {262, 294, 330, 392};
    private static final int[] NOTE_MILLIS = {120, 120, 120, 240};
    private static final int GAP_MILLIS = 20;

    @Test
    public void testHighLevelProducesStereoSamples() {
        RecordingSink sink = new RecordingSink();
        Beeper beeper = new Beeper(sink, 1000, 120, 16);

        beeper.setLevel(true);
        beeper.passedCycles(120);
        beeper.close();

        short[] samples = sink.toShortArray();
        assertTrue(containsNonZeroSample(samples));
        assertStereoFrames(samples);
    }

    @Test
    public void testLevelChangesAreResampledIntoSamples() {
        RecordingSink sink = new RecordingSink();
        Beeper beeper = new Beeper(sink, 1000, 100, 16);

        beeper.passedCycles(10);
        beeper.setLevel(true);
        beeper.passedCycles(10);
        beeper.setLevel(false);
        beeper.passedCycles(10);
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
        RecordingSink sink = new RecordingSink();
        Beeper beeper = new Beeper(sink, 1000, 120, 16);

        beeper.passedCycles(120);
        beeper.close();

        assertFalse(containsNonZeroSample(sink.toShortArray()));
    }

    @Test
    public void testMelodyRecordedSinkContainsPositiveAndNegativeSamples() {
        RecordingSink sink = new RecordingSink();
        Beeper beeper = new Beeper(sink, Beeper.ZX_SPECTRUM_FREQUENCY, Beeper.DEFAULT_SAMPLE_RATE, 512);
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
        RecordingSink sink = new RecordingSink();
        Beeper beeper = new Beeper(sink, 1000, 120, 16);

        beeper.passedCycles(10);
        beeper.setLevel(true);
        beeper.passedCycles(10);
        beeper.setLevel(false);
        beeper.passedCycles(60);
        beeper.reset();
        beeper.passedCycles(120);
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

    private static boolean playNote(Beeper beeper, boolean level, int frequencyHz, int durationMillis) {
        long noteCycles = toCycles(durationMillis);
        long halfPeriodCycles = Math.max(1L, Math.round(Beeper.ZX_SPECTRUM_FREQUENCY / (frequencyHz * 2.0)));
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
        return Math.round(Beeper.ZX_SPECTRUM_FREQUENCY * (durationMillis / 1000.0));
    }
}
