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
public class InstructionsLogicTest extends InstructionsTest{


    @Test
    public void testCPIR() {
        resetProgram(
                0xED, 0xB1,
                0,
                127 // address 3
        );

        setRegisters(129, 0, 2, 0, 0, 0, 3);
        setFlags(FLAG_C);
        stepAndCheckAccAndFlags(129, FLAG_PV_N_C, FLAG_S_Z_H);
        checkRegister(REG_H, 0);
        checkRegister(REG_L, 4);
        checkRegister(REG_B, 0);
        checkRegister(REG_C, 1);
        assertEquals(0, cpu.getEngine().PC);
    }

    @Test
    public void testCPD() {
        resetProgram(
                0xED, 0xA9,
                0,
                127 // address 3
        );

        setRegisters(129, 0, 1, 0, 0, 0, 3);
        setFlags(FLAG_PV_C);
        stepAndCheckAccAndFlags(129, FLAG_N_C, FLAG_S_Z_H_PV);
        checkRegister(REG_H, 0);
        checkRegister(REG_L, 2);
        checkRegister(REG_B, 0);
        checkRegister(REG_C, 0);
    }

    @Test
    public void testCPDR() {
        resetProgram(
                0xED, 0xB9,
                0,
                127 // address 3
        );

        setRegisters(129, 0, 2, 0, 0, 0, 3);
        setFlags(FLAG_C);
        stepAndCheckAccAndFlags(129, FLAG_PV_N_C, FLAG_S_Z_H);
        checkRegister(REG_H, 0);
        checkRegister(REG_L, 2);
        checkRegister(REG_B, 0);
        checkRegister(REG_C, 1);
        assertEquals(0, cpu.getEngine().PC);
    }

    @Test
    public void testAND__IX_plus_d() {
        resetProgram(
                0xDD, 0xA6,
                1,
                0,0,
                0xC5
        );

        setRegisterIX(4);
        setFlags(FLAG_C | FLAG_N);
        setRegisters(0xC5);

        stepAndCheckAccAndFlags(0xC5, FLAG_S_H_PV, FLAG_Z_N_C);
    }

    @Test
    public void testAND__IY_plus_d() {
        resetProgram(
                0xFD, 0xA6,
                1,
                0,0,
                0xC5
        );

        setRegisterIY(4);
        setFlags(FLAG_C | FLAG_N);
        setRegisters(0xC5);

        stepAndCheckAccAndFlags(0xC5, FLAG_S_H_PV, FLAG_Z_N_C);
    }

    public void testOR__IX_plus_d() {
        
    }
    
    public void testOR__IY_plus_d() {
        
    }
    
    public void testXOR__IX_plus_d() {
        
    }
    
    public void testXOR__IY_plus_d() {
        
    }
    
    public void testCP__IX_plus_d() {
        
    }
    
    public void testCP__IY_plus_d() {
        
    }
    
    public void testNEG() {
        
    }
    
    public void testRLC__r() {
        
    }
    
    public void testRLC__mHL() {
        
    }
    
    public void testRLC__IX_plus_d() {
        
    }
    
    public void testRLC__IY_plus_d() {
        
    }
    
    public void testRL__r() {
        
    }
    
    public void testRL__mHL() {
        
    }
    
    public void testRL__IX_plus_d() {
        
    }
    
    public void testRL__IY_plus_d() {
        
    }
    
    public void testRRC__r() {
        
    }
    
    public void testRRC__mHL() {
        
    }
    
    public void testRRC__IX_plus_d() {
        
    }
    
    public void testRRC__IY_plus_d() {
        
    }
    
    public void testRR__r() {
        
    }
    
    public void testRR__mHL() {
        
    }
    
    public void testRR__IX_plus_d() {
        
    }
    
    public void testRR__IY_plus_d() {
        
    }
    
    public void testSLA__r() {
        
    }
    
    public void testSLA__mHL() {
        
    }
    
    public void testSLA__IX_plus_d() {
        
    }
    
    public void testSLA__IY_plus_d() {
        
    }

    public void testSRA__r() {
        
    }
    
    public void testSRA__mHL() {
        
    }
    
    public void testSRA__IX_plus_d() {
        
    }
    
    public void testSRA__IY_plus_d() {
        
    }
    
    public void testSRL__r() {
        
    }
    
    public void testSRL__mHL() {
        
    }
    
    public void testSRL__IX_plus_d() {
        
    }
    
    public void testSRL__IY_plus_d() {
        
    }
    
    public void testRLD() {
        
    }
    
    public void testRRD() {
        
    }
    
}

