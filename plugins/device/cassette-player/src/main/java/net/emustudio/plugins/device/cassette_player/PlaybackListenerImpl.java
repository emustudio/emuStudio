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

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.cassette_player.gui.CassettePlayerGui;
import net.emustudio.plugins.device.cassette_player.loaders.Loader;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

// Pulse length:
// https://worldofspectrum.org/faq/reference/48kreference.htm
// https://softspectrum48.weebly.com/notes/tape-loading-routines

//Machine	Pilot pulse	Length	Sync1	Sync2	Bit 0	Bit 1
//ZX Spectrum	2168	(1)  	667 	735 	855 	1710

// Tape data is encoded as two 855 T-state pulses for binary zero, and two 1,710 T-state pulses for binary one.

// To distinguish header blocks from data blocks, a sequence of leader pulses precedes each type of block.
// The leader pulse is 2,168 T-states long and is repeated 8,063 times for header blocks and 3,223 times for data blocks.
//
// After the leader pulses, two sync pulses (667 T-states plus 735 T-states long) follow to signal the beginning of the
// actual data.
public class PlaybackListenerImpl implements Loader.PlaybackListener {
    private final DeviceContext<Byte> bus;
    private final AtomicReference<CassettePlayerGui> gui = new AtomicReference<>();

    public PlaybackListenerImpl(DeviceContext<Byte> bus) {
        this.bus = Objects.requireNonNull(bus);
    }

    public void setGui(CassettePlayerGui gui) {
        this.gui.set(gui);
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
    public void onData(byte[] data) {
        log("DATA");
    }

    @Override
    public void onPause(int millis) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setMetadata("PAUSE " + millis + "ms"));
    }

    @Override
    public void onStateChange(CassetteController.CassetteState state) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setCassetteState(state));
    }

    private void log(String message) {
        Optional.ofNullable(gui.get()).ifPresent(g -> g.setMetadata(message));
    }

    private void pulse(boolean active, int howLong) {

    }
}
