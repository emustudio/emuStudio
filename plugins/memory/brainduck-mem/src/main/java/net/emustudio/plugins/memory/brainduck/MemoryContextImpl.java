/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.memory.brainduck;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.brainduck.api.RawMemoryContext;

import java.util.Arrays;

public class MemoryContextImpl extends AbstractMemoryContext<Byte> implements RawMemoryContext {
    private final Byte[] memory = new Byte[65536];

    @Override
    public void clear() {
        Arrays.fill(memory, (byte) 0);
        notifyMemoryChanged(-1); // notify that all memory has changed
    }

    @Override
    public Byte read(int from) {
        return memory[from];
    }

    @Override
    public Byte[] read(int from, int count) {
        int to = Math.min(memory.length, from + count);
        return Arrays.copyOfRange(memory, from, to);
    }

    @Override
    public void write(int to, Byte value) {
        memory[to] = value;
        notifyMemoryChanged(to);
    }

    @Override
    public void write(int to, Byte[] values, int count) {
        System.arraycopy(values, 0, memory, to, count);
        for (int i = 0; i < values.length; i++) {
            notifyMemoryChanged(to + i);
        }
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public int getSize() {
        return memory.length;
    }

    @Override
    public Byte[] getRawMemory() {
        return memory;
    }
}
