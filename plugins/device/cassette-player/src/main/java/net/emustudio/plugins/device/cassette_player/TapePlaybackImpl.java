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
        pulseUp = true;
    }

    @Override
    public void onHeaderStart() {
        for (int i = 0; i < HEADER_LEADER_PULSE_COUNT; i++) {
            schedulePulse(LEADER_PULSE_TSTATES);
        }
        schedulePulse(SYNC1_PULSE_TSTATES);
        schedulePulse(SYNC2_PULSE_TSTATES);
    }

    @Override
    public void onDataStart() {
        for (int i = 0; i < DATA_LEADER_PULSE_COUNT; i++) {
            schedulePulse(LEADER_PULSE_TSTATES);
        }
        schedulePulse(SYNC1_PULSE_TSTATES);
        schedulePulse(SYNC2_PULSE_TSTATES);
    }

    @Override
    public void onBlockFlag(int flag) {
        transmitByte(flag);
    }

    @Override
    public void onProgram(String filename, int dataLength, int autoStart, int programLength) {
        log(filename + " : PROGRAM (start=" + autoStart + ", length=" + programLength + ")");
    }

    @Override
    public void onNumberArray(String filename, int dataLength, char variable) {
        log(filename + " : NUMBER ARRAY (variable=" + variable + ")");
    }

    @Override
    public void onStringArray(String filename, int dataLength, char variable) {
        log(filename + " : STRING ARRAY (variable=" + variable + ")");
    }

    @Override
    public void onMemoryBlock(String filename, int dataLength, int startAddress) {
        log(filename + " : MEMORY BLOCK (start=" + startAddress + ")");
    }

    @Override
    public void onBlockData(byte[] data) {
        for (byte d : data) {
            transmitByte(d & 0xFF);
        }
    }

    @Override
    public void onBlockChecksum(byte checksum) {
        transmitByte(checksum & 0xFF);
        //  schedulePulse(SYNC3_PULSE_TSTATES);
        //   pulseUp = false;
        //   schedulePulse(PAUSE_PULSE_TSTATES);
    }

    @Override
    public void onFileEnd() {
        playPulses();
    }

    @Override
    public void onStateChange(CassetteController.CassetteState state) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setCassetteState(state));
    }

    private void log(String message) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setMetadata(message));
    }

    private void transmitByte(int data) {
        int mask = 0x80; // 1000 0000
        while (mask != 0) {
            int pulseLength = ((data & mask) == 0) ? DATA_PULSE_ZERO_TSTATES : DATA_PULSE_ONE_TSTATES;
            schedulePulse(pulseLength); // 2x according to https://sinclair.wiki.zxnet.co.uk/wiki/Spectrum_tape_interface
            schedulePulse(pulseLength);
            mask >>>= 1;
        }
    }

    private void schedulePulse(int length) {
        Runnable one = () -> lineIn.writeData((byte) 1);
        Runnable zero = () -> lineIn.writeData((byte) 0);

        loaderSchedule.put(currentTstates, pulseUp ? one : zero);
        currentTstates += length;
        pulseUp = !pulseUp;
    }

    private void playPulses() {
        if (tep == null) {
            tep = tepSupplier.get();
        }
        tep.scheduleOnceMultiple(loaderSchedule);
    }
}
