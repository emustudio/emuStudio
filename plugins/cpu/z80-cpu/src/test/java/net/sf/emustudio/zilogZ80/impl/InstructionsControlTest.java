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

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_L;
import org.junit.Test;

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
    
    public void testIM__0() {
        
    }
    
    public void testIM__1() {
        
    }
    
    public void testIM__2() {
        
    }
    
    public void testJR__e() {
        resetProgram(
                0x18, 4,
                0, // address 2
                0x38, 8,
                0, // address 5
                0x30, 0xC,
                0, // address 8
                0x28, 0x10,
                0, // address 0xB
                0x20, 0x14,
                0, // address 0xE
                0);

    }
    
    public void testJP__IX() {
        
    }
    
    public void testJP__IY() {
        
    }

    public void testDJNZ__e() {
        
    }
    
    public void testRETI() {
        
    }
    
    public void testRETN() {
        
    }
}
