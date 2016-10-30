/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.memory.impl;

import emulib.plugins.memory.AbstractMemoryContext;
import java.util.Arrays;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class MemoryContextImpl extends AbstractMemoryContext<Byte> {
    static final int NUMBER_OF_CELLS = 32 * 32;

    // byte type is atomic in JVM memory model
    private final byte[]memory = new byte[NUMBER_OF_CELLS];

    @Override
    public void clear() {
        Arrays.fill(memory, (byte)0);
        notifyMemoryChanged(-1); // notify that all memory has changed
    }

    @Override
    public Class<?> getDataType() {
        return Byte.class;
    }

    @Override
    public Byte read(int from) {
        return memory[from];
    }

    @Override
    public Byte[] readWord(int from) {
        return new Byte[] { memory[from], memory[from+1], memory[from+2], memory[from+3] };
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
            i++;
            notifyMemoryChanged(to+i);
        }
    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
