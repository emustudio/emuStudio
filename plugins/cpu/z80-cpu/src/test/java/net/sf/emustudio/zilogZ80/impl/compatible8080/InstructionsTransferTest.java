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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstructionsTransferTest extends InstructionsTest {
    private int[] defaultValues;

    private short[] generateLD_r__n(int... values) {
        int[] opcodes = new int[] {
                0x3E, 0x06, 0x0E, 0x16, 0x1E, 0x26, 0x2E
        };
        short[] result = new short[values.length * 2];
        for (int i = 0, j = 0; i < values.length; i++, j+=2) {
            result[j] = (short)opcodes[i];
            result[j+1] = (short)values[i];
        }
        return result;
    }
    
    @Before
    public void setupValues() {
        defaultValues = new int[] { 1,2,3,4,5,6,7 };
    }

    @Test
    public void testMemoryOverflow() throws Exception {
        resetProgram(new short[] {});

        cpu.step();
        checkRunState(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT);
    }

    @Test
    public void testLD_r__n() throws Exception {
        resetProgram(generateLD_r__n(defaultValues));
        stepAndCheckRegisters(defaultValues);
    }

    @Test
    public void testLD_mHL__n() throws Exception {
        resetProgram(
                0x36, 0xAB, 0, 0, 0,
                0xFF //address 5
        );

        setRegister(EmulatorEngine.REG_L, 5);
        stepAndCheckMemory(0xAB, 5);
    }

    @Test
    public void testLD_A__r() throws Exception {
        resetProgram(0x7F, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D);
        setRegisters(defaultValues);

        stepAndCheckRegister(EmulatorEngine.REG_A, defaultValues);
    }

    @Test
    public void testLD_B__r() throws Exception {
        resetProgram(0x47, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_B, 1,1,3,4,5,6,7);
    }

    @Test
    public void testLD_C__r() throws Exception {
        resetProgram(0x4F, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_C, 1,2,2,4,5,6,7);
    }

    @Test
    public void testLD_D__r() throws Exception {
        resetProgram(0x57, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_D, 1,2,3,3,5,6,7);
    }

    @Test
    public void testLD_E__r() throws Exception {
        resetProgram(0x5F, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_E, 1,2,3,4,4,6,7);
    }

    @Test
    public void testLD_H__r() throws Exception {
        resetProgram(0x67, 0x60, 0x61, 0x62, 0x63, 0x64, 0x65);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_H, 1,2,3,4,5,5,7);
    }

    @Test
    public void testLD_L__r() throws Exception {
        resetProgram(0x6F, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D);

        setRegisters(defaultValues);
        stepAndCheckRegister(EmulatorEngine.REG_L, 1,2,3,4,5,6,6);
    }

    @Test
    public void testLD_r__mHL() throws Exception {
        resetProgram(
                0x7E, 0x46, 0x4E, 0x56, 0x5E, 0x66, 0x6E,0,
                0xFF // address 8
        );

        setRegisters(0,0,0,0,0,0,8);
        stepAndCheckRegisters(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF);
        setRegister(EmulatorEngine.REG_H, 0); // hack
        stepAndCheck(0xFF, EmulatorEngine.REG_L);
    }

    @Test
    public void testLD_mHL__r() throws Exception {
        resetProgram(
                0x77, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75,0,
                0xFF // address 8
        );

        setRegisters(1,2,3,4,5,0,8);
        stepAndCheckMemory(8, 1,2,3,4,5,0,8);
    }

    @Test
    public void testLD_A__dd() throws Exception {
        resetProgram(
                0x0A, 0x1A, 0,
                0xFF // address 3
        );

        setRegisters(0,0,3,0,3,0,0);
        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        setRegister(EmulatorEngine.REG_A,  0); // hack
        stepAndCheck(0xFF, EmulatorEngine.REG_A);
    }

    @Test
    public void testLD_A__nn() throws Exception {
        resetProgram(
                0x3A, 5, 0, 0, 0,
                0xFF //address 5
        );

        stepAndCheck(0xFF, EmulatorEngine.REG_A);
    }
    
    @Test
    public void testLD_dd__A() throws Exception {
        resetProgram(
                0x02, 0x12, 0,
                0, // address 3
                0  // address 4
        );

        setRegisters(0xFF,0,3,0,4,0,0);
        stepAndCheckMemory(0xFF, 3);
        stepAndCheckMemory(0xFF, 4);
    }

    @Test
    public void testLD_nn__A() throws Exception {
        resetProgram(
                0x32, 5, 0, 0, 0,
                0xFF //address 5
        );

        setRegisters(0xAB);
        stepAndCheckMemory(0xAB, 5);
    }

    @Test
    public void testLD_BC__nn() throws Exception {
        resetProgram(
                0x01, 0x12, 0x34
        );
        stepAndCheck(0x12, EmulatorEngine.REG_C);
        checkRegister(EmulatorEngine.REG_B, 0x34);
    }

    @Test
    public void testLD_DE__nn() throws Exception {
        resetProgram(
                0x11, 0x12, 0x34
        );
        stepAndCheck(0x12, EmulatorEngine.REG_E);
        checkRegister(EmulatorEngine.REG_D, 0x34);
    }

    @Test
    public void testLD_HL__nn() throws Exception {
        resetProgram(
                0x21, 0x12, 0x34
        );
        stepAndCheck(0x12, EmulatorEngine.REG_L);
        checkRegister(EmulatorEngine.REG_H, 0x34);
    }

    @Test
    public void testLD_SP__nn() throws Exception {
        resetProgram(
                0x31, 0x12, 0x34
        );
        cpu.step();
        assertEquals(0x3412, cpu.getEngine().SP);
    }
    
    @Test
    public void testLD_mHL__nn() throws Exception {
        resetProgram(
                0x2A, 5, 0, 0, 0,
                0xAB, //address 5
                0xCD  //address 6
        );

        stepAndCheck(0xCD, EmulatorEngine.REG_H);
        stepAndCheck(0xAB, EmulatorEngine.REG_L);
    }

    @Test
    public void testLD_mnn__HL() throws Exception {
        resetProgram(
                0x22, 5, 0, 0, 0,
                0xFF, //address 5
                0xFF  //address 6
        );

        setRegister(EmulatorEngine.REG_H, 0xCD);
        setRegister(EmulatorEngine.REG_L, 0xAB);
        stepAndCheckMemory(0xAB, 5);
        stepAndCheckMemory(0xCD, 6);
    }


    @Test
    public void testLD_SP__HL() throws Exception {
        resetProgram(0xF9);

        setRegister(EmulatorEngine.REG_H, 0x12);
        setRegister(EmulatorEngine.REG_L, 0x34);

        cpu.step();
        assertEquals(0x1234, cpu.getEngine().SP);
    }

    @Test
    public void testEX_DE__HL() throws Exception {
        resetProgram(0xEB);

        setRegister(EmulatorEngine.REG_H, 0x12);
        setRegister(EmulatorEngine.REG_L, 0x34);

        stepAndCheck(0x12, EmulatorEngine.REG_D);
        checkRegister(EmulatorEngine.REG_E, 0x34);

        checkRegister(EmulatorEngine.REG_H, 0);
        checkRegister(EmulatorEngine.REG_L, 0);
    }

    @Test
    public void testEX_mSP__HL() throws Exception {
        resetProgram(
                0xE3, 0,
                0x34, // address 2
                0x12  // address 3
        );

        cpu.getEngine().SP = 2;

        stepAndCheck(0x12, EmulatorEngine.REG_H);
        checkRegister(EmulatorEngine.REG_L, 0x34);

        checkMemory(0, 2);
        checkMemory(0, 3);
    }

}
