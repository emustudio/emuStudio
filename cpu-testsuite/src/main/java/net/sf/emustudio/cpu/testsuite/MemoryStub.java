/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.cpu.testsuite;

import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;

public class MemoryStub implements MemoryContext<Short> {
    private short[] memory;

    public void setMemory(short[] memory) {
        this.memory = memory;
    }

    @Override
    public Short read(int memoryPosition) {
        return memory[memoryPosition];
    }

    @Override
    public Short[] readWord(int memoryPosition) {
        return new Short[] { memory[memoryPosition], memory[memoryPosition + 1] };
    }

    @Override
    public void write(int memoryPosition, Short value) {
        memory[memoryPosition] = value;
    }

    @Override
    public void writeWord(int memoryPosition, Short[] cells) {
        memory[memoryPosition] = cells[0];
        memory[memoryPosition + 1] = cells[1];
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
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
