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
package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.ContextType;
import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;

@ContextType
public class MemoryStub implements MemoryContext<Short> {
    private final short[] memory;
    private int afterProgram;

    public MemoryStub(int size) {
        this.memory = new short[size];
    }

    public void setProgram(byte[] program) {
        clear();
        for (afterProgram = 0; afterProgram < program.length; afterProgram++) {
            memory[afterProgram] = program[afterProgram];
        }
    }

    public int getDataStart() {
        return afterProgram + 1;
    }

    public void setData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            memory[afterProgram + 1 + i] = data[i];
        }
    }

    @Override
    public void clear() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    @Override
    public void addMemoryListener(Memory.MemoryListener listener) {

    }

    @Override
    public void removeMemoryListener(Memory.MemoryListener listener) {

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
    public Short[] readWord(int from) {
        if (from == memory.length - 1) {
            return new Short[] { memory[from] };
        }
        return new Short[] { memory[from], memory[from + 1] };
    }

    @Override
    public void write(int to, Short val) {
        memory[to] = (short)(val & 0xFF);
    }

    @Override
    public void writeWord(int to, Short[] cells) {
        memory[to] = cells[0];
        if (to < memory.length - 1) {
            memory[to + 1] = cells[1];
        }
    }

    @Override
    public int getSize() {
        return memory.length;
    }

    @Override
    public boolean areMemoryNotificationsEnabled() {
        return false;
    }

    @Override
    public void setMemoryNotificationsEnabled(boolean enabled) {

    }
}
