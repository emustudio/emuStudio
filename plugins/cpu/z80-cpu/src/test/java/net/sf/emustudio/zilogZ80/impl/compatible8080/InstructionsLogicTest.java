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

public class InstructionsLogicTest extends InstructionsTest {

    @Test
    public void testAND__r() throws Exception {
        resetProgram(0xA7, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5);

        setRegisters(0xC5, 0x5, 0x7, 0x4, 0x1, 0xF0, 0x0F);

        setFlags(FLAG_C | FLAG_N);
        stepAndCheckAccAndFlags(0xC5, FLAG_S_H_PV, FLAG_Z_N_C);
        setFlags(FLAG_C | FLAG_N);
        stepAndCheckAccAndFlags(0x05, FLAG_H_PV, FLAG_S_Z_N_C);
        setFlags(FLAG_C | FLAG_N);
        stepAndCheckAccAndFlags(0x05, FLAG_H_PV, FLAG_S_Z_N_C);
        setFlags(FLAG_C | FLAG_N);
        stepAndCheckAccAndFlags(0x04, FLAG_H, FLAG_S_Z_PV_N_C);
        setFlags(FLAG_C | FLAG_N);
        setRegisters(5);
        stepAndCheckAccAndFlags(0x01, FLAG_H, FLAG_S_Z_PV_N_C);

        setRegisters(0xFF);
        setFlags(FLAG_S | FLAG_C | FLAG_N);
        stepAndCheckAccAndFlags(0xF0, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_PV, FLAG_S_N_C);
    }

