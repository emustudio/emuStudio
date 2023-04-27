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

//Machine	Pilot pulse	Length	Sync1	Sync2	Bit 0	Bit 1
//ZX Spectrum	2168	(1)	667	735	855	1710
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
}
