/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@PluginContext
public class DeviceStub implements DeviceContext<Byte> {
    private final List<Byte> output = new CopyOnWriteArrayList<>();
    private final Queue<Byte> input = new ConcurrentLinkedQueue<>();

    public void setInput(byte[] input) {
        for (byte value : input) {
            this.input.add(value);
        }
    }

    @Override
    public Byte readData() {
        return input.poll();
    }

    @Override
    public void writeData(Byte data) {
        output.add(data);
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    public boolean wasInputRead() {
        return input.isEmpty();
    }

    public List<Byte> getOutput() {
        return output;
    }
}
