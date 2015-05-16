/*
 * Copyright (C) 2015 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.impl.compatible8080;

import emulib.plugins.cpu.CPU;
import net.sf.emustudio.zilogZ80.impl.EmulatorEngine;
import net.sf.emustudio.zilogZ80.impl.InstructionsTest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class InstructionsControlTest extends InstructionsTest {

    @Test
    public void testEI_DI() throws Exception {
        resetProgram(0xFB, 0xF3);

        cpu.step();
        assertTrue(cpu.getEngine().IFF[0]);

        cpu.step();
        assertFalse(cpu.getEngine().IFF[0]);
    }

    @Test
    public void testJP__nn__AND__JP_cc__nn() throws Exception {
        resetProgram(
                0xC3, 4, 0,
                0, // address 3
                0xC2, 8, 0,
                0, // address 7
                0xCA, 0xC, 0,
                0, // address 0xB
                0xD2, 0x10, 0,
                0, // address 0xF
                0xDA, 0x14, 0,
                0, // address 0x13
                0xE2, 0x18, 0,
                0, // address 0x17
                0xEA, 0x1C, 0,
                0, // address 0x1B
                0xF2, 0x20, 0,
                0, // address 0x1F
                0xFA, 0x24, 0,
                0, // address 0x23
                0);

        stepAndCheckPC(4);
        stepAndCheckPC(8);
        setFlags(EmulatorEngine.FLAG_Z);
        stepAndCheckPC(0xC);
        stepAndCheckPC(0x10);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckPC(0x14);
        stepAndCheckPC(0x18);
        setFlags(EmulatorEngine.FLAG_PV);
        stepAndCheckPC(0x1C);
        stepAndCheckPC(0x20);
        setFlags(EmulatorEngine.FLAG_S);
        stepAndCheckPC(0x24);
    }

    @Test
    public void testJP__mHL() throws Exception {
        resetProgram(0xE9);
        setRegister(EmulatorEngine.REG_H, 0x1F);
        setRegister(EmulatorEngine.REG_L, 0x2F);

        stepAndCheckPC(0x1F2F);
    }
    
    @Test
    public void testCALL__nn__AND__CALL_cc__nn() throws Exception {
        resetProgram(
                0xCD, 4, 0,
                0, // address 3
                0xC4, 8, 0,
                0, // address 7
                0xCC, 0xC, 0,
                0, // address 0xB
                0xD4, 0x10, 0,
                0, // address 0xF
                0xDC, 0x14, 0,
                0, // address 0x13
                0xE4, 0x18, 0,
                0, // address 0x17
                0xEC, 0x1C, 0,
                0, // address 0x1B
                0xF4, 0x20, 0,
                0, // address 0x1F
                0xFC, 0x24, 0,
                0, // address 0x23
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        cpu.getEngine().SP = 0x35;
        stepAndCheckPCandSPandMemory(4, 0x33, 3);
        stepAndCheckPCandSPandMemory(8, 0x31, 7);
        setFlags(EmulatorEngine.FLAG_Z);
        stepAndCheckPCandSPandMemory(0xC, 0x2F, 0xB);
        stepAndCheckPCandSPandMemory(0x10, 0x2D, 0xF);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckPCandSPandMemory(0x14, 0x2B, 0x13);
        stepAndCheckPCandSPandMemory(0x18, 0x29, 0x17);
        setFlags(EmulatorEngine.FLAG_PV);
        stepAndCheckPCandSPandMemory(0x1C, 0x27, 0x1B);
        stepAndCheckPCandSPandMemory(0x20, 0x25, 0x1F);
        setFlags(EmulatorEngine.FLAG_S);
        stepAndCheckPCandSPandMemory(0x24, 0x23, 0x23);
    }

    @Test
    public void testRET__AND__RET__cc() throws Exception {
        resetProgram(
                0xC9, 0xC0, 0xC8, 0xD0, 0xD8, 0xE0, 0xE8, 0xF0, 0xF8,
                0, // address 9
                1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8, 0, 9, 0, 0xFF, 0xFF
        );

        cpu.getEngine().SP = 0xA;
        stepAndCheckPCandSPandMemory(1, 0xC, 2);
        stepAndCheckPCandSPandMemory(2, 0xE, 3);
        setFlags(EmulatorEngine.FLAG_Z);
        stepAndCheckPCandSPandMemory(3, 0x10, 4);
        stepAndCheckPCandSPandMemory(4, 0x12, 5);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckPCandSPandMemory(5, 0x14, 6);
        stepAndCheckPCandSPandMemory(6, 0x16, 7);
        setFlags(EmulatorEngine.FLAG_PV);
        stepAndCheckPCandSPandMemory(7, 0x18, 8);
        stepAndCheckPCandSPandMemory(8, 0x1A, 9);
        setFlags(EmulatorEngine.FLAG_S);
        stepAndCheckPCandSPandMemory(9, 0x1C, 0xFFFF);
    }

    @Test
    public void testRST() throws Exception {
        resetProgram(
                0xC7, 0xCF, 0xD7, 0xDF, 0xE7, 0xEF, 0xF7, 0xFF,
                0, // address 8
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        cpu.getEngine().SP = 0x18;
        stepAndCheckPCandSPandMemory(0, 0x16, 1);
        cpu.getEngine().PC = 1;
        stepAndCheckPCandSPandMemory(8, 0x14, 2);
        cpu.getEngine().PC = 2;
        stepAndCheckPCandSPandMemory(0x10, 0x12, 3);
        cpu.getEngine().PC = 3;
        stepAndCheckPCandSPandMemory(0x18, 0x10, 4);
        cpu.getEngine().PC = 4;
        stepAndCheckPCandSPandMemory(0x20, 0xE, 5);
        cpu.getEngine().PC = 5;
        stepAndCheckPCandSPandMemory(0x28, 0xC, 6);
        cpu.getEngine().PC = 6;
        stepAndCheckPCandSPandMemory(0x30, 0xA, 7);
        cpu.getEngine().PC = 7;
        stepAndCheckPCandSPandMemory(0x38, 0x8, 8);
    }

    @Test
    public void testHLT() throws Exception {
        resetProgram(0x76);

        cpu.step();
        checkRunState(CPU.RunState.STATE_STOPPED_NORMAL);
    }
}
