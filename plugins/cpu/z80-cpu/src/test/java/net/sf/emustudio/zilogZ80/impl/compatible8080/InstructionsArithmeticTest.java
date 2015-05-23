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
import org.junit.Test;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_N;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_Z;
import static org.junit.Assert.assertEquals;

public class InstructionsArithmeticTest extends InstructionsTest {

    @Test
    public void testADD_A__r() throws Exception {
        resetProgram(0x87, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85);

        setRegisters(0xFF, 1, 1, 0xFF, 3, 4, 0xFF-5);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_H_C, FLAG_Z_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(2, FLAG_H_C, FLAG_S_Z_PV_N);
        stepAndCheckAccAndFlags(6, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }

    @Test
    public void testADD_A__r__overflow() throws Exception {
        resetProgram(0x80, 0x81, 0x82, 0x83, 0x84, 0x85);

        setRegisters(0xFF, 128, 1, 128, 129, 127, 128);

        stepAndCheckAccAndFlags(127, FLAG_PV_C, FLAG_S_Z_H_N);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_PV_C, FLAG_S_H_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        setRegisters(1);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
    }
    
    @Test
    public void testADD_A__mHL() throws Exception {
        resetProgram(
                0x86,
                2 // address 1
        );

        setRegister(EmulatorEngine.REG_A, 0xFE);
        setFlags(EmulatorEngine.FLAG_S);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_N);
    }

    @Test
    public void testADD_A__n() throws Exception {
        resetProgram(0xC6, 1);

        setRegisters(0xFF);
        setFlags(EmulatorEngine.FLAG_S);

        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }

