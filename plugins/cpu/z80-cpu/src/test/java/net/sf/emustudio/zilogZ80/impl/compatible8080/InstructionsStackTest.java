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

import net.sf.emustudio.zilogZ80.impl.EmulatorEngine;
import net.sf.emustudio.zilogZ80.impl.InstructionsTest;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class InstructionsStackTest extends InstructionsTest {

    @Test
    public void testPUSH_qq() throws Exception {
        resetProgram(
                0xC5, 0xD5, 0xE5, 0xF5,
                0, // address 4
                0, 0, 0, 0, 0, 0, 0, 0
        );
        setRegisters(1, 2, 3, 4, 5, 6, 7);

        cpu.getEngine().SP = 0xC;
        stepAndCheckPCandSPandMemory(1, 0xA, 0x203);
        stepAndCheckPCandSPandMemory(2, 8, 0x405);
        stepAndCheckPCandSPandMemory(3, 6, 0x607);
        setFlags(FLAG_S_Z_H_PV_C);
        
        stepAndCheckPCandSPandMemory(4, 4, 0x01D5);
    }

    @Test
    public void testPOP_qq() throws Exception {
        resetProgram(
                0xC1, 0xD1, 0xE1, 0xF1,
                0, //address 4
                3, 2, 5, 4, 7, 6, 0xD7, 1
        );

        cpu.getEngine().SP = 5;
        stepAndCheckPCandSP(1, 7);
        checkRegister(EmulatorEngine.REG_B, 2);
        checkRegister(EmulatorEngine.REG_C, 3);

        stepAndCheckPCandSP(2, 9);
        checkRegister(EmulatorEngine.REG_D, 4);
        checkRegister(EmulatorEngine.REG_E, 5);

        stepAndCheckPCandSP(3, 0xB);
        checkRegister(EmulatorEngine.REG_H, 6);
        checkRegister(EmulatorEngine.REG_L, 7);

        stepAndCheckPCandSP(4, 0xD);
        checkRegister(EmulatorEngine.REG_A, 1);
        checkFlags(FLAG_S_Z_H_PV_C);
    }
}
