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

public class CassetteListenerImpl implements Loader.CassetteListener {
    private final DeviceContext<Byte> lineIn;
    private Optional<CassettePlayerGui> gui = Optional.empty();

    public CassetteListenerImpl(DeviceContext<Byte> lineIn) {
        this.lineIn = Objects.requireNonNull(lineIn);
    }

    public void setGui(CassettePlayerGui gui) {
        this.gui = Optional.ofNullable(gui);
    }

    @Override
    public void onProgram(String filename, int dataLength, int autoStart, int programLength) {
        gui.ifPresent(g -> g.setMetadata(filename));
    }

    @Override
    public void onNumberArray(String filename, int dataLength, char variable) {
        gui.ifPresent(g -> g.setMetadata(filename));
    }

    @Override
    public void onStringArray(String filename, int dataLength, char variable) {
        gui.ifPresent(g -> g.setMetadata(filename));
    }

    @Override
    public void onMemoryBlock(String filename, int dataLength, int startAddress) {
        gui.ifPresent(g -> g.setMetadata(filename));
    }

    @Override
    public void onData(byte[] data) {

    }

    @Override
    public void onPause(int millis) {
        gui.ifPresent(g -> g.setMetadata("PAUSE " + millis + "ms"));
    }
}
