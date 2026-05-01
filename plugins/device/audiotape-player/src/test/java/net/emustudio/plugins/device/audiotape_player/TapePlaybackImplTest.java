/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.emulib.plugins.device.DeviceContext;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class TapePlaybackImplTest {
    private static final int TEST_CPU_FREQUENCY_KHZ = 3500;

    private TapePlaybackImpl playback;
    private DeviceContext<Byte> lineIn;
    private List<Byte> writtenData;

    @Before
    public void setUp() {
        writtenData = new ArrayList<>();
        lineIn = mock(DeviceContext.class);
        lineIn.writeData(anyByte());
        expectLastCall().andAnswer(() -> {
            writtenData.add((Byte) getCurrentArguments()[0]);
            return null;
        }).anyTimes();
        expect(lineIn.getDataType()).andReturn(Byte.class).anyTimes();
        replay(lineIn);

        playback = new TapePlaybackImpl(lineIn, () -> TEST_CPU_FREQUENCY_KHZ);
    }

    /**
     * Starts playback in a background thread and drains all scheduled pulses.
     * Each passedCycles call drains all overdue entries, but we call in a loop
     * to advance the playback clock gradually.
     */
    private void drainPulses(int callCount, int tstatesPerCall) throws InterruptedException {
        Thread playThread = new Thread(() -> playback.onFileEnd());
        playThread.start();
        Thread.sleep(50);

        for (int i = 0; i < callCount; i++) {
            playback.passedCycles(tstatesPerCall);
        }
        playThread.join(5000);
    }

    private Thread startPlaybackAsync() throws InterruptedException {
        Thread playThread = new Thread(() -> playback.onFileEnd());
        playThread.start();
        Thread.sleep(50);
        return playThread;
    }

    private int getLastReportedProgress() {
        try {
            Field field = TapePlaybackImpl.class.getDeclaredField("lastReportedProgress");
            field.setAccessible(true);
            return field.getInt(playback);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not read lastReportedProgress", e);
        }
    }

    private long getTotalPlayableTstates() {
        try {
            Field field = TapePlaybackImpl.class.getDeclaredField("totalPlayableTstates");
            field.setAccessible(true);
            return field.getLong(playback);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not read totalPlayableTstates", e);
        }
    }

    private long getCurrentTstates() {
        try {
            Field field = TapePlaybackImpl.class.getDeclaredField("currentTstates");
            field.setAccessible(true);
            return field.getLong(playback);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not read currentTstates", e);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNullLineInThrows() {
        new TapePlaybackImpl(null, () -> TEST_CPU_FREQUENCY_KHZ);
    }

    @Test(expected = NullPointerException.class)
    public void testNullFrequencySupplierThrows() {
        new TapePlaybackImpl(lineIn, null);
    }

    @Test
    public void testPassedCyclesDoesNothingWhenNotPlaying() {
        playback.passedCycles(1000);
        assertTrue(writtenData.isEmpty());
    }

    @Test
    public void testPlaybackWritesPulsesToLineIn() throws InterruptedException {
        playback.onFileStart();
        drainPulses(100, 100_000);
        assertFalse("Expected at least one pulse written to lineIn", writtenData.isEmpty());
    }

    @Test
    public void testPauseDurationsUseConfiguredCpuFrequency() {
        playback = new TapePlaybackImpl(lineIn, () -> 4000);

        playback.onFileStart();
        assertEquals(8_000_001L, getCurrentTstates());

        playback.onPause(10);
        assertEquals(8_040_001L, getCurrentTstates());
    }


    @Test
    public void testPauseScalesLinearlyWithCpuFrequency() {
        // Same tape semantics at two different CPU frequencies must scale ms-pauses proportionally.
        TapePlaybackImpl at35 = new TapePlaybackImpl(lineIn, () -> 3500);
        TapePlaybackImpl at70 = new TapePlaybackImpl(lineIn, () -> 7000);
        at35.onFileStart();
        at70.onFileStart();
        long t35 = readCurrentTstates(at35);
        long t70 = readCurrentTstates(at70);
        // 2× the clock -> 2× the T-states for the same wall-clock pause
        assertEquals((t35 - 1) * 2, t70 - 1);
    }

    @Test
    public void testTurboPauseUsesConfiguredCpuFrequency() {
        playback = new TapePlaybackImpl(lineIn, () -> 4000);
        long before = getCurrentTstates();
        // Single-byte turbo block, no pilot/data pulses worth checking, only the trailing pause.
        // Pulse counts: 0 pilot pulses + sync1 + sync2 + 8 bits * 2 pulses = 18 schedule entries.
        playback.onTurboSpeedData(100, 200, 300, 400, 800, 0, 8, 5, new byte[]{0x00});
        long after = getCurrentTstates();
        // 0 pilot + 200 + 300 + 8*2*400 (zero bits) + 5 ms * 4000 kHz = 6_900 + 20_000 = 26_900
        assertEquals(before + 200 + 300 + 8 * 2 * 400 + 5 * 4000, after);
    }

    @Test
    public void testPureDataPauseUsesConfiguredCpuFrequency() {
        playback = new TapePlaybackImpl(lineIn, () -> 7000);
        long before = getCurrentTstates();
        playback.onPureData(400, 800, 8, 3, new byte[]{0x00}); // all zeros -> 16 pulses of 400
        long after = getCurrentTstates();
        assertEquals(before + 8 * 2 * 400 + 3 * 7000, after);
    }

    @Test
    public void testDirectRecordingPauseUsesConfiguredCpuFrequency() {
        playback = new TapePlaybackImpl(lineIn, () -> 7000);
        long before = getCurrentTstates();
        playback.onDirectRecording(2, 4, 8, new byte[]{(byte) 0x00});
        long after = getCurrentTstates();
        // 8 samples of 2 T-states + 4 ms * 7000 kHz
        assertEquals(before + 8 * 2 + 4 * 7000, after);
    }

    @Test
    public void testPassedCyclesDrainsAllOverdueEventsInSingleCall() throws InterruptedException {
        // Schedule a dense burst of edges then advance the playback clock past the last edge in
        // a single passedCycles() call. All edges must fire (regression: a previous version only
        // drained one entry per callback, breaking TZX turbo blocks).
        playback.onFileStart();
        playback.onBlockData(new byte[]{(byte) 0xFF, 0x00, (byte) 0xA5});

        Thread playThread = startPlaybackAsync();
        playback.passedCycles(Integer.MAX_VALUE);
        playThread.join(2000);

        assertFalse("Playback should finish in one drain", playThread.isAlive());
        // 1 PAUSE + 3 bytes * 16 pulses = 49 edges
        assertEquals(49, writtenData.size());
        assertEquals(100, getLastReportedProgress());
    }

    private long readCurrentTstates(TapePlaybackImpl target) {
        try {
            Field field = TapePlaybackImpl.class.getDeclaredField("currentTstates");
            field.setAccessible(true);
            return field.getLong(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testFirstPulseIsZero() throws InterruptedException {
        // onFileStart: pulseUp=false, so first scheduled pulse writes 0
        playback.onFileStart();
        drainPulses(100, 100_000);
        assertEquals("First pulse should be 0 (pulseUp starts false)", (byte) 0, (byte) writtenData.get(0));
    }

    @Test
    public void testPulsesContainBothZerosAndOnes() throws InterruptedException {
        // Schedule multiple pulses that alternate 0/1
        playback.onFileStart();
        playback.onBlockFlag(0x00); // 16 data pulses

        drainPulses(100, 100_000);

        assertTrue("Expected multiple pulses", writtenData.size() > 1);

        // Verify that both 0 and 1 appear (pulses alternate but may fire out of order with large jumps)
        boolean hasZero = writtenData.contains((byte) 0);
        boolean hasOne = writtenData.contains((byte) 1);
        assertTrue("Expected both 0 and 1 pulses", hasZero && hasOne);
    }

    @Test
    public void testOnBlockDataSchedulesTwoPulsesPerBit() throws InterruptedException {
        // 1 byte = 8 bits, each bit produces 2 pulses = 16 pulses per byte
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onBlockData(new byte[]{(byte) 0xFF}); // 16 pulses

        drainPulses(100, 100_000);

        // 1 (PAUSE) + 16 (data byte) = 17 pulses
        assertEquals(17, writtenData.size());
    }

    @Test
    public void testOnBlockChecksumSchedulesPulsesAndSync3() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onBlockChecksum((byte) 0x00); // 16 pulses (1 byte) + 1 SYNC3 pulse

        drainPulses(100, 100_000);

        // 1 (PAUSE) + 16 (checksum byte) + 1 (SYNC3) = 18 pulses
        assertEquals(18, writtenData.size());
    }

    @Test
    public void testOnHeaderStartSchedulesCorrectPulseCount() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onHeaderStart(); // 8063 leader + SYNC1 + SYNC2 = 8065 pulses

        // Need 8066 pulse-firing calls + ~2334 dead calls (7M / 3000) = ~10400 total
        drainPulses(11000, 3000);

        // 1 (PAUSE) + 8065 (header pilot + syncs) = 8066
        assertEquals(8066, writtenData.size());
    }

    @Test
    public void testOnDataStartSchedulesCorrectPulseCount() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onDataStart(); // 3223 leader + SYNC1 + SYNC2 = 3225 pulses

        // Need 3226 pulse-firing calls + ~1750 dead calls (7M / 4000) = ~4976 total
        drainPulses(6000, 4000);

        // 1 (PAUSE) + 3225 (data pilot + syncs) = 3226
        assertEquals(3226, writtenData.size());
    }

    @Test
    public void testOnFileStartResetsState() throws InterruptedException {
        // Schedule some pulses
        playback.onFileStart();
        playback.onBlockFlag(0x00);

        // Reset by calling onFileStart again
        playback.onFileStart();

        drainPulses(100, 100_000);

        // Only 1 PAUSE pulse from the second onFileStart
        assertEquals(1, writtenData.size());
    }

    @Test
    public void testOnStateChangeDoesNotThrowWithoutGui() {
        playback.onStateChange(TapePlaybackController.CassetteState.PLAYING);
        playback.onStateChange(TapePlaybackController.CassetteState.STOPPED);
        playback.onStateChange(TapePlaybackController.CassetteState.UNLOADED);
    }

    @Test
    public void testTurboSpeedDataCompletesWhenCyclesJumpPastWholeBlock() throws InterruptedException {
        playback.onFileStart();
        playback.onTurboSpeedData(100, 200, 300, 400, 800, 2, 8, 1, new byte[]{(byte) 0xA5});

        Thread playThread = startPlaybackAsync();

        playback.passedCycles(Integer.MAX_VALUE);
        playThread.join(1000);

        assertFalse("Turbo playback should finish after draining all overdue pulses", playThread.isAlive());
        assertEquals(22, writtenData.size());
        assertEquals(100, getLastReportedProgress());
    }

    @Test
    public void testOnProgramDoesNotThrowWithoutGui() {
        playback.onProgram("test", 100, 10, 50);
    }

    @Test
    public void testOnNumberArrayDoesNotThrowWithoutGui() {
        playback.onNumberArray("test", 100, 'A');
    }

    @Test
    public void testOnStringArrayDoesNotThrowWithoutGui() {
        playback.onStringArray("test", 100, 'B');
    }

    @Test
    public void testOnMemoryBlockDoesNotThrowWithoutGui() {
        playback.onMemoryBlock("test", 100, 32768);
    }

    @Test
    public void testSetGuiDoesNotThrow() {
        playback.setGui(null);
    }

    @Test
    public void testPlaybackReportsProgressPercentage() throws InterruptedException {
        playback.onFileStart();
        playback.onBlockFlag(0x00);

        Thread playThread = startPlaybackAsync();

        playback.passedCycles(100_000);
        assertTrue("Expected playback progress to advance from 0%", getLastReportedProgress() > 0);
        assertTrue("Expected playback progress to stay below 100% mid-play", getLastReportedProgress() < 100);

        for (int i = 0; i < 100; i++) {
            playback.passedCycles(100_000);
        }
        playThread.join(5000);

        assertEquals("Expected playback progress to end at 100%", 100, getLastReportedProgress());
    }

    @Test
    public void testUnloadResetsPlaybackProgress() throws InterruptedException {
        playback.onFileStart();
        playback.onBlockFlag(0x00);

        Thread playThread = startPlaybackAsync();

        for (int i = 0; i < 100; i++) {
            playback.passedCycles(100_000);
        }
        playThread.join(5000);

        assertEquals(100, getLastReportedProgress());
        assertTrue(getTotalPlayableTstates() > 0);

        playback.onStateChange(TapePlaybackController.CassetteState.UNLOADED);

        assertEquals(-1, getLastReportedProgress());
        assertEquals(0, getTotalPlayableTstates());
    }

    @Test
    public void testOnBlockDataWithEmptyArray() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onBlockData(new byte[0]); // 0 pulses

        drainPulses(100, 100_000);

        // Only 1 PAUSE pulse, no data pulses
        assertEquals(1, writtenData.size());
    }

    @Test
    public void testZeroBitPulseCount() throws InterruptedException {
        // 0x00 = all zero bits, each zero bit uses DATA_PULSE_ZERO_TSTATES = 855
        playback.onFileStart();
        playback.onBlockData(new byte[]{0x00});

        drainPulses(100, 100_000);

        assertEquals(17, writtenData.size()); // 1 PAUSE + 16 data pulses
    }

    @Test
    public void testOneBitPulseCount() throws InterruptedException {
        // 0xFF = all one bits, each one bit uses DATA_PULSE_ONE_TSTATES = 1710
        playback.onFileStart();
        playback.onBlockData(new byte[]{(byte) 0xFF});

        drainPulses(100, 100_000);

        assertEquals(17, writtenData.size()); // 1 PAUSE + 16 data pulses
    }

    @Test
    public void testMultipleDataBytes() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onBlockData(new byte[]{0x01, 0x02, 0x03}); // 3 bytes * 16 pulses = 48 pulses

        drainPulses(200, 100_000);

        // 1 (PAUSE) + 48 (3 bytes) = 49
        assertEquals(49, writtenData.size());
    }

    @Test
    public void testOnBlockFlagSchedulesSixteenPulses() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onBlockFlag(0x00); // 1 byte = 16 pulses

        drainPulses(100, 100_000);

        // 1 (PAUSE) + 16 (flag byte) = 17
        assertEquals(17, writtenData.size());
    }

    @Test
    public void testOnBlockFlagWithAllOnes() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onBlockFlag(0xFF); // 1 byte = 16 pulses

        drainPulses(100, 100_000);

        assertEquals(17, writtenData.size());
    }

    @Test
    public void testOnPauseZeroDoesNotSchedulePulse() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        long before = getCurrentTstates();
        playback.onPause(0); // "stop the tape" — should NOT add a pulse
        long after = getCurrentTstates();

        assertEquals("Pause=0 must not advance T-state cursor", before, after);

        drainPulses(100, 100_000);
        // Only 1 PAUSE from onFileStart
        assertEquals(1, writtenData.size());
    }

    @Test
    public void testOnSetSignalLevelChangesPulsePolarity() throws InterruptedException {
        playback.onFileStart(); // first pulse is 0 (pulseUp=false)
        playback.onSetSignalLevel(1); // pulseUp=true

        // Schedule two more pulses via block flag — first should be 1 (high), second 0 (low)
        playback.onBlockFlag(0x00); // 8 zero bits = 16 pulses

        drainPulses(100, 100_000);
        // Pulse at index 0 = PAUSE (value 0, since pulseUp was false at onFileStart)
        assertEquals((byte) 0, (byte) writtenData.get(0));
        // After setSignalLevel(1), next pulse should be 1
        assertEquals("After setSignalLevel(1), next pulse should be high",
                (byte) 1, (byte) writtenData.get(1));
    }

    @Test
    public void testOnPureToneSchedulesCorrectPulseCount() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onPureTone(1000, 5); // 5 pulses of 1000 T-states

        drainPulses(100, 100_000);
        // 1 (PAUSE) + 5 (pure tone) = 6
        assertEquals(6, writtenData.size());
    }

    @Test
    public void testOnPulseSequenceSchedulesCorrectPulseCount() throws InterruptedException {
        playback.onFileStart(); // 1 PAUSE pulse
        playback.onPulseSequence(new int[]{500, 600, 700}); // 3 pulses

        drainPulses(100, 100_000);
        // 1 (PAUSE) + 3 (pulse sequence) = 4
        assertEquals(4, writtenData.size());
    }

    @Test
    public void testMillisToTstatesGuardsAgainstZeroFrequency() {
        // Frequency supplier returning 0 should be treated as 1 (Math.max(1,...) guard)
        playback = new TapePlaybackImpl(lineIn, () -> 0);
        playback.onFileStart(); // uses millisToTstates(2000) internally
        long tstates = getCurrentTstates();
        // 2000 ms * max(1, 0) kHz = 2000 T-states + initial 1
        assertEquals(2001L, tstates);
    }
}
