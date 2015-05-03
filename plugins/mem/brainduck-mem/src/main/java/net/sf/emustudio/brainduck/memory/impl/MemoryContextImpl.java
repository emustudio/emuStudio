/*
 * MemoryContextImpl.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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

public class MemoryContextImpl extends AbstractMemoryContext<Short, Integer> {

    private short[] memory; // this array is the operating memory

    public MemoryContextImpl() {
        super();
    }

    /**
     * Initializes the memory context. 
     * 
     * It is called by the main class.
     * 
     * @param size  size of the memory
     * @return      true if the initialization was successful, false otherwise
     */
    public boolean init(int size) {
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
    public Class<?> getDataType() {
        return Short.class;
    }

    @Override
    public Short read(int from) {
        return memory[from];
    }

    @Override
    public Integer readWord(int from) {
        if (from == memory.length - 1) {
            return (int)memory[from];
        }
        int low = memory[from] & 0xFF;
        int high = memory[from + 1];
        return ((high << 8) | low);
    }

    @Override
    public void write(int to, Short val) {
        memory[to] = (short) (val & 0xFF);
        notifyMemoryChanged(to);
    }

    @Override
    public void writeWord(int to, Integer val) {
        short low = (short) (val & 0xFF);
        memory[to] = low;
        notifyMemoryChanged(to);
        if (to < memory.length - 1) {
            short high = (short) ((val >>> 8) & 0xFF);
            memory[to + 1] = high;
            notifyMemoryChanged(to + 1);
        }
    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