    @Test
    public void testADC_A__r() throws Exception {
        resetProgram(0x8F, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D);

        setRegisters(0xFF, 0, 1, 0xFE, 3, 3, 0xFF-5);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_H_C, FLAG_Z_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(2, FLAG_H_C, FLAG_S_Z_PV_N);
        stepAndCheckAccAndFlags(6, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }

    @Test
    public void testADC_A__r__overflow() throws Exception {
        resetProgram(0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D);

        setRegisters(0xFF, 128, 0, 128, 128, 127, 128);

        stepAndCheckAccAndFlags(127, FLAG_PV_C, FLAG_S_Z_H_N);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_PV_C, FLAG_S_H_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
    }
    
    @Test
    public void testADC_A__mHL() throws Exception {
        resetProgram(
                0x8E,
                2 // address 1
        );

        setFlags(FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(3, -1, FLAG_S_Z_H_PV_N_C);
    }

    @Test
    public void testADC_A__n() throws Exception {
        resetProgram(0xCE, 1);

        setRegisters(0xFE);
        setFlags(FLAG_S_C);

        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }

    @Test
    public void testSUB_A__r() throws Exception {
        resetProgram(0x97, 0x90, 0x91, 0x92, 0x93, 0x94, 0x95);
        setRegisters(0xFF, 1, 0xFE, 1, 0xFF, 0xFF, 0xFF);

        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_N_C, FLAG_Z_H_PV);
        stepAndCheckAccAndFlags(1, FLAG_H_N, FLAG_S_Z_PV_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckAccAndFlags(1, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(2, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(3, FLAG_N_C, FLAG_S_Z_H_PV);
    }

    @Test
    public void testSUB_A__r__overflow() throws Exception {
        resetProgram(0x90, 0x91, 0x92, 0x93, 0x94, 0x95);

        setRegisters(129, 127, 128, 127, 129, 1, 0xFF);

        stepAndCheckAccAndFlags(2, FLAG_PV_N, FLAG_S_Z_H_C);
        setRegisters(1);
        stepAndCheckAccAndFlags(129, FLAG_S_PV_N_C, FLAG_Z_H);
        stepAndCheckAccAndFlags(2, FLAG_PV_N, FLAG_S_Z_H_C);
        setRegisters(1);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV_N_C, FLAG_Z);
        stepAndCheckAccAndFlags(127, FLAG_PV_N, FLAG_S_Z_H_C);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV_N_C, FLAG_Z);
    }
    
    @Test
    public void testSUB_A__mHL() throws Exception {
        resetProgram(
                0x96,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(0xFF, FLAG_S_N_C, FLAG_Z_H_PV);
    }

    @Test
    public void testSUB_A__n() throws Exception {
        resetProgram(0xD6, 0xFF);

        setRegisters(1);

        stepAndCheckAccAndFlags(2, FLAG_N_C, FLAG_S_Z_H_PV);
    }

    @Test
    public void testSBC_A__r() throws Exception {
        resetProgram(0x9F, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D);
        setRegisters(0xFF, 0, 0xFD, 0, 0xFE, 0xFE, 0xFE);

        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);

        setFlags(FLAG_C);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_N_C, FLAG_Z_H_PV);

        stepAndCheckAccAndFlags(1, FLAG_H_N, FLAG_S_Z_PV_C);

        setFlags(FLAG_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);

        setFlags(FLAG_C);
        stepAndCheckAccAndFlags(1, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(2, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(3, FLAG_N_C, FLAG_S_Z_H_PV);
    }

    @Test
    public void testSBC_A__r__overflow() throws Exception {
        resetProgram(0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D);

        setRegisters(129, 127, 128, 126, 129, 0, 0xFF);

        stepAndCheckAccAndFlags(2, FLAG_PV_N, FLAG_S_Z_H_C);
        setRegisters(1);
        stepAndCheckAccAndFlags(129, FLAG_S_PV_N_C, FLAG_Z_H);
        stepAndCheckAccAndFlags(2, FLAG_PV_N, FLAG_S_Z_H_C);
        setRegisters(1);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV_N_C, FLAG_Z);
        stepAndCheckAccAndFlags(127, FLAG_PV_N, FLAG_S_Z_H_C);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV_N_C, FLAG_Z);
    }

    @Test
    public void testSBC_A__n() throws Exception {
        resetProgram(0xDE, 0xFF);

        setRegister(EmulatorEngine.REG_A, 2);
        setFlags(FLAG_C);
        
        stepAndCheckAccAndFlags(2, FLAG_N_C, FLAG_S_Z_H_PV);
    }

    @Test
    public void testSBC_A__mHL() throws Exception {
        resetProgram(
                0x9E,
                1 // address 1
        );

        setRegister(EmulatorEngine.REG_A, 2);
        setFlags(FLAG_C);

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);
    }

    @Test
    public void testINC__r() throws Exception {
        resetProgram(0x3C, 0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C, 0x3C);
        setRegisters(0xFF, 0, 0x7F, 0x0F, 2, 0xFE, 0x0E);

        stepAndCheckAccAndFlags(0, FLAG_Z_H, FLAG_S_PV_N_C);

        stepAndCheck(1, EmulatorEngine.REG_B);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        stepAndCheck(0x80, EmulatorEngine.REG_C);
        checkFlags(FLAG_S_H_PV);
        checkNotFlags(FLAG_Z_N_C);

        stepAndCheck(0x10, EmulatorEngine.REG_D);
        checkFlags(FLAG_H);
        checkNotFlags(FLAG_S_Z_PV_N_C);

        stepAndCheck(3, EmulatorEngine.REG_E);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        stepAndCheck(0xFF, EmulatorEngine.REG_H);
        checkFlags(FLAG_S);
        checkNotFlags(FLAG_Z_H_PV_N_C);

        stepAndCheck(0x0F, EmulatorEngine.REG_L);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        setFlags(FLAG_C);
        stepAndCheckAccAndFlags(1, FLAG_C, FLAG_Z_H_PV_N);
    }

