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
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.jcip.annotations.ThreadSafe;

import java.util.Arrays;

@ThreadSafe
public class MemoryContextImpl extends AbstractMemoryContext<Byte> {
    public static final int NUMBER_OF_CELLS = 32 * 4;

    // byte type is atomic in JVM memory model
    private final byte[] memory = new byte[NUMBER_OF_CELLS];

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
    public Byte[] readWord(int from) {
        return new Byte[]{memory[from], memory[from + 1], memory[from + 2], memory[from + 3]};
    }

    @Override
    public void write(int to, Byte val) {
        memory[to] = val;
        notifyMemoryChanged(to);
    }

    @Override
    public void writeWord(int to, Byte[] cells) {
        int i = 0;
        for (byte cell : cells) {
            memory[to + i] = cell;
            notifyMemoryChanged(to + i);
            i++;
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
}
