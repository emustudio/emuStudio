/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.brainduck.memory.impl;

import emulib.plugins.memory.AbstractMemoryContext;
import net.sf.emustudio.brainduck.memory.RawMemoryContext;

public class MemoryContextImpl extends AbstractMemoryContext<Short> implements RawMemoryContext {

    private short[] memory; // this array is the operating memory

    MemoryContextImpl() {
        super();
    }

    /**
     * Initializes the memory context.
     * <p>
     * It is called by the main class.
     *
     * @param size size of the memory
     * @return true if the initialization was successful, false otherwise
     */
    boolean init(int size) {
        memory = new short[size];
        return true;
    }

    public void destroy() {
        clear();
        memory = null;
    }

    @Override
    public void clear() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
        notifyMemoryChanged(-1); // notify that all memory has changed
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
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
    public int getSize() {
        return memory.length;
    }

    @Override
    public short[] getRawMemory() {
        return memory;
    }
}
