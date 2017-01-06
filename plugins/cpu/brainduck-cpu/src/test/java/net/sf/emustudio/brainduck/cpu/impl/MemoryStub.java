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
package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.ContextType;
import emulib.runtime.NumberUtils;
import net.sf.emustudio.brainduck.memory.RawMemoryContext;
import net.sf.emustudio.cpu.testsuite.memory.ShortMemoryStub;

@ContextType
public class MemoryStub extends ShortMemoryStub implements RawMemoryContext {
    private int afterProgram;

    MemoryStub() {
        super(NumberUtils.Strategy.LITTLE_ENDIAN);
    }

    void setProgram(byte[] program) {
        clear();
        for (afterProgram = 0; afterProgram < program.length; afterProgram++) {
            memory[afterProgram] = program[afterProgram];
        }
    }

    int getDataStart() {
        return afterProgram + 1;
    }

    void setData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            memory[afterProgram + 1 + i] = data[i];
        }
    }

    @Override
    public short[] getRawMemory() {
        return memory;
    }

}
