/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.audiotape_player.gui.TapePlayerGui;
import net.emustudio.plugins.device.audiotape_player.loaders.Loader;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * Tape playback.
 * <p>
 * ZX-Spectrum 48K pulses (for TAP file):
 * For each block:
 * - on header block: 8063 pulses of 2168 t-states
 * - on data block: 3223 pulses of 2168 t-states
 * - sync1 (667 t-states)
 * - sync2 (735 t-states)
 * - block data: 2 pulses of 855 t-states (zero) or 1710 t-states (one) for each byte
 * (this is: flag (1 byte), data (xx - flag - checksum), checksum (1 byte)
 * <p>
 * Links:
 * - <a href="https://worldofspectrum.org/faq/reference/48kreference.htm">ZX-Spectrum 48K Technical Reference</a>
 * - <a href="https://softspectrum48.weebly.com/notes/tape-loading-routines">Tape loading routines</a>
 * - <a href="https://sinclair.wiki.zxnet.co.uk/wiki/Spectrum_tape_interface">Spectrum tape interface</a>
 */
public class TapePlaybackImpl implements Loader.TapePlayback, CPUContext.PassedCyclesListener {
    private static final int FILE_START_PAUSE_MS = 2000;
    private final static int LEADER_PULSE_TSTATES = 2168;
    private final static int SYNC1_PULSE_TSTATES = 667;
    private final static int SYNC2_PULSE_TSTATES = 735;
    private final static int SYNC3_PULSE_TSTATES = 954;
    private final static int HEADER_LEADER_PULSE_COUNT = 8063;
    private final static int DATA_LEADER_PULSE_COUNT = 3223;
    private final static int DATA_PULSE_ONE_TSTATES = 1710;
    private final static int DATA_PULSE_ZERO_TSTATES = 855;

    private final DeviceContext<Byte> lineIn;
    private final IntSupplier cpuFrequencyKHzSupplier;
    private final AtomicReference<TapePlayerGui> gui = new AtomicReference<>();

    private final NavigableMap<Long, Runnable> loaderSchedule = new TreeMap<>();
    private long currentTstates;
    private boolean pulseUp;
    private boolean lastIntervalNeedsClosingEdge;

    private volatile boolean playing;
    private long playingTstates;
    private volatile long totalPlayableTstates;
    private volatile int lastReportedProgress = -1;
    private final CyclicBarrier barrier = new CyclicBarrier(2);


    /**
     * @param lineIn                  device receiving the tape pulses (one byte per edge: 0 or 1)
     * @param cpuFrequencyKHzSupplier returns the current CPU frequency in kHz; consulted on every
     *                                ms-to-T-state conversion so that frequency changes at runtime
     *                                are honoured. Pulse lengths defined by TAP/TZX in raw T-states
     *                                are NOT scaled — tape file formats specify edges in T-states
     *                                directly and the ROM loader counts T-states regardless of clock.
     */
    public TapePlaybackImpl(DeviceContext<Byte> lineIn, IntSupplier cpuFrequencyKHzSupplier) {
        this.lineIn = Objects.requireNonNull(lineIn);
        this.cpuFrequencyKHzSupplier = Objects.requireNonNull(cpuFrequencyKHzSupplier);
    }

    public void setGui(TapePlayerGui gui) {
        this.gui.set(gui);
        if (gui != null) {
            if (lastReportedProgress >= 0) {
                gui.setPlaybackProgress(lastReportedProgress);
            } else {
                gui.resetPlaybackProgress();
            }
        }
    }

    @Override
    public void onFileStart() {
        loaderSchedule.clear();
        currentTstates = 1;
        pulseUp = false;
        lastIntervalNeedsClosingEdge = false;
        resetPlaybackMetrics();
        updatePlaybackProgress(0);
        schedulePulse(millisToTstates(FILE_START_PAUSE_MS), "PAUSE", "", false);
    }

    @Override
    public void onHeaderStart() {
        String eventType = "PILOT";
        String details = "header, " + HEADER_LEADER_PULSE_COUNT + " pulses";
        for (int i = 0; i < HEADER_LEADER_PULSE_COUNT; i++) {
            schedulePulse(LEADER_PULSE_TSTATES, eventType, details);
            eventType = "";
            details = "";
        }
        schedulePulse(SYNC1_PULSE_TSTATES, "SYNC1", "");
        schedulePulse(SYNC2_PULSE_TSTATES, "SYNC2", "");
    }

    @Override
    public void onDataStart() {
        String eventType = "PILOT";
        String details = "data, " + DATA_LEADER_PULSE_COUNT + " pulses";
        for (int i = 0; i < DATA_LEADER_PULSE_COUNT; i++) {
            schedulePulse(LEADER_PULSE_TSTATES, eventType, details);
            eventType = "";
            details = "";
        }
        schedulePulse(SYNC1_PULSE_TSTATES, "SYNC1", "");
        schedulePulse(SYNC2_PULSE_TSTATES, "SYNC2", "");
    }

    @Override
    public void onBlockFlag(int flag) {
        transmitByte(flag, "FLAG", String.format("0x%02X", flag & 0xFF));
    }

    @Override
    public void onProgram(String filename, int dataLength, int autoStart, int programLength) {
        logProgramDetail("PROGRAM", filename + " (start=" + autoStart + ", length=" + programLength + ")");
    }

    @Override
    public void onNumberArray(String filename, int dataLength, char variable) {
        logProgramDetail("NUMBER ARRAY", filename + " (variable=" + variable + ")");
    }

    @Override
    public void onStringArray(String filename, int dataLength, char variable) {
        logProgramDetail("STRING ARRAY", filename + " (variable=" + variable + ")");
    }

    @Override
    public void onMemoryBlock(String filename, int dataLength, int startAddress) {
        logProgramDetail("MEMORY BLOCK", filename + " (start=" + startAddress + ")");
    }

    @Override
    public void onBlockData(byte[] data) {
        String eventType = "DATA";
        String details = String.format("length=0x%04X", data.length & 0xFFFF);
        for (byte d : data) {
            transmitByte(d & 0xFF, eventType, details);
            eventType = "";
            details = "";
        }
    }

    @Override
    public void onBlockChecksum(byte checksum) {
        transmitByte(checksum & 0xFF, "CHECKSUM", String.format("0x%02X", checksum & 0xFF));
        schedulePulse(SYNC3_PULSE_TSTATES, "SYNC3", "");
    }

    @Override
    public void onFileEnd() {
        ensureClosingEdgeAtFileEnd();
        totalPlayableTstates = loaderSchedule.isEmpty() ? 0L : currentTstates;
        barrier.reset();
        playPulses();
        try {
            barrier.await();
        } catch (InterruptedException e) {
            // cancel playing
            playing = false;
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStateChange(TapePlaybackController.CassetteState state) {
        if ((state == TapePlaybackController.CassetteState.UNLOADED)
                || (state == TapePlaybackController.CassetteState.CLOSED)) {
            resetPlaybackMetrics();
        }
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setCassetteState(state));
    }

    @Override
    public void onTurboSpeedData(int pilotPulseLen, int sync1PulseLen, int sync2PulseLen,
                                 int zeroBitPulseLen, int oneBitPulseLen, int pilotToneCount,
                                 int usedBitsInLastByte, int pauseAfterMs, byte[] data) {
        // Pilot tone
        String eventType = "PILOT";
        String details = "turbo, " + pilotToneCount + " pulses";
        for (int i = 0; i < pilotToneCount; i++) {
            schedulePulse(pilotPulseLen, eventType, details);
            eventType = "";
            details = "";
        }

        // Sync pulses
        schedulePulse(sync1PulseLen, "SYNC1", "turbo");
        schedulePulse(sync2PulseLen, "SYNC2", "turbo");

        // Data
        String dataEvent = "TURBO DATA";
        String dataDetails = String.format("length=0x%04X", data.length & 0xFFFF);
        for (int i = 0; i < data.length; i++) {
            int bits = (i == data.length - 1) ? usedBitsInLastByte : 8;
            transmitBits(data[i] & 0xFF, zeroBitPulseLen, oneBitPulseLen, bits, dataEvent, dataDetails);
            dataEvent = "";
            dataDetails = "";
        }

        // Pause
        if (pauseAfterMs > 0) {
            schedulePulse(millisToTstates(pauseAfterMs), "PAUSE", pauseAfterMs + " ms", false);
        }
    }

    @Override
    public void onPureTone(int pulseLength, int pulseCount) {
        String eventType = "PURE TONE";
        String details = pulseCount + " pulses, " + pulseLength + " T";
        for (int i = 0; i < pulseCount; i++) {
            schedulePulse(pulseLength, eventType, details);
            eventType = "";
            details = "";
        }
    }

    @Override
    public void onPulseSequence(int[] pulseLengths) {
        String eventType = "PULSE SEQ";
        String details = pulseLengths.length + " pulses";
        for (int pulseLength : pulseLengths) {
            schedulePulse(pulseLength, eventType, details);
            eventType = "";
            details = "";
        }
    }

    @Override
    public void onPureData(int zeroBitPulseLen, int oneBitPulseLen,
                           int usedBitsInLastByte, int pauseAfterMs, byte[] data) {
        String eventType = "PURE DATA";
        String details = String.format("length=0x%04X", data.length & 0xFFFF);
        for (int i = 0; i < data.length; i++) {
            int bits = (i == data.length - 1) ? usedBitsInLastByte : 8;
            transmitBits(data[i] & 0xFF, zeroBitPulseLen, oneBitPulseLen, bits, eventType, details);
            eventType = "";
            details = "";
        }

        if (pauseAfterMs > 0) {
            schedulePulse(millisToTstates(pauseAfterMs), "PAUSE", pauseAfterMs + " ms", false);
        }
    }

    @Override
    public void onDirectRecording(int tstatesPerSample, int pauseAfterMs,
                                  int usedBitsInLastByte, byte[] samples) {
        String eventType = "DIRECT REC";
        String details = String.format("length=0x%04X, %d T/sample", samples.length & 0xFFFF, tstatesPerSample);
        for (int i = 0; i < samples.length; i++) {
            int bits = (i == samples.length - 1) ? usedBitsInLastByte : 8;
            int mask = 0x80;
            for (int b = 0; b < bits; b++) {
                boolean high = (samples[i] & mask) != 0;
                scheduleDirectSample(tstatesPerSample, high, eventType, details);
                eventType = "";
                details = "";
                mask >>>= 1;
            }
        }

        if (pauseAfterMs > 0) {
            schedulePulse(millisToTstates(pauseAfterMs), "PAUSE", pauseAfterMs + " ms", false);
        }
    }

    @Override
    public void onCswRecording(int pauseAfterMs, int sampleRate, int compressionType,
                               long storedPulseCount, byte[] data) {
        logProgramDetail("CSW REC", String.format(
                "rate=%d Hz, compression=%d, pulses=%d (not yet fully supported)",
                sampleRate, compressionType, storedPulseCount));
    }

    @Override
    public void onGeneralizedData(int pauseAfterMs, byte[] data) {
        logProgramDetail("GEN DATA", String.format(
                "pause=%d ms, length=0x%04X (not yet fully supported)",
                pauseAfterMs, data.length & 0xFFFF));
    }

    @Override
    public void onPause(int durationMs) {
        if (durationMs == 0) {
            logProgramDetail("STOP TAPE", "Stop the tape (pause=0)");
        } else {
            schedulePulse(millisToTstates(durationMs), "PAUSE", durationMs + " ms", false);
        }
    }

    @Override
    public void onGroupStart(String name) {
        logProgramDetail("GROUP", name);
    }

    @Override
    public void onGroupEnd() {
        logProgramDetail("GROUP END", "");
    }

    @Override
    public void onStopIfIn48KMode() {
        logProgramDetail("STOP 48K", "Stop if in 48K mode");
    }

    @Override
    public void onSetSignalLevel(int level) {
        pulseUp = (level != 0);
        logProgramDetail("SIGNAL", "level=" + level);
    }

    @Override
    public void onTextDescription(String text) {
        logProgramDetail("TEXT", text);
    }

    @Override
    public void onMessage(String message, int displayTimeSeconds) {
        logProgramDetail("MESSAGE", message + " (" + displayTimeSeconds + "s)");
    }

    @Override
    public void onArchiveInfo(java.util.List<String[]> entries) {
        String details = entries.stream()
                .map(e -> e[0] + ": " + e[1])
                .collect(Collectors.joining("; "));
        logProgramDetail("ARCHIVE", details);
    }

    @Override
    public void onHardwareInfo(java.util.List<int[]> entries) {
        logProgramDetail("HARDWARE", entries.size() + " entries");
    }

    @Override
    public void onCustomInfo(String id, byte[] data) {
        logProgramDetail("CUSTOM", id + " (" + data.length + " bytes)");
    }

    @Override
    public void onGlueBlock() {
        logProgramDetail("GLUE", "");
    }

    private void logPulse(long tstate, int length, String eventType, String details) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.addPulseRow(tstate, length, eventType, details));
    }

    private void logProgramDetail(String eventType, String details) {
        long tstate = currentTstates;
        Optional.ofNullable(gui.get()).ifPresent(g -> g.addProgramDetail(tstate, eventType, details));
    }

    private void transmitByte(int data, String eventType, String details) {
        transmitBits(data, DATA_PULSE_ZERO_TSTATES, DATA_PULSE_ONE_TSTATES, 8, eventType, details);
    }

    /**
     * Transmit bits of a byte with configurable pulse lengths and bit count.
     *
     * @param data         byte value (bits read from MSB)
     * @param zeroPulseLen T-states for zero bit pulse
     * @param onePulseLen  T-states for one bit pulse
     * @param bits         number of bits to transmit (1-8)
     * @param eventType    event type for logging (first call only)
     * @param details      event details for logging (first call only)
     */
    private void transmitBits(int data, int zeroPulseLen, int onePulseLen, int bits,
                              String eventType, String details) {
        int mask = 0x80; // start from MSB
        for (int i = 0; i < bits; i++) {
            int pulseLength = ((data & mask) == 0) ? zeroPulseLen : onePulseLen;
            schedulePulse(pulseLength, eventType, details);
            schedulePulse(pulseLength, "", "");
            eventType = "";
            details = "";
            mask >>>= 1;
        }
    }

    private void schedulePulse(int length, String eventType, String details) {
        schedulePulse(length, eventType, details, true);
    }

    private void schedulePulse(int length, String eventType, String details, boolean needsClosingEdge) {
        final long tstate = currentTstates;
        Runnable one = () -> {
            if (!eventType.isEmpty()) {
                logPulse(tstate, length, eventType, details);
            }
            writeLineValue((byte) 1);
        };
        Runnable zero = () -> {
            if (!eventType.isEmpty()) {
                logPulse(tstate, length, eventType, details);
            }
            writeLineValue((byte) 0);
        };

        loaderSchedule.put(currentTstates, pulseUp ? one : zero);
        currentTstates += length;
        pulseUp = !pulseUp;
        lastIntervalNeedsClosingEdge = needsClosingEdge;
    }

    /**
     * Schedule a direct sample (doesn't toggle pulse - uses the sample value directly).
     */
    private void scheduleDirectSample(int tstatesPerSample, boolean high,
                                      String eventType, String details) {
        final long tstate = currentTstates;
        byte value = high ? (byte) 1 : (byte) 0;
        loaderSchedule.put(currentTstates, () -> {
            if (!eventType.isEmpty()) {
                logPulse(tstate, tstatesPerSample, eventType, details);
            }
            writeLineValue(value);
        });
        currentTstates += tstatesPerSample;
        pulseUp = high;
        lastIntervalNeedsClosingEdge = false;
    }

    private void playPulses() {
        playingTstates = 0;
        updatePlaybackProgress(0);
        playing = true;
    }

    @Override
    public void passedCycles(long tstates) {
        if (playing) {
            playingTstates += tstates;
            updatePlaybackProgressFromCurrentPosition();
            Map.Entry<Long, Runnable> entry = loaderSchedule.firstEntry();
            while ((entry != null) && (entry.getKey() <= playingTstates)) {
                loaderSchedule.pollFirstEntry().getValue().run();
                entry = loaderSchedule.firstEntry();
            }
            if (loaderSchedule.isEmpty() && playingTstates >= totalPlayableTstates) {
                updatePlaybackProgress(100);
                playing = false;
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException ignored) {

                }
            }
        }
    }

    private void resetPlaybackMetrics() {
        playing = false;
        playingTstates = 0;
        totalPlayableTstates = 0;
        lastReportedProgress = -1;
    }

    private void updatePlaybackProgressFromCurrentPosition() {
        long totalTstates = totalPlayableTstates;
        if (totalTstates <= 0) {
            return;
        }
        long playedTstates = Math.max(0, playingTstates);
        int progress = (int) Math.min(100, (playedTstates * 100) / totalTstates);
        updatePlaybackProgress(progress);
    }

    private void updatePlaybackProgress(int progress) {
        int clampedProgress = Math.max(0, Math.min(100, progress));
        if (lastReportedProgress != clampedProgress) {
            lastReportedProgress = clampedProgress;
            Optional.ofNullable(gui.get()).ifPresent(g -> g.setPlaybackProgress(clampedProgress));
        }
    }

    private void writeLineValue(byte value) {
        lineIn.writeData(value);
    }

    private void ensureClosingEdgeAtFileEnd() {
        if (!lastIntervalNeedsClosingEdge || loaderSchedule.isEmpty()) {
            return;
        }

        final byte value = pulseUp ? (byte) 1 : (byte) 0;
        loaderSchedule.put(currentTstates, () -> writeLineValue(value));
        lastIntervalNeedsClosingEdge = false;
    }

    private int millisToTstates(int durationMs) {
        return Math.toIntExact((long) durationMs * Math.max(1, cpuFrequencyKHzSupplier.getAsInt()));
    }
}
