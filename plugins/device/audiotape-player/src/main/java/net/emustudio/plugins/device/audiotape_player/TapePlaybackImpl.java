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
    private final static int LEADER_PULSE_TSTATES = 2168;
    private final static int SYNC1_PULSE_TSTATES = 667;
    private final static int SYNC2_PULSE_TSTATES = 735;
    private final static int SYNC3_PULSE_TSTATES = 954;
    private final static int PAUSE_PULSE_TSTATES = 7000000;
    private final static int HEADER_LEADER_PULSE_COUNT = 8063;
    private final static int DATA_LEADER_PULSE_COUNT = 3223;
    private final static int DATA_PULSE_ONE_TSTATES = 1710;
    private final static int DATA_PULSE_ZERO_TSTATES = 855;

    private final DeviceContext<Byte> lineIn;
    private final AtomicReference<TapePlayerGui> gui = new AtomicReference<>();

    private final NavigableMap<Long, Runnable> loaderSchedule = new TreeMap<>();
    private long currentTstates;
    private boolean pulseUp;

    private volatile boolean playing;
    private long playingTstates;
    private final CyclicBarrier barrier = new CyclicBarrier(2);

    public TapePlaybackImpl(DeviceContext<Byte> lineIn) {
        this.lineIn = Objects.requireNonNull(lineIn);
    }

    public void setGui(TapePlayerGui gui) {
        this.gui.set(gui);
    }

    @Override
    public void onFileStart() {
        loaderSchedule.clear();
        currentTstates = 1;
        pulseUp = false;
        schedulePulse(PAUSE_PULSE_TSTATES, "PAUSE", "");
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
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setCassetteState(state));
    }

    private void logPulse(long tstate, int length, String eventType, String details) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.addPulseRow(tstate, length, eventType, details));
    }

    private void logProgramDetail(String eventType, String details) {
        long tstate = currentTstates;
        Optional.ofNullable(gui.get()).ifPresent(g -> g.addProgramDetail(tstate, eventType, details));
    }

    private void transmitByte(int data, String eventType, String details) {
        int mask = 0x80; // 1000 0000
        while (mask != 0) {
            int pulseLength = ((data & mask) == 0) ? DATA_PULSE_ZERO_TSTATES : DATA_PULSE_ONE_TSTATES;
            schedulePulse(pulseLength, eventType, details); // 2x according to https://sinclair.wiki.zxnet.co.uk/wiki/Spectrum_tape_interface
            schedulePulse(pulseLength, "", "");
            eventType = "";
            details = "";
            mask >>>= 1;
        }
    }

    private void schedulePulse(int length, String eventType, String details) {
        final long tstate = currentTstates;
        Runnable one = () -> {
            if (!eventType.isEmpty()) {
                logPulse(tstate, length, eventType, details);
            }
            lineIn.writeData((byte) 1);
        };
        Runnable zero = () -> {
            if (!eventType.isEmpty()) {
                logPulse(tstate, length, eventType, details);
            }
            lineIn.writeData((byte) 0);
        };

        loaderSchedule.put(currentTstates, pulseUp ? one : zero);
        currentTstates += length;
        pulseUp = !pulseUp;
    }

    private void playPulses() {
        playingTstates = 0;
        playing = true;
    }

    @Override
    public void passedCycles(long tstates) {
        if (playing) {
            playingTstates += tstates;
            Map.Entry<Long, Runnable> entry = loaderSchedule.floorEntry(playingTstates);
            if (entry != null) {
                loaderSchedule.remove(entry.getKey());
                entry.getValue().run();
            }
            if (loaderSchedule.isEmpty()) {
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
}
