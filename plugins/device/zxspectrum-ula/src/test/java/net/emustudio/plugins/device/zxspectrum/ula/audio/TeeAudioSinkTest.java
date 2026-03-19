/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import org.junit.Test;

import static org.junit.Assert.*;

public class TeeAudioSinkTest {

    // --- Constructor ---

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullPrimary() {
        new TeeAudioSink(null);
    }

    // --- setSecondarySink ---

    @Test(expected = NullPointerException.class)
    public void testSetSecondarySinkRejectsNull() {
        TeeAudioSink tee = new TeeAudioSink(AudioSink.NULL);
        tee.setSecondarySink(null);
    }

    // --- accept ---

    @Test
    public void testAcceptForwardsToPrimarySink() {
        RecordingAudioSink primary = new RecordingAudioSink();
        TeeAudioSink tee = new TeeAudioSink(primary);

        byte[] data = {1, 2, 3, 4};
        tee.accept(data, data.length);

        short[] samples = primary.toShortArray();
        assertTrue("Primary sink should receive data", samples.length > 0);
    }

    @Test
    public void testAcceptForwardsToSecondarySink() {
        RecordingAudioSink primary = new RecordingAudioSink();
        RecordingAudioSink secondary = new RecordingAudioSink();
        TeeAudioSink tee = new TeeAudioSink(primary);
        tee.setSecondarySink(secondary);

        byte[] data = {1, 2, 3, 4};
        tee.accept(data, data.length);

        short[] primarySamples = primary.toShortArray();
        short[] secondarySamples = secondary.toShortArray();
        assertArrayEquals("Both sinks should receive the same data", primarySamples, secondarySamples);
    }

    @Test
    public void testAcceptWithoutSecondarySinkDoesNotThrow() {
        RecordingAudioSink primary = new RecordingAudioSink();
        TeeAudioSink tee = new TeeAudioSink(primary);

        byte[] data = {1, 2, 3, 4};
        tee.accept(data, data.length);

        // Default secondary is AudioSink.NULL — should not throw
        assertTrue(primary.toShortArray().length > 0);
    }

    @Test
    public void testReplacingSecondarySinkOnlyAffectsSubsequentCalls() {
        RecordingAudioSink primary = new RecordingAudioSink();
        RecordingAudioSink first = new RecordingAudioSink();
        RecordingAudioSink second = new RecordingAudioSink();
        TeeAudioSink tee = new TeeAudioSink(primary);

        tee.setSecondarySink(first);
        byte[] data1 = {1, 2, 3, 4};
        tee.accept(data1, data1.length);

        tee.setSecondarySink(second);
        byte[] data2 = {5, 6, 7, 8};
        tee.accept(data2, data2.length);

        // 'first' received only the first batch (4 bytes = 2 shorts)
        assertEquals(2, first.toShortArray().length);
        // 'second' received only the second batch (4 bytes = 2 shorts)
        assertEquals(2, second.toShortArray().length);
    }

    // --- flushAudio ---

    @Test
    public void testFlushAudioTargetsOnlyPrimarySink() {
        RecordingAudioSink primary = new RecordingAudioSink();
        RecordingAudioSink secondary = new RecordingAudioSink();
        TeeAudioSink tee = new TeeAudioSink(primary);
        tee.setSecondarySink(secondary);

        byte[] data = {1, 2, 3, 4};
        tee.accept(data, data.length);
        int secondaryLengthBefore = secondary.toShortArray().length;

        tee.flushAudio();

        // Primary flushed (RecordingAudioSink.flushAudio resets size to 0)
        assertEquals(0, primary.toShortArray().length);
        // Secondary untouched
        assertEquals(secondaryLengthBefore, secondary.toShortArray().length);
    }

    // --- close ---

    @Test
    public void testCloseTargetsOnlyPrimarySink() {
        CloseTrackingSink primary = new CloseTrackingSink();
        CloseTrackingSink secondary = new CloseTrackingSink();
        TeeAudioSink tee = new TeeAudioSink(primary);
        tee.setSecondarySink(secondary);

        tee.close();

        assertTrue("Primary should be closed", primary.closed);
        assertFalse("Secondary should not be closed", secondary.closed);
    }

    // --- Helper ---

    private static final class CloseTrackingSink implements AudioSink {
        boolean closed;

        @Override
        public void accept(byte[] pcmSamples, int length) {
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}

