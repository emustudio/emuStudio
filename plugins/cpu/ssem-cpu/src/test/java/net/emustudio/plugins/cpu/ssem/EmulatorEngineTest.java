/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.cpu.testsuite.memory.MemoryStub;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmulatorEngineTest {
    private EmulatorEngine engine;
    private MemoryStub<Byte> memoryStub;

    @Before
    public void setUp() {
        memoryStub = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        engine = new EmulatorEngine(memoryStub, p -> false);
    }

    @Test
    public void testAddition() {
        /*
            01: LDN 29  -- A = -X
            02: SUB 30  -- A = -X - Y
            03: STO 31  -- store -Sum
            04: LDN 31  -- A = -(-Sum)
            05: STO 31  -- store Sum
            06: HLT

            29: NUM 5   -- X Parameter
            30: NUM 3   -- Y Parameter
            31:         -- Sum Result will appear here
        */
        memoryStub.setMemory(new short[]{
                0, 0, 0, 0,
                0xB8, 0x02, 0, 0,
                0x78, 0x01, 0, 0,
                0xF8, 0x06, 0, 0,
                0xF8, 0x02, 0, 0,
                0xF8, 0x06, 0, 0,
                0x00, 0x07, 0, 0,

                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0xA0, 0, 0, 0, 0xC0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        });

        engine.reset(0);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.run());
        assertEquals(8, NumberUtils.readInt(memoryStub.read(31 * 4, 4), memoryStub.getWordReadingStrategy()));
    }

    @Test(timeout = 700)
    public void testEndlessLoopPrevention() {
        // instruction 0 is JMP
        memoryStub.setMemory(new short[32 * 4]);
        engine.reset(0);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.run());
    }
}
