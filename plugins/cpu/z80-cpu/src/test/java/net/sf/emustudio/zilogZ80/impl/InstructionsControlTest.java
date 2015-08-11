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
package net.sf.emustudio.zilogZ80.impl;

import org.junit.Ignore;
import org.junit.Test;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.*;
import static org.junit.Assert.assertEquals;

@Ignore
public class InstructionsControlTest extends InstructionsTest {
    
    @Test
    public void testNOP() {
        resetProgram(0);
        
        setRegisters(1,2,3,4,5,6,7);
        setFlags(FLAG_S_Z_H_PV_N_C);
        
        stepAndCheckAccAndFlags(1, FLAG_S_Z_H_PV_N_C, 0);
        checkRegister(REG_B, 2);
        checkRegister(REG_C, 3);
        checkRegister(REG_D, 4);
        checkRegister(REG_E, 5);
        checkRegister(REG_H, 6);
        checkRegister(REG_L, 7);
    }

    @Test
    public void testIM__0() {
        resetProgram(0xED, 0x46);

        cpu.getEngine().intMode = 2;
        stepWithAssert();
        assertEquals(0, cpu.getEngine().intMode);
    }

    @Test
    public void testIM__1() {
        resetProgram(0xED, 0x56);

        cpu.getEngine().intMode = 2;
        stepWithAssert();
        assertEquals(1, cpu.getEngine().intMode);
    }

    @Test
    public void testIM__2() {
        resetProgram(0xED, 0x5E);

        cpu.getEngine().intMode = 0;
        stepWithAssert();
        assertEquals(2, cpu.getEngine().intMode);
    }

    @Test
    public void testJR__e() {
        resetProgram(0, 0x18, 4, 0x18, 0xFF);

        cpu.getEngine().PC = 1;
        stepAndCheckPC(1 + 6);

        cpu.getEngine().PC = 3;
        stepAndCheckPC(3 + 1);
    }

    @Test
    public void testJR_C__e() {
        resetProgram(0, 0x38, 4, 0x38, 0xFF);

        setFlags(FLAG_C);
        cpu.getEngine().PC = 1;
        stepAndCheckPC(1 + 6);

        cpu.getEngine().PC = 3;
        stepAndCheckPC(3 + 1);
    }

    @Test
    public void testJR_NC__e() {
        resetProgram(0, 0x30, 4, 0x30, 0xFF);

        cpu.getEngine().PC = 1;
        stepAndCheckPC(1 + 6);

        cpu.getEngine().PC = 3;
        stepAndCheckPC(3 + 1);
    }

    @Test
    public void testJR_Z__e() {
        resetProgram(0, 0x28, 4, 0x28, 0xFF);

        setFlags(FLAG_Z);
        cpu.getEngine().PC = 1;
        stepAndCheckPC(1 + 6);

        cpu.getEngine().PC = 3;
        stepAndCheckPC(3 + 1);
    }

    @Test
    public void testJR_NZ__e() {
        resetProgram(0, 0x20, 4, 0x20, 0xFF);

        cpu.getEngine().PC = 1;
        stepAndCheckPC(1 + 6);

        cpu.getEngine().PC = 3;
        stepAndCheckPC(3 + 1);
    }

    @Test
    public void testJP__IX() {
        resetProgram(0xDD, 0xE9);
        setRegisterIX(0x1234);

        stepAndCheckPC(0x1234);
    }

    @Test
    public void testJP__IY() {
        resetProgram(0xFD, 0xE9);
        setRegisterIY(0x1234);

        stepAndCheckPC(0x1234);
    }

    @Test
    public void testDJNZ__e() {
        resetProgram(0, 0x10, 4, 0x10, 0xFF, 0x10, 0xFF);
        setRegisters(0, 3);

        cpu.getEngine().PC = 1;
        stepAndCheckPC(1 + 6);
        checkRegister(EmulatorEngine.REG_B, 2);

        cpu.getEngine().PC = 3;
        stepAndCheckPC(3 + 1);
        checkRegister(EmulatorEngine.REG_B, 1);

        cpu.getEngine().PC = 5;
        stepAndCheckPC(7); // do not jump
        checkRegister(EmulatorEngine.REG_B, 0);
        checkNotFlags(FLAG_Z);
    }
    
    public void testRETI() {
        
    }
    
    public void testRETN() {
        
    }
}