    @Test
    public void testINC__mHL() throws Exception {
        resetProgram(
                0x34,
                0xFF // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        stepAndCheckMemoryAndFlags(0, 1, FLAG_Z_H, FLAG_S_PV_N_C);
    }

    @Test
    public void testDEC__r() throws Exception {
        resetProgram(0x3D, 0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D);
        setRegisters(0, 1, 0x80, 0x10, 3, 0xFF, 0x0F);

        stepAndCheckAccAndFlags(0xFF, FLAG_S_N, FLAG_Z_H_PV_C);

        stepAndCheck(0, EmulatorEngine.REG_B);
        checkFlags(FLAG_Z_H_N);
        checkNotFlags(FLAG_S_PV_C);

        stepAndCheck(0x7F, EmulatorEngine.REG_C);
        checkFlags(FLAG_PV_N);
        checkNotFlags(FLAG_S_Z_H_C);

        stepAndCheck(0x0F, EmulatorEngine.REG_D);
        checkFlags(FLAG_N);
        checkNotFlags(FLAG_S_Z_H_PV_C);

        stepAndCheck(2, EmulatorEngine.REG_E);
        checkFlags(FLAG_H_N);
        checkNotFlags(FLAG_S_Z_PV_C);

        stepAndCheck(0xFE, EmulatorEngine.REG_H);
        checkFlags(FLAG_S_H_N);
        checkNotFlags(FLAG_Z_PV_C);

        stepAndCheck(0x0E, EmulatorEngine.REG_L);
        checkFlags(FLAG_H_N);
        checkNotFlags(FLAG_S_Z_PV_C);
    }

    @Test
    public void testDEC__mHL() throws Exception {
        resetProgram(
                0x35,
                0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);

        stepAndCheckMemoryAndFlags(0xFF, 1, FLAG_S_N, FLAG_Z_H_PV_C);
    }

    @Test
    public void testINC__ss() throws Exception {
        resetProgram(0x03, 0x13, 0x23, 0x33);

        setRegisters(0, 0xFF, 0xFF, 0, 0xFF, 0x11, 0xFF);
        cpu.getEngine().SP = 0x0F;

        stepAndCheck(0, EmulatorEngine.REG_B);
        checkRegister(EmulatorEngine.REG_C, 0);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        stepAndCheck(1, EmulatorEngine.REG_D);
        checkRegister(EmulatorEngine.REG_E, 0);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        stepAndCheck(0x12, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        cpu.step();
        assertEquals(0x10, cpu.getEngine().SP);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);
    }

    @Test
    public void testDEC__ss() throws Exception {
        resetProgram(0x0B, 0x1B, 0x2B, 0x3B);

        setRegisters(0, 0, 0, 1, 0, 0x12, 0);
        cpu.getEngine().SP = 0x10;

        stepAndCheck(0xFF, EmulatorEngine.REG_B);
        checkRegister(EmulatorEngine.REG_C, 0xFF);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        stepAndCheck(0, EmulatorEngine.REG_D);
        checkRegister(EmulatorEngine.REG_E, 0xFF);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        stepAndCheck(0x11, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0xFF);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);

        cpu.step();
        assertEquals(0x0F, cpu.getEngine().SP);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);
    }

    @Test
    public void testADD_HL__ss() throws Exception {
        resetProgram(0x09, 0x19, 0x29, 0x39);

        setRegisters(0x12, 0x34, 0x56, 0x78, 0x90, 0xAB, 0xCD);

        stepAndCheck(0xE0, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0x23);
        checkNotFlags(EmulatorEngine.FLAG_C);

        stepAndCheck(0x58, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0xB3);
        checkFlags(EmulatorEngine.FLAG_C);

        stepAndCheck(0xB1, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0x66);
        checkNotFlags(EmulatorEngine.FLAG_C);

        cpu.getEngine().SP = 0x4E9A;
        stepAndCheck(0, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0);
        checkFlags(EmulatorEngine.FLAG_C);
        checkNotFlags(EmulatorEngine.FLAG_Z);
    }
        
}
