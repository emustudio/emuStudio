/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.cassette_player;

import net.emustudio.emulib.plugins.cpu.TimedEventsProcessor;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.cassette_player.gui.CassettePlayerGui;
import net.emustudio.plugins.device.cassette_player.loaders.Loader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
public class TapePlaybackImpl implements Loader.TapePlayback {
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
    private final AtomicReference<CassettePlayerGui> gui = new AtomicReference<>();
    private final Supplier<TimedEventsProcessor> tepSupplier;
    private TimedEventsProcessor tep = null;

    private final Map<Integer, Runnable> loaderSchedule = new HashMap<>();
    private int currentTstates;
    private boolean pulseUp;
    private final CyclicBarrier barrier = new CyclicBarrier(2);

    public TapePlaybackImpl(DeviceContext<Byte> lineIn, Supplier<TimedEventsProcessor> tepSupplier) {
        this.lineIn = Objects.requireNonNull(lineIn);
        this.tepSupplier = Objects.requireNonNull(tepSupplier);
    }

    public void setGui(CassettePlayerGui gui) {
        this.gui.set(gui);
    }

    @Override
    public void onFileStart() {
        loaderSchedule.clear();
        currentTstates = 1;
        pulseUp = false;
        schedulePulse(PAUSE_PULSE_TSTATES, "PAUSE");
    }

    @Override
    public void onHeaderStart() {
        String msg = "PILOT (header)";
        for (int i = 0; i < HEADER_LEADER_PULSE_COUNT; i++) {
            schedulePulse(LEADER_PULSE_TSTATES, msg);
            msg = "";
        }
        schedulePulse(SYNC1_PULSE_TSTATES, "SYNC1");
        schedulePulse(SYNC2_PULSE_TSTATES, "SYNC2");
    }

    @Override
    public void onDataStart() {
        String msg = "PILOT (data)";
        for (int i = 0; i < DATA_LEADER_PULSE_COUNT; i++) {
            schedulePulse(LEADER_PULSE_TSTATES, msg);
            msg = "";
        }
        schedulePulse(SYNC1_PULSE_TSTATES, "SYNC1");
        schedulePulse(SYNC2_PULSE_TSTATES, "SYNC2");
    }

    @Override
    public void onBlockFlag(int flag) {
        transmitByte(flag, String.format("FLAG (0x%02X)", flag & 0xFF));
    }

    @Override
    public void onProgram(String filename, int dataLength, int autoStart, int programLength) {
        logProgramDetail(filename,"PROGRAM (start=" + autoStart + ", length=" + programLength + ")");
    }

    @Override
    public void onNumberArray(String filename, int dataLength, char variable) {
        logProgramDetail(filename, "NUMBER ARRAY (variable=" + variable + ")");
    }

    @Override
    public void onStringArray(String filename, int dataLength, char variable) {
        logProgramDetail(filename, "STRING ARRAY (variable=" + variable + ")");
    }

    @Override
    public void onMemoryBlock(String filename, int dataLength, int startAddress) {
        logProgramDetail(filename, "MEMORY BLOCK (start=" + startAddress + ")");
    }

    @Override
    public void onBlockData(byte[] data) {
        String msg = String.format("DATA (length=0x%04X)", data.length & 0xFFFF);
        for (byte d : data) {
            transmitByte(d & 0xFF, msg);
            msg = "";
        }
    }

    @Override
    public void onBlockChecksum(byte checksum) {
        transmitByte(checksum & 0xFF, String.format("CHECKSUM (0x%02X)", checksum & 0xFF));
        schedulePulse(SYNC3_PULSE_TSTATES, "SYNC3");
    }

    @Override
    public void onFileEnd() {
        barrier.reset();
        playPulses();
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStateChange(CassetteController.CassetteState state) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setCassetteState(state));
    }

    private void logPulse(String message) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setPulseInfo(message));
    }

    private void logProgramDetail(String program, String detail) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setProgramDetail(program, detail));
    }

    private void transmitByte(int data, String msg) {
        int mask = 0x80; // 1000 0000
        while (mask != 0) {
            int pulseLength = ((data & mask) == 0) ? DATA_PULSE_ZERO_TSTATES : DATA_PULSE_ONE_TSTATES;
            schedulePulse(pulseLength, msg); // 2x according to https://sinclair.wiki.zxnet.co.uk/wiki/Spectrum_tape_interface
            schedulePulse(pulseLength, "");
            msg="";
            mask >>>= 1;
        }
    }

    private void schedulePulse(int length, String msg) {
        Runnable one = () -> {
            if (!msg.isEmpty()) {
                logPulse(msg);
            }
            lineIn.writeData((byte) 1);
        };
        Runnable zero = () -> {
            if (!msg.isEmpty()) {
                logPulse(msg);
            }
            lineIn.writeData((byte) 0);
        };

        loaderSchedule.put(currentTstates, pulseUp ? one : zero);
        currentTstates += length;
        pulseUp = !pulseUp;
    }

    private void playPulses() {
        if (tep == null) {
            tep = tepSupplier.get();
        }
        loaderSchedule.put(currentTstates, () -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        });
        tep.scheduleOnceMultiple(loaderSchedule);
    }
}
