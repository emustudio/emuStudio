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

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_N;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_Z;
import static net.sf.emustudio.zilogZ80.impl.InstructionsTest.FLAG_S_H_C;
import static net.sf.emustudio.zilogZ80.impl.InstructionsTest.FLAG_S_N;
import static net.sf.emustudio.zilogZ80.impl.InstructionsTest.FLAG_Z_H;
import static net.sf.emustudio.zilogZ80.impl.InstructionsTest.FLAG_Z_H_N;
import org.junit.Test;

public class InstructionsArithmeticTest extends InstructionsTest {
    
    @Test
    public void testADD_A__IX_plus_d() {
        resetProgram(
                0xDD, 0x86, 0x14,
                0xDD, 0x86, 0x15,
                0xDD, 0x86, 0x16,
                0xDD, 0x86, 0x17,
                0xDD, 0x86, 0x18,
                0xDD, 0x86, 0x19,
                0xDD, 0x86, 0x1A,
                
                0xFF,  // address 0x15
                1,     // address 0x16
                1,     // address 0x17
                0xFF,  // address 0x18
                3,    // address 0x19
                4,    // address 0x1A
                0xFF - 5 // address 0x1B
        );
        setRegisterIX(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_H_C, FLAG_Z_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(2, FLAG_H_C, FLAG_S_Z_PV_N);
        stepAndCheckAccAndFlags(6, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }
    
    @Test
    public void testADD_A__IX_plus_d__overflow() {
        resetProgram(
                0xDD, 0x86, 0x11,
                0xDD, 0x86, 0x12,
                0xDD, 0x86, 0x13,
                0xDD, 0x86, 0x14,
                0xDD, 0x86, 0x15,
                0xDD, 0x86, 0x16,
                
                128,  // address 0x12
                1,    // address 0x13
                128,  // address 0x14
                129,  // address 0x15
                127,  // address 0x16
                128   // address 0x17
        );
        setRegisterIX(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(127, FLAG_PV_C, FLAG_S_Z_H_N);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_PV_C, FLAG_S_H_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        setRegisters(1);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
    }
    
    @Test
    public void testADD_A__IY_plus_d() {
        resetProgram(
                0xFD, 0x86, 0x14,
                0xFD, 0x86, 0x15,
                0xFD, 0x86, 0x16,
                0xFD, 0x86, 0x17,
                0xFD, 0x86, 0x18,
                0xFD, 0x86, 0x19,
                0xFD, 0x86, 0x1A,
                
                0xFF,  // address 0x15
                1,     // address 0x16
                1,     // address 0x17
                0xFF,  // address 0x18
                3,    // address 0x19
                4,    // address 0x1A
                0xFF - 5 // address 0x1B
        );
        setRegisterIY(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_H_C, FLAG_Z_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(2, FLAG_H_C, FLAG_S_Z_PV_N);
        stepAndCheckAccAndFlags(6, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }
    
    @Test
    public void testADD_A__IY_plus_d__overflow() {
        resetProgram(
                0xFD, 0x86, 0x11,
                0xFD, 0x86, 0x12,
                0xFD, 0x86, 0x13,
                0xFD, 0x86, 0x14,
                0xFD, 0x86, 0x15,
                0xFD, 0x86, 0x16,
                
                128,  // address 0x12
                1,    // address 0x13
                128,  // address 0x14
                129,  // address 0x15
                127,  // address 0x16
                128   // address 0x17
        );
        setRegisterIY(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(127, FLAG_PV_C, FLAG_S_Z_H_N);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_PV_C, FLAG_S_H_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        setRegisters(1);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
    }    
    
    @Test
    public void testADC_A__IX_plus_d() {
        resetProgram(
                0xDD, 0x8E, 0x14,
                0xDD, 0x8E, 0x15,
                0xDD, 0x8E, 0x16,
                0xDD, 0x8E, 0x17,
                0xDD, 0x8E, 0x18,
                0xDD, 0x8E, 0x19,
                0xDD, 0x8E, 0x1A,
                
                0xFF,  // address 0x15
                0,     // address 0x16
                1,     // address 0x17
                0xFE,  // address 0x18
                3,    // address 0x19
                3,    // address 0x1A
                0xFF - 5 // address 0x1B
        );
        setRegisterIX(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_H_C, FLAG_Z_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(2, FLAG_H_C, FLAG_S_Z_PV_N);
        stepAndCheckAccAndFlags(6, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }

    @Test
    public void testADC_A__IX_plus_d__overflow() {
        resetProgram(
                0xDD, 0x8E, 0x11,
                0xDD, 0x8E, 0x12,
                0xDD, 0x8E, 0x13,
                0xDD, 0x8E, 0x14,
                0xDD, 0x8E, 0x15,
                0xDD, 0x8E, 0x16,
                
                128,  // address 0x12
                0,    // address 0x13
                128,  // address 0x14
                128,  // address 0x15
                127,  // address 0x16
                128   // address 0x17
        );
        setRegisterIX(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(127, FLAG_PV_C, FLAG_S_Z_H_N);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_PV_C, FLAG_S_H_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
    }
    
    @Test
    public void testADC_A__IY_plus_d() {
        resetProgram(
                0xFD, 0x8E, 0x14,
                0xFD, 0x8E, 0x15,
                0xFD, 0x8E, 0x16,
                0xFD, 0x8E, 0x17,
                0xFD, 0x8E, 0x18,
                0xFD, 0x8E, 0x19,
                0xFD, 0x8E, 0x1A,
                
                0xFF,  // address 0x15
                0,     // address 0x16
                1,     // address 0x17
                0xFE,  // address 0x18
                3,    // address 0x19
                3,    // address 0x1A
                0xFF - 5 // address 0x1B
        );
        setRegisterIY(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(0xFE, FLAG_S_H_C, FLAG_Z_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(0xFF, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(2, FLAG_H_C, FLAG_S_Z_PV_N);
        stepAndCheckAccAndFlags(6, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
    }
    
    @Test
    public void testADC_A__IY_plus_d__overflow() {
        resetProgram(
                0xFD, 0x8E, 0x11,
                0xFD, 0x8E, 0x12,
                0xFD, 0x8E, 0x13,
                0xFD, 0x8E, 0x14,
                0xFD, 0x8E, 0x15,
                0xFD, 0x8E, 0x16,
                
                128,  // address 0x12
                0,    // address 0x13
                128,  // address 0x14
                128,  // address 0x15
                127,  // address 0x16
                128   // address 0x17
        );
        setRegisterIY(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);

        stepAndCheckAccAndFlags(127, FLAG_PV_C, FLAG_S_Z_H_N);
        stepAndCheckAccAndFlags(128, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_PV_C, FLAG_S_H_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_C, FLAG_S_PV_N);
        stepAndCheckAccAndFlags(129, FLAG_S, FLAG_Z_H_PV_N_C);        
    }
    
    @Test
    public void testSUB_A__IX_plus_d() {
        resetProgram(
                0xDD, 0x96, 0x14,
                0xDD, 0x96, 0x15,
                0xDD, 0x96, 0x16,
                0xDD, 0x96, 0x17,
                0xDD, 0x96, 0x18,
                0xDD, 0x96, 0x19,
                0xDD, 0x96, 0x1A,
                
                0xFF,  // address 0x15
                1,     // address 0x16
                0xFE,  // address 0x17
                1,     // address 0x18
                0xFF,  // address 0x19
                0xFF,  // address 0x1A
                0xFF   // address 0x1B
        );
        setRegisterIX(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);
                
        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_N_C, FLAG_Z_H_PV);
        stepAndCheckAccAndFlags(1, FLAG_H_N, FLAG_S_Z_PV_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckAccAndFlags(1, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(2, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(3, FLAG_N_C, FLAG_S_Z_H_PV);
    }
    
    @Test
    public void testSUB_A__IX_plus_d__overflow() throws Exception {
        resetProgram(
                0xDD, 0x96, 0x11,
                0xDD, 0x96, 0x12,
                0xDD, 0x96, 0x13,
                0xDD, 0x96, 0x14,
                0xDD, 0x96, 0x15,
                0xDD, 0x96, 0x16,
                
                127,  // address 0x12
                128,  // address 0x13
                127,  // address 0x14
                129,  // address 0x15
                1,    // address 0x16
                0xFF  // address 0x17
        );
        setRegisterIX(1);
        setRegisters(129);
        setFlags(FLAG_S);

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
    public void testSUB_A__IY_plus_d() {
        resetProgram(
                0xFD, 0x96, 0x14,
                0xFD, 0x96, 0x15,
                0xFD, 0x96, 0x16,
                0xFD, 0x96, 0x17,
                0xFD, 0x96, 0x18,
                0xFD, 0x96, 0x19,
                0xFD, 0x96, 0x1A,
                
                0xFF,  // address 0x15
                1,     // address 0x16
                0xFE,  // address 0x17
                1,     // address 0x18
                0xFF,  // address 0x19
                0xFF,  // address 0x1A
                0xFF   // address 0x1B
        );
        setRegisterIY(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);
                
        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckAccAndFlags(0xFF, FLAG_S_N_C, FLAG_Z_H_PV);
        stepAndCheckAccAndFlags(1, FLAG_H_N, FLAG_S_Z_PV_C);
        stepAndCheckAccAndFlags(0, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckAccAndFlags(1, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(2, FLAG_N_C, FLAG_S_Z_H_PV);
        stepAndCheckAccAndFlags(3, FLAG_N_C, FLAG_S_Z_H_PV);
    }
    
    @Test
    public void testSUB_A__IY_plus_d__overflow() throws Exception {
        resetProgram(
                0xFD, 0x96, 0x11,
                0xFD, 0x96, 0x12,
                0xFD, 0x96, 0x13,
                0xFD, 0x96, 0x14,
                0xFD, 0x96, 0x15,
                0xFD, 0x96, 0x16,
                
                127,  // address 0x12
                128,  // address 0x13
                127,  // address 0x14
                129,  // address 0x15
                1,    // address 0x16
                0xFF  // address 0x17
        );
        setRegisterIY(1);
        setRegisters(129);
        setFlags(FLAG_S);

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
    public void testSBC_A__IX_plus_d() {
        resetProgram(
                0xDD, 0x9E, 0x14,
                0xDD, 0x9E, 0x15,
                0xDD, 0x9E, 0x16,
                0xDD, 0x9E, 0x17,
                0xDD, 0x9E, 0x18,
                0xDD, 0x9E, 0x19,
                0xDD, 0x9E, 0x1A,
                
                0xFF,  // address 0x15
                0,     // address 0x16
                0xFD,  // address 0x17
                0,     // address 0x18
                0xFE,  // address 0x19
                0xFE,  // address 0x1A
                0xFE   // address 0x1B
        );
        setRegisterIX(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);
        
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
    public void testSBC_A__IX_plus_d__overflow() {
        resetProgram(
                0xDD, 0x9E, 0x11,
                0xDD, 0x9E, 0x12,
                0xDD, 0x9E, 0x13,
                0xDD, 0x9E, 0x14,
                0xDD, 0x9E, 0x15,
                0xDD, 0x9E, 0x16,
                
                127,  // address 0x12
                128,  // address 0x13
                126,  // address 0x14
                129,  // address 0x15
                0,    // address 0x16
                0xFF  // address 0x17
        );
        setRegisterIX(1);
        setRegisters(129);
        setFlags(FLAG_S);

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
    public void testSBC_A__IY_plus_d() {
        resetProgram(
                0xFD, 0x9E, 0x14,
                0xFD, 0x9E, 0x15,
                0xFD, 0x9E, 0x16,
                0xFD, 0x9E, 0x17,
                0xFD, 0x9E, 0x18,
                0xFD, 0x9E, 0x19,
                0xFD, 0x9E, 0x1A,
                
                0xFF,  // address 0x15
                0,     // address 0x16
                0xFD,  // address 0x17
                0,     // address 0x18
                0xFE,  // address 0x19
                0xFE,  // address 0x1A
                0xFE   // address 0x1B
        );
        setRegisterIY(1);
        setRegisters(0xFF);
        setFlags(FLAG_S);
        
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
    public void testSBC_A__IY_plus_d__overflow() {
        resetProgram(
                0xFD, 0x9E, 0x11,
                0xFD, 0x9E, 0x12,
                0xFD, 0x9E, 0x13,
                0xFD, 0x9E, 0x14,
                0xFD, 0x9E, 0x15,
                0xFD, 0x9E, 0x16,
                
                127,  // address 0x12
                128,  // address 0x13
                126,  // address 0x14
                129,  // address 0x15
                0,    // address 0x16
                0xFF  // address 0x17
        );
        setRegisterIY(1);
        setRegisters(129);
        setFlags(FLAG_S);

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
    public void testINC__IX() {
        resetProgram(
                0xDD, 0x23,
                0xDD, 0x23
        );
        
        setRegisterIX(0xFFFE);
        stepAndCheckIX(0xFFFF);
        stepAndCheckIX(0);
    }
    
    @Test
    public void testINC__IY() {
        resetProgram(
                0xFD, 0x23,
                0xFD, 0x23
        );
        
        setRegisterIY(0xFFFE);
        stepAndCheckIY(0xFFFF);
        stepAndCheckIY(0);
    }

    @Test
    public void testDEC__IX() {
        resetProgram(
                0xDD, 0x2B,
                0xDD, 0x2B
        );
        
        setRegisterIX(0);
        stepAndCheckIX(0xFFFF);
        stepAndCheckIX(0xFFFE);
    }

    @Test
    public void testDEC__IY() {
        resetProgram(
                0xFD, 0x2B,
                0xFD, 0x2B
        );
        
        setRegisterIY(0);
        stepAndCheckIY(0xFFFF);
        stepAndCheckIY(0xFFFE);
    }
    
    @Test
    public void testINC__IX_plus_d() {
        resetProgram(
                0xDD, 0x34, 0x17,
                0xDD, 0x34, 0x18,
                0xDD, 0x34, 0x19,
                0xDD, 0x34, 0x1A,
                0xDD, 0x34, 0x1B,
                0xDD, 0x34, 0x1C,
                0xDD, 0x34, 0x1D,
                0xDD, 0x34, 0x1E,
                
                0xFF,  // address 0x18
                0,     // address 0x19
                0x7F,  // address 0x1A
                0x0F,  // address 0x1B
                2,     // address 0x1C
                0xFE,  // address 0x1D
                0x0E,  // address 0x1E
                0      // address 0x1F 
        );
        setRegisterIX(1);
        
        stepAndCheckMemoryAndFlags(0, 0x18, FLAG_Z_H, FLAG_S_PV_N_C);
        stepAndCheckMemoryAndFlags(1, 0x19, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckMemoryAndFlags(0x80, 0x1A, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckMemoryAndFlags(0x10, 0x1B, FLAG_H, FLAG_S_Z_PV_N_C);
        stepAndCheckMemoryAndFlags(3, 0x1C, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckMemoryAndFlags(0xFF, 0x1D, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckMemoryAndFlags(0x0F, 0x1E, -1, FLAG_S_Z_H_PV_N_C);

        setFlags(FLAG_C);
        stepAndCheckMemoryAndFlags(1, 0x1F, FLAG_C, FLAG_Z_H_PV_N);
    }

    @Test
    public void testINC__IY_plus_d() {
        resetProgram(
                0xFD, 0x34, 0x17,
                0xFD, 0x34, 0x18,
                0xFD, 0x34, 0x19,
                0xFD, 0x34, 0x1A,
                0xFD, 0x34, 0x1B,
                0xFD, 0x34, 0x1C,
                0xFD, 0x34, 0x1D,
                0xFD, 0x34, 0x1E,
                
                0xFF,  // address 0x18
                0,     // address 0x19
                0x7F,  // address 0x1A
                0x0F,  // address 0x1B
                2,     // address 0x1C
                0xFE,  // address 0x1D
                0x0E,  // address 0x1E
                0      // address 0x1F 
        );
        setRegisterIY(1);
        
        stepAndCheckMemoryAndFlags(0, 0x18, FLAG_Z_H, FLAG_S_PV_N_C);
        stepAndCheckMemoryAndFlags(1, 0x19, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckMemoryAndFlags(0x80, 0x1A, FLAG_S_H_PV, FLAG_Z_N_C);
        stepAndCheckMemoryAndFlags(0x10, 0x1B, FLAG_H, FLAG_S_Z_PV_N_C);
        stepAndCheckMemoryAndFlags(3, 0x1C, -1, FLAG_S_Z_H_PV_N_C);
        stepAndCheckMemoryAndFlags(0xFF, 0x1D, FLAG_S, FLAG_Z_H_PV_N_C);
        stepAndCheckMemoryAndFlags(0x0F, 0x1E, -1, FLAG_S_Z_H_PV_N_C);

        setFlags(FLAG_C);
        stepAndCheckMemoryAndFlags(1, 0x1F, FLAG_C, FLAG_Z_H_PV_N);
    }

    @Test
    public void testDEC__IX_plus_d() {
        resetProgram(
                0xDD, 0x35, 0x14,
                0xDD, 0x35, 0x15,
                0xDD, 0x35, 0x16,
                0xDD, 0x35, 0x17,
                0xDD, 0x35, 0x18,
                0xDD, 0x35, 0x19,
                0xDD, 0x35, 0x1A,
                
                0,     // address 0x15
                1,     // address 0x16
                0x80,  // address 0x17
                0x10,  // address 0x18
                3,     // address 0x19
                0xFF,  // address 0x1A
                0x0F   // address 0x1B
        );
        setRegisterIX(1);
        
        stepAndCheckMemoryAndFlags(0xFF, 0x15, FLAG_S_N, FLAG_Z_H_PV_C);
        stepAndCheckMemoryAndFlags(0, 0x16, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckMemoryAndFlags(0x7F, 0x17, FLAG_PV_N, FLAG_S_Z_H_C);
        stepAndCheckMemoryAndFlags(0x0F, 0x18, FLAG_N, FLAG_S_Z_H_PV_C);
        stepAndCheckMemoryAndFlags(2, 0x19, FLAG_H_N, FLAG_S_Z_PV_C);
        stepAndCheckMemoryAndFlags(0xFE, 0x1A, FLAG_S_H_N, FLAG_Z_PV_C);
        stepAndCheckMemoryAndFlags(0x0E, 0x1B, FLAG_H_N, FLAG_S_Z_PV_C);
    }

    @Test
    public void testDEC__IY_plus_d() {
        resetProgram(
                0xFD, 0x35, 0x14,
                0xFD, 0x35, 0x15,
                0xFD, 0x35, 0x16,
                0xFD, 0x35, 0x17,
                0xFD, 0x35, 0x18,
                0xFD, 0x35, 0x19,
                0xFD, 0x35, 0x1A,
                
                0,     // address 0x15
                1,     // address 0x16
                0x80,  // address 0x17
                0x10,  // address 0x18
                3,     // address 0x19
                0xFF,  // address 0x1A
                0x0F   // address 0x1B
        );
        setRegisterIY(1);
        
        stepAndCheckMemoryAndFlags(0xFF, 0x15, FLAG_S_N, FLAG_Z_H_PV_C);
        stepAndCheckMemoryAndFlags(0, 0x16, FLAG_Z_H_N, FLAG_S_PV_C);
        stepAndCheckMemoryAndFlags(0x7F, 0x17, FLAG_PV_N, FLAG_S_Z_H_C);
        stepAndCheckMemoryAndFlags(0x0F, 0x18, FLAG_N, FLAG_S_Z_H_PV_C);
        stepAndCheckMemoryAndFlags(2, 0x19, FLAG_H_N, FLAG_S_Z_PV_C);
        stepAndCheckMemoryAndFlags(0xFE, 0x1A, FLAG_S_H_N, FLAG_Z_PV_C);
        stepAndCheckMemoryAndFlags(0x0E, 0x1B, FLAG_H_N, FLAG_S_Z_PV_C);
    }
    
    public void testADD_IX__ss() {
        
    }
    
    public void testADD_IY__ss() {
        
    }
    
    @Test
    public void testADC_HL__ss() throws Exception {
        resetProgram(0xED, 0x4A, 0xED, 0x5A, 0xED, 0x6A, 0xED, 0x7A);

    }
    
    @Test
    public void testSBC_HL__ss() throws Exception {

    }

}
