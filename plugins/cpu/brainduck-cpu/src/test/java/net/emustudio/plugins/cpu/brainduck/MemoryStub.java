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
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.cpu.testsuite.memory.ShortMemoryStub;
import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.memory.brainduck.api.RawMemoryContext;

@PluginContext
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
