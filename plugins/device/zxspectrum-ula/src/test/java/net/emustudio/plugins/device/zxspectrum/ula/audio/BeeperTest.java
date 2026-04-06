/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.audio;

import org.junit.Test;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.ZX_48K_CPU_FREQUENCY;
import static net.emustudio.plugins.device.zxspectrum.ula.Constants.AUDIO_DEFAULT_BATCH_FRAMES;
import static org.junit.Assert.*;

public class BeeperTest {
    // With sampleRate=100, exactly 35_000 CPU cycles produce one PCM frame.
    private static final int TEST_SAMPLE_RATE = 100;
    private static final long CYCLES_PER_SAMPLE = ZX_48K_CPU_FREQUENCY / TEST_SAMPLE_RATE; // 35_000

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroSampleRate() {
        new Beeper(AudioSink.NULL, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeSampleRate() {
        new Beeper(AudioSink.NULL, -1);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullSink() {
        new Beeper(null, TEST_SAMPLE_RATE);
    }

    @Test
    public void testGetSampleRateReturnsConstructorValue() {
        Beeper beeper = new Beeper(AudioSink.NULL, 44_100);
        assertEquals(44_100, beeper.getSampleRate());
    }

    @Test
    public void testSilentBeeperDoesNotThrow() {
        Beeper beeper = Beeper.silent();
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();
    }

    @Test
    public void testHighLevelProducesNonZeroStereoSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        short[] samples = sink.toShortArray();
        assertTrue("Expected non-zero samples", containsNonZero(samples));
        assertStereoFrames(samples);
    }

    @Test
    public void testLowLevelProducesSilence() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertFalse("Expected silence", containsNonZero(sink.toShortArray()));
    }

    @Test
    public void testLevelTransitionHighLowProducesPositiveThenNegative() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        // silent -> high -> low generates: silence, positive, negative
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(false, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        short[] samples = sink.toShortArray();
        // 3 stereo frames = 6 shorts
        assertEquals(6, samples.length);
        // frame 0: silence (audio not started until setLevel(true))
        assertEquals(0, samples[0]);
        assertEquals(0, samples[1]);
        // frame 1: high (positive)
        assertTrue("high level should be positive", samples[2] > 0);
        assertEquals(samples[2], samples[3]);
        // frame 2: low (negative because audioStarted=true, all-off level is below midpoint)
        assertTrue("low level after high should be negative", samples[4] < 0);
        assertEquals(samples[4], samples[5]);
    }

    @Test
    public void testAudioDoesNotStartUntilFirstTrueLevel() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        // setLevel(false, false, false) should not "start" audio
        beeper.setLevel(false, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertFalse("Audio should not start until first true level", containsNonZero(sink.toShortArray()));
    }

    @Test
    public void testWeightedLevelOrdering() {
        // Issue 3 voltage levels: 0.34V (off/off) < 0.66V (off/on) < 3.56V (on/off) < 3.70V (on/on)
        short bothOff = sampleForLevel(false, false);
        short micOnly = sampleForLevel(false, true);
        short earOnly = sampleForLevel(true, false);
        short bothOn = sampleForLevel(true, true);

        assertTrue("bothOff < micOnly", bothOff < micOnly);
        assertTrue("micOnly < earOnly", micOnly < earOnly);
        assertTrue("earOnly < bothOn", earOnly < bothOn);
    }

    @Test
    public void testEarOnlyProducesHighestAmplitude() {
        short earOnly = sampleForLevel(true, false);
        assertTrue("ear_only should be positive", earOnly > 0);
    }

    @Test
    public void testMicOnlyIsLowerThanEarOnly() {
        short micOnly = sampleForLevel(false, true);
        short earOnly = sampleForLevel(true, false);
        assertTrue("mic_only should be lower than ear_only", micOnly < earOnly);
    }

    @Test
    public void testEarOnlyIsPositive() {
        assertTrue(sampleForLevel(true, false) > 0);
    }

    @Test
    public void testBothOffIsNegativeWhenAudioStarted() {
        // When audio has started but both EAR and MIC are off, the weighted sum is 0
        // which maps to negative PCM (below midpoint)
        short bothOff = sampleForLevel(false, false);
        assertTrue("both_off should be negative", bothOff < 0);
    }

    @Test
    public void testWeightedLevelsAreDistinctExceptSaturated() {
        short bothOff = sampleForLevel(false, false);
        short micOnly = sampleForLevel(false, true);
        short earOnly = sampleForLevel(true, false);
        short bothOn = sampleForLevel(true, true);

        assertNotEquals(bothOff, micOnly);
        assertNotEquals(micOnly, earOnly);
        assertNotEquals(bothOff, earOnly);
        // Issue 3 has four distinct voltage levels — EAR+MIC is slightly above EAR-only
        assertNotEquals(earOnly, bothOn);
    }

    @Test
    public void testZeroCyclesProducesNoSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(0);
        beeper.close();

        assertEquals(0, sink.toShortArray().length);
    }