    @Test
    public void testAND__mHL() throws Exception {
        resetProgram(
                0xA6,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(FLAG_S);

        stepAndCheck(0xC0, EmulatorEngine.REG_A);
        checkFlags(FLAG_S_H_PV);
        checkNotFlags(FLAG_Z_N_C);
    }

    @Test
    public void testAND__n() throws Exception {
        resetProgram(0xE6, 0xC0);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(0xC0, FLAG_S_H_PV, FLAG_Z_N_C);
    }

    @Test
    public void testOR__r() throws Exception {
        resetProgram(0xB7, 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5);
        setRegisters(0xC5, 0x5, 0x0F, 0x5, 0x1, 0, 0x1F);

        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0xC5, FLAG_S_H_PV, FLAG_Z_N_C);
        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0xC5, FLAG_S_H_PV, FLAG_Z_N_C);
        setFlags(FLAG_N_C);
        setRegisters(0x8);
        stepAndCheckAccAndFlags(0x0F, FLAG_H_PV, FLAG_S_Z_N_C);
        setFlags(FLAG_N_C);
        setRegisters(0xA);
        stepAndCheckAccAndFlags(0x0F, FLAG_H_PV, FLAG_S_Z_N_C);
        setFlags(FLAG_Z_N_C);
        setRegisters(0);
        stepAndCheckAccAndFlags(0x1, FLAG_H, FLAG_S_Z_PV_N_C);
        setRegisters(0);
        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_PV, FLAG_S_N_C);
        stepAndCheckAccAndFlags(0x1F, FLAG_H, FLAG_S_Z_PV_N_C);
    }

    @Test
    public void testOR__mHL() throws Exception {
        resetProgram(
                0xB6,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(FLAG_S_N_C);

        stepAndCheckAccAndFlags(0xFF, FLAG_S_H_PV, FLAG_Z_N_C);
    }

    @Test
    public void testOR__n() throws Exception {
        resetProgram(0xF6, 0xC0);
        setRegister(EmulatorEngine.REG_A, 0x5);
        setFlags(FLAG_PV_N_C);

        stepAndCheckAccAndFlags(0xC5, FLAG_S_H_PV, FLAG_Z_N_C);
    }
    
    @Test
    public void testXOR__r() throws Exception {
        resetProgram(0xAF, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD);
        setRegisters(0xC5, 0x5, 0x0F, 0x8, 0x1, 0xF0, 0x0F);

        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_PV, FLAG_S_N_C);
        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0x05, FLAG_H_PV, FLAG_S_Z_N_C);
        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0x0A, FLAG_H_PV, FLAG_S_Z_N_C);
        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0x2, FLAG_H, FLAG_S_Z_PV_N_C);
        setFlags(FLAG_N_C);
        stepAndCheckAccAndFlags(0x3, FLAG_H_PV, FLAG_S_Z_N_C);
        setRegisters(0xFF);
        setFlags(FLAG_S_N_C);
        resetFlags(FLAG_H);
        stepAndCheckAccAndFlags(0x0F, FLAG_H_PV, FLAG_S_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_PV, FLAG_S_N_C);
    }

    @Test
    public void testXOR__mHL() throws Exception {
        resetProgram(
                0xAE,
                0xC0 // address 1
        );

        setRegister(EmulatorEngine.REG_H, 0);
        setRegister(EmulatorEngine.REG_L, 1);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(FLAG_S_N_C);

        stepAndCheckAccAndFlags(0x3F, FLAG_H_PV, FLAG_S_Z_N_C);
    }

    @Test
    public void testXOR__n() throws Exception {
        resetProgram(0xEE, 0xC0);
        setRegister(EmulatorEngine.REG_A, 0xFF);
        setFlags(FLAG_S_N_C);

        stepAndCheckAccAndFlags(0x3F, FLAG_H_PV, FLAG_S_Z_N_C);
    }
    
    @Test
    public void testCP__r() throws Exception {
        resetProgram(0xBF, 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD);
        setRegisters(0xFF, 1, 0xFE, 1, 0xFF, 0xFF, 0xFF);
        
        stepAndCheckAccAndFlags(0xFF, FLAG_Z_H_N, FLAG_S_PV_C);
        setRegisters(0);
        stepAndCheckAccAndFlags(0, FLAG_S_N_C, FLAG_Z_H_PV);
        setRegisters(0xFF);
        stepAndCheckAccAndFlags(0xFF, FLAG_H_N, FLAG_S_Z_PV_C);
        setRegisters(1);
        stepAndCheckAccAndFlags(1, FLAG_Z_H_N, FLAG_S_PV_C);
        setRegisters(0);
        stepAndCheckAccAndFlags(0, FLAG_N_C, FLAG_S_Z_H_PV);
        setRegisters(1);
        stepAndCheckAccAndFlags(1, FLAG_N_C, FLAG_S_Z_H_PV);
        setRegisters(2);
        stepAndCheckAccAndFlags(2, FLAG_N_C, FLAG_S_Z_H_PV);
    }
    
    @Test
    public void testCP__r__overflow() throws Exception {
        resetProgram(0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD);
        setRegisters(129, 127, 128, 127, 129, 1, 0xFF);

        stepAndCheckAccAndFlags(129, FLAG_PV_N, FLAG_S_Z_H_C);        
        setRegisters(1);
        stepAndCheckAccAndFlags(1, FLAG_S_PV_N_C, FLAG_Z_H);
        setRegisters(129);
        stepAndCheckAccAndFlags(129, FLAG_PV_N, FLAG_S_Z_H_C);
        setRegisters(1);
        stepAndCheckAccAndFlags(1, FLAG_S_H_PV_N_C, FLAG_Z);
        setRegisters(128);
        stepAndCheckAccAndFlags(128, FLAG_PV_N, FLAG_S_Z_H_C);
        setRegisters(127);
        stepAndCheckAccAndFlags(127, FLAG_S_H_PV_N_C, FLAG_Z);
    }    
    
    @Test
    public void testCP__mHL() throws Exception {
        resetProgram(
                0xBE,
                0xC0 // address 1
        );

        setRegisters(0xFF, 0, 0, 0, 0, 0, 1);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(0xFF, FLAG_N, FLAG_S_Z_H_PV_C);
    }
    
    @Test
    public void testCP__n() throws Exception {
        resetProgram(0xFE, 0xC0);

        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(0xFF, FLAG_N, FLAG_S_Z_H_PV_C);
    }
    
    @Test
    public void testDAA() throws Exception {
        resetProgram(0x27, 0x27, 0x27, 0x27, 0x27, 0x27);

        setRegisters(0x9B);
        setFlags(EmulatorEngine.FLAG_S);
        stepAndCheckAccAndFlags(1, FLAG_H_C, FLAG_S_Z_PV_N);

        setRegisters(0x22);
        resetFlags(FLAG_S_Z_H_N_C);
        setFlags(FLAG_PV_N);
        stepAndCheckAccAndFlags(0x22, FLAG_PV_N, FLAG_S_Z_H_C);

        setRegisters(0x22);
        resetFlags(FLAG_S_Z_C);
        setFlags(FLAG_H_PV_N);
        stepAndCheckAccAndFlags(0x28, FLAG_PV_N, FLAG_S_Z_H_C);

        setRegisters(0x22);
        resetFlags(FLAG_S_Z_H_N);
        setFlags(FLAG_PV_C);
        stepAndCheckAccAndFlags(0x82, FLAG_S_PV, FLAG_Z_H_N_C);

        resetFlags(FLAG_S_H_PV_C);
        setFlags(FLAG_Z_PV_N);
        setRegisters(0);
        stepAndCheckAccAndFlags(0, FLAG_Z_PV_N, FLAG_S_H_C);
        
        setRegisters(0xB9);
        setFlags(FLAG_S);
        resetFlags(FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0x19, FLAG_C, FLAG_S_Z_H_PV_N);        
    }

    @Test
    public void testCPL() throws Exception {
        resetProgram(0x2F, 0x2F, 0x2F);

        stepAndCheckAccAndFlags(0xFF, FLAG_H_N, FLAG_S_Z_PV_C);
        stepAndCheckAccAndFlags(0, FLAG_H_N, FLAG_S_Z_PV_C);

        setRegisters(0x2F);
        stepAndCheckAccAndFlags(0xD0, FLAG_H_N, FLAG_S_Z_PV_C);
    }

    @Test
    public void testSCF() throws Exception {
        resetProgram(0x37);

        setFlags(FLAG_H);
        stepAndCheckAccAndFlags(0, FLAG_N_C, FLAG_S_Z_H_PV);
    }

    @Test
    public void testCCF() throws Exception {
        resetProgram(0x3F, 0x3F);

        setFlags(FLAG_H_N);
        stepAndCheckAccAndFlags(0, FLAG_H_C, FLAG_S_Z_PV_N);
        stepAndCheckAccAndFlags(0, FLAG_H, FLAG_S_Z_PV_N_C);
    }

    @Test
    public void testRLCA() throws Exception {
        resetProgram(0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07);

        setRegisters(0xF2);
        stepAndCheckAccAndFlags(0xE5, FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xCB, FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0x97, FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0x2F, FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0x5E, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0xBC, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x79, FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xF2, -1, FLAG_S_Z_H_PV_C);
    }

    @Test
    public void testRRCA() throws Exception {
        resetProgram(0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F);

        setRegisters(0xF2);
        stepAndCheckAccAndFlags(0x79, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0xBC, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0x5E, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x2F, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x97, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xCB, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xE5, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xF2, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
    }

    @Test
    public void testRLA() throws Exception {
        resetProgram(0x17, 0x17, 0x17, 0x17, 0x17, 0x17, 0x17, 0x17);

        setRegisters(0xB5);
        stepAndCheckAccAndFlags(0x6A, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xD5, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0xAA, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0x55, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xAB, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x56, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xAD, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x5A, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
    }

    @Test
    public void testRRA() throws Exception {
        resetProgram(0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F);

        setRegisters(0x6A);
        setFlags(EmulatorEngine.FLAG_C);
        stepAndCheckAccAndFlags(0xB5, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x5A, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xAD, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x56, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xAB, -1, FLAG_S_Z_H_PV_C);
        stepAndCheckAccAndFlags(0x55, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xAA, EmulatorEngine.FLAG_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(0xD5, -1, FLAG_S_Z_H_PV_C);
    }

}
