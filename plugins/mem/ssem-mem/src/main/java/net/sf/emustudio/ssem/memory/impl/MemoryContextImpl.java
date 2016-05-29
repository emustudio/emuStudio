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

public class MemoryContextImpl extends AbstractMemoryContext<Integer> {
    static final int NUMBER_OF_CELLS = 32;

    private final int[] memory = new int[NUMBER_OF_CELLS];

    MemoryContextImpl() {
        super();
    }

    @Override
    public void clear() {
        Arrays.fill(memory, 0);
        notifyMemoryChanged(-1); // notify that all memory has changed
    }

    @Override
    public Class<?> getDataType() {
        return Integer.class;
    }

    @Override
    public Integer read(int from) {
        return memory[from];
    }

    @Override
    public Integer[] readWord(int from) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int to, Integer val) {
        memory[to] = val;
        notifyMemoryChanged(to);
    }

    @Override
    public void writeWord(int to, Integer[] cells) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
