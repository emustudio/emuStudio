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

public class MemoryContextImpl extends AbstractMemoryContext<Short> implements RawMemoryContext {
    private final short[] memory = new short[65536];

    @Override
    public void clear() {
        Arrays.fill(memory, (short) 0);
        notifyMemoryChanged(-1); // notify that all memory has changed
    }

    @Override
    public Short read(int from) {
        return memory[from];
    }

    @Override
    public Short[] readWord(int from) {
        if (from == memory.length - 1) {
            return new Short[]{memory[from], 0};
        }
        return new Short[]{memory[from], memory[from + 1]};
    }

    @Override
    public void write(int to, Short val) {
        memory[to] = val;
        notifyMemoryChanged(to);
    }

    @Override
    public void writeWord(int to, Short[] cells) {
        memory[to] = cells[0];
        notifyMemoryChanged(to);
        if (to < memory.length - 1) {
            memory[to + 1] = cells[1];
            notifyMemoryChanged(to + 1);
        }
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    @Override
    public int getSize() {
        return memory.length;
    }

    @Override
    public short[] getRawMemory() {
        return memory;
    }
}