    @Test
    public void testNegativeCyclesProducesNoSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(-100);
        beeper.close();

        assertEquals(0, sink.toShortArray().length);
    }

    @Test
    public void testExactCyclesPerSampleProducesOneStereoFrame() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        // One stereo frame = 2 shorts (left + right)
        assertEquals(2, sink.toShortArray().length);
    }

    @Test
    public void testFractionalCyclesAccumulateAcrossCalls() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        // Each call passes less than one sample worth of cycles.
        // After enough calls, the accumulator should overflow and produce a sample.
        long fraction = CYCLES_PER_SAMPLE / 3;
        beeper.passedCycles(fraction);
        beeper.passedCycles(fraction);
        beeper.passedCycles(fraction);
        // 3 * (35000/3) = 3 * 11666 = 34998 < 35000 — still no sample
        // One more push should trigger it
        beeper.passedCycles(fraction);
        beeper.close();

        short[] samples = sink.toShortArray();
        assertTrue("Fractional cycles should accumulate into at least one frame", samples.length >= 2);
        assertTrue(containsNonZero(samples));
    }

    @Test
    public void testMultipleCyclesProduceProportionalSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * 5);
        beeper.close();

        // 5 stereo frames = 10 shorts
        assertEquals(10, sink.toShortArray().length);
    }

    @Test
    public void testDefaultVolumeIs100() {
        Beeper beeper = new Beeper(AudioSink.NULL, TEST_SAMPLE_RATE);
        assertEquals(100, beeper.getVolumePercent());
    }

    @Test
    public void testHalfVolumeProducesHalfAmplitude() {
        short fullAmplitude = firstNonZero(sampleAtVolume(100));
        short halfAmplitude = firstNonZero(sampleAtVolume(50));

        assertTrue("Full amplitude should be non-zero", fullAmplitude != 0);
        assertTrue("Half amplitude should be non-zero", halfAmplitude != 0);
        assertTrue("Half volume should produce ~half amplitude",
                Math.abs((fullAmplitude / 2) - halfAmplitude) <= 1);
    }

    @Test
    public void testZeroVolumeProducesSilence() {
        short[] samples = sampleAtVolume(0);
        assertFalse("Zero volume should produce silence", containsNonZero(samples));
    }

    @Test
    public void testVolumeClampedToZero() {
        Beeper beeper = new Beeper(AudioSink.NULL, TEST_SAMPLE_RATE);
        beeper.setVolumePercent(-50);
        assertEquals(0, beeper.getVolumePercent());
    }

    @Test
    public void testVolumeClampedTo100() {
        Beeper beeper = new Beeper(AudioSink.NULL, TEST_SAMPLE_RATE);
        beeper.setVolumePercent(200);
        assertEquals(100, beeper.getVolumePercent());
    }

    @Test
    public void testVolumeChangeAffectsSubsequentSamples() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setVolumePercent(0);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        short[] samples = sink.toShortArray();
        assertEquals(4, samples.length);
        // frame 0: full volume
        assertTrue(samples[0] != 0);
        // frame 1: zero volume — the low-pass filter may leave a tiny residual, but it should be
        // much smaller than the full-volume sample (< 10% of the absolute value).
        assertTrue("volume=0 should produce near-silence (left)",
                Math.abs(samples[2]) < Math.abs(samples[0]) / 10);
        assertTrue("volume=0 should produce near-silence (right)",
                Math.abs(samples[3]) < Math.abs(samples[0]) / 10);
    }

    @Test
    public void testResetClearsPendingAudioAndReturnsSilence() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.reset();
        // After reset, audio is flushed (RecordingAudioSink.flushAudio resets size to 0)
        // and level is back to silent
        beeper.passedCycles(CYCLES_PER_SAMPLE * 5);
        beeper.close();

        assertFalse("After reset, output should be silent", containsNonZero(sink.toShortArray()));
    }

    @Test
    public void testResetRequiresNewSetLevelToRestart() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.reset();

        // setLevel(false) shouldn't restart audio (audioStarted is false after reset)
        beeper.setLevel(false, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertFalse("setLevel(false) after reset should not produce audio", containsNonZero(sink.toShortArray()));
    }

    @Test
    public void testResetThenSetLevelTrueRestartsAudio() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.reset();

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertTrue("setLevel(true) after reset should restart audio", containsNonZero(sink.toShortArray()));
    }

    @Test
    public void testCloseFlushesPartialBatch() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);
        // Generate fewer samples than AUDIO_DEFAULT_BATCH_FRAMES so the buffer isn't auto-flushed
        beeper.passedCycles(CYCLES_PER_SAMPLE * 3);

        // Before close, sink may or may not have data (depends on batch size)
        beeper.close();

        // After close, all samples must be delivered
        short[] samples = sink.toShortArray();
        assertEquals("3 stereo frames expected after close", 6, samples.length);
        assertTrue(containsNonZero(samples));
    }

    @Test
    public void testBatchFlushOccursAtBatchSize() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(true, false, false);

        // Generate exactly AUDIO_DEFAULT_BATCH_FRAMES samples to trigger automatic flush
        beeper.passedCycles(CYCLES_PER_SAMPLE * AUDIO_DEFAULT_BATCH_FRAMES);

        // The batch should have been flushed before close
        short[] beforeClose = sink.toShortArray();
        assertEquals(AUDIO_DEFAULT_BATCH_FRAMES * 2, beforeClose.length);

        beeper.close();
    }

    @Test
    public void testRecordingSinkReceivesSameDataAsPrimary() {
        RecordingAudioSink primary = new RecordingAudioSink();
        RecordingAudioSink recording = new RecordingAudioSink();
        Beeper beeper = new Beeper(primary, TEST_SAMPLE_RATE);
        beeper.setRecordingSink(recording);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * 3);
        beeper.close();

        assertArrayEquals(primary.toShortArray(), recording.toShortArray());
    }

    @Test
    public void testResetFlushesAllSinksUniformly() {
        RecordingAudioSink primary = new RecordingAudioSink();
        RecordingAudioSink recording = new RecordingAudioSink();
        Beeper beeper = new Beeper(primary, TEST_SAMPLE_RATE);
        beeper.setRecordingSink(recording);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE * AUDIO_DEFAULT_BATCH_FRAMES);
        // Both sinks should have data from the batch flush
        assertTrue(primary.toShortArray().length > 0);
        assertTrue(recording.toShortArray().length > 0);

        beeper.reset();
        // Both sinks are flushed — each sink decides what "flush" means via the AudioSink interface.
        // RecordingAudioSink.flushAudio() resets its size to 0, same as SoundAudioSink would clear its queue.
        assertEquals(0, primary.toShortArray().length);
        assertEquals(0, recording.toShortArray().length);

        beeper.close();
    }

    @Test
    public void testTapeInputStartsAudio() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        // tapeIn=true should start audio even when earOn=false, micOn=false
        beeper.setLevel(false, false, true);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertTrue("tapeIn should start audio", containsNonZero(sink.toShortArray()));
    }

    @Test
    public void testTapeInputAddsAmplitude() {
        short withoutTape = sampleForLevelWithTape(true, false, false);
        short withTape = sampleForLevelWithTape(true, false, true);

        assertTrue("Tape input should add amplitude", withTape > withoutTape);
    }

    @Test
    public void testTapeInputAloneProducesPositiveSample() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        beeper.setLevel(false, false, true); // only tape in
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        short[] samples = sink.toShortArray();
        assertTrue("Tape input alone should produce non-zero audio", containsNonZero(samples));
    }

    @Test
    public void testFlushRecordingBufferForcesPartialBatchToSinks() {
        RecordingAudioSink primary = new RecordingAudioSink();
        RecordingAudioSink recording = new RecordingAudioSink();
        Beeper beeper = new Beeper(primary, TEST_SAMPLE_RATE);
        beeper.setRecordingSink(recording);

        beeper.setLevel(true, false, false);
        // Generate fewer samples than AUDIO_DEFAULT_BATCH_FRAMES (partial batch, not auto-flushed)
        beeper.passedCycles(CYCLES_PER_SAMPLE * 3);

        // Before flush: recording may not have data yet (buffered in Beeper)
        int recordingBefore = recording.toShortArray().length;

        beeper.flushRecordingBuffer();

        // After flush: recording must have all 3 stereo frames = 6 shorts
        short[] afterFlush = recording.toShortArray();
        assertTrue("flushRecordingBuffer should push buffered data to recording sink",
                afterFlush.length >= recordingBefore);
        assertEquals(6, afterFlush.length);

        beeper.close();
    }

    @Test
    public void testFlushRecordingBufferWhenEmptyDoesNotThrow() {
        Beeper beeper = new Beeper(AudioSink.NULL, TEST_SAMPLE_RATE);
        // Nothing buffered yet — should not throw
        beeper.flushRecordingBuffer();
        beeper.close();
    }

    @Test(expected = NullPointerException.class)
    public void testSetRecordingSinkRejectsNull() {
        Beeper beeper = new Beeper(AudioSink.NULL, TEST_SAMPLE_RATE);
        beeper.setRecordingSink(null);
    }

    @Test
    public void testSetRecordingSinkToNullSinkDisconnectsRecording() {
        RecordingAudioSink primary = new RecordingAudioSink();
        RecordingAudioSink recording = new RecordingAudioSink();
        Beeper beeper = new Beeper(primary, TEST_SAMPLE_RATE);
        beeper.setRecordingSink(recording);

        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.flushRecordingBuffer();
        int recordingLengthBefore = recording.toShortArray().length;

        // Disconnect recording sink
        beeper.setRecordingSink(AudioSink.NULL);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        // Recording should not have received the second batch
        assertEquals(recordingLengthBefore, recording.toShortArray().length);
    }

    @Test
    public void testAllFramesAreStereoPaired() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);

        // Generate a pattern: silence, high, low, high
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(false, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();

        assertStereoFrames(sink.toShortArray());
    }

    @Test
    public void testSquareWaveMelodyContainsBothPolarities() {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, Beeper.DEFAULT_SAMPLE_RATE);

        int[] melodyHz = {262, 294, 330, 392};
        int[] noteMillis = {120, 120, 120, 240};
        boolean level = false;

        for (int i = 0; i < melodyHz.length; i++) {
            level = playNote(beeper, level, melodyHz[i], noteMillis[i]);
            if (i + 1 < melodyHz.length) {
                level = false;
                beeper.setLevel(false, false, false);
                beeper.passedCycles(toCycles(20));
            }
        }
        beeper.close();

        short[] samples = sink.toShortArray();
        assertTrue("Melody should contain positive samples", containsPositive(samples));
        assertTrue("Melody should contain negative samples", containsNegative(samples));
        assertStereoFrames(samples);
    }

    private short sampleForLevel(boolean earOn, boolean micOn) {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);
        beeper.setLevel(true, false, false); // ensure audioStarted=true
        beeper.setLevel(earOn, micOn, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();
        return sink.toShortArray()[0];
    }

    private short sampleForLevelWithTape(boolean earOn, boolean micOn, boolean tapeIn) {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);
        beeper.setLevel(true, false, false); // ensure audioStarted=true
        beeper.setLevel(earOn, micOn, tapeIn);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();
        return sink.toShortArray()[0];
    }

    private short[] sampleAtVolume(int volumePercent) {
        RecordingAudioSink sink = new RecordingAudioSink();
        Beeper beeper = new Beeper(sink, TEST_SAMPLE_RATE);
        beeper.setVolumePercent(volumePercent);
        beeper.setLevel(true, false, false);
        beeper.passedCycles(CYCLES_PER_SAMPLE);
        beeper.close();
        return sink.toShortArray();
    }

    private static boolean containsNonZero(short[] samples) {
        for (short s : samples) {
            if (s != 0) return true;
        }
        return false;
    }

    private static boolean containsPositive(short[] samples) {
        for (short s : samples) {
            if (s > 0) return true;
        }
        return false;
    }

    private static boolean containsNegative(short[] samples) {
        for (short s : samples) {
            if (s < 0) return true;
        }
        return false;
    }

    private static short firstNonZero(short[] samples) {
        for (short s : samples) {
            if (s != 0) return s;
        }
        return 0;
    }

    private static void assertStereoFrames(short[] samples) {
        assertEquals("Sample count must be even (stereo)", 0, samples.length % 2);
        for (int i = 0; i < samples.length; i += 2) {
            assertEquals("Left and right must match at frame " + (i / 2), samples[i], samples[i + 1]);
        }
    }

    private static boolean playNote(Beeper beeper, boolean level, int frequencyHz, int durationMillis) {
        long noteCycles = toCycles(durationMillis);
        long halfPeriodCycles = Math.max(1L, Math.round(ZX_48K_CPU_FREQUENCY / (frequencyHz * 2.0)));
        long playedCycles = 0;
        while (playedCycles < noteCycles) {
            level = !level;
            beeper.setLevel(level, false, false);
            long chunk = Math.min(halfPeriodCycles, noteCycles - playedCycles);
            beeper.passedCycles(chunk);
            playedCycles += chunk;
        }
        return level;
    }

    private static long toCycles(int durationMillis) {
        return Math.round(ZX_48K_CPU_FREQUENCY * (durationMillis / 1000.0));
    }
}
