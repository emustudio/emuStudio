/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeperTest {

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
        assertTrue(samples[2] != 0);
        assertEquals(samples[2], samples[3]);
        assertEquals(0, samples[4]);
        assertEquals(0, samples[5]);
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
}
