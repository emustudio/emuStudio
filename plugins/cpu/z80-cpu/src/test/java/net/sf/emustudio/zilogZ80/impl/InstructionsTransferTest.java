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
package net.sf.emustudio.zilogZ80.impl;

import org.junit.Test;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.*;
import static org.junit.Assert.assertEquals;

public class InstructionsTransferTest extends InstructionsTest {

    private short[] generateLD_r__special_plus_d(int prefix, int base, short displacement, int... values) {
        int[] opcodes = new int[] {
                0x7E, 0x46, 0x4E, 0x56, 0x5E, 0x66, 0x6E
        };
        if (base < 3 * opcodes.length) {
            throw new IllegalArgumentException();
        }
        short[] result = new short[(base + displacement) +  values.length];
        int j = 0;
        for (int i = 0; i < opcodes.length; i++, j+=3) {
            result[j] = (short) prefix;
            result[j+1] = (short)opcodes[i];
            result[j+2] = (short)(displacement + i);
        }
        for (int i = 0; i < values.length; i++) {
            result[i + (base+displacement)] = (short)values[i];
        }
        return result;
    }

    @Test
    public void testLD_r__IX_plus_d() {
        int[] values = new int[] {1,2,3,4,5,6,7};

        resetProgram(generateLD_r__special_plus_d(0xDD, 22, (short) 2, values));
        cpu.getEngine().IX = 22;
        stepAndCheckRegisters(values);

        resetProgram(generateLD_r__special_plus_d(0xDD, 25, (short)-2, values));
        cpu.getEngine().IX = 25;
        stepAndCheckRegisters(values);
    }
    
    @Test
    public void testLD_r__IY_plus_d() {
        int[] values = new int[] {1,2,3,4,5,6,7};

        resetProgram(generateLD_r__special_plus_d(0xFD, 22, (short) 2, values));
        cpu.getEngine().IY = 22;
        stepAndCheckRegisters(values);

        resetProgram(generateLD_r__special_plus_d(0xFD, 25, (short)-2, values));
        cpu.getEngine().IY = 25;
        stepAndCheckRegisters(values);
    }

    @Test
    public void testLD_IX_plus_d__r() {
        int[] values = new int[] {1,2,3,4,5,6,7};
        resetProgram(
                0xDD, 0x77, 1,
                0xDD, 0x70, 2,
                0xDD, 0x71, 3,
                0xDD, 0x72, 4,
                0xDD, 0x73, 5,
                0xDD, 0x74, 6,
                0xDD, 0x75, 7,
                0,0,0,0,0,0,0,0,0
        );

        cpu.getEngine().IX = 22;
        setRegisters(values);

        stepAndCheckAndIncMemory(23, values);
    }

    @Test
    public void testLD_IY_plus_d__r() {
        int[] values = new int[] {1,2,3,4,5,6,7};
        resetProgram(
                0xFD, 0x77, 1,
                0xFD, 0x70, 2,
                0xFD, 0x71, 3,
                0xFD, 0x72, 4,
                0xFD, 0x73, 5,
                0xFD, 0x74, 6,
                0xFD, 0x75, 7,
                0,0,0,0,0,0,0,0,0
        );

        cpu.getEngine().IY = 22;
        setRegisters(values);

        stepAndCheckAndIncMemory(23, values);
    }

    @Test
    public void testLD_IX_plus_d__n() {
        int[] values = new int[] {1,2,3,4,5,6,7};
        resetProgram(
                0xDD, 0x36, 1, 1,
                0xDD, 0x36, 2, 2,
                0xDD, 0x36, 3, 3,
                0xDD, 0x36, 4, 4,
                0xDD, 0x36, 5, 5,
                0xDD, 0x36, 6, 6,
                0xDD, 0x36, 7, 7,
                0,0,0,0,0,0,0,0,0
        );

        cpu.getEngine().IX = 28;
        stepAndCheckAndIncMemory(29, values);
    }

    @Test
    public void testLD_IY_plus_d__n() {
        int[] values = new int[] {1,2,3,4,5,6,7};
        resetProgram(
                0xFD, 0x36, 1, 1,
                0xFD, 0x36, 2, 2,
                0xFD, 0x36, 3, 3,
                0xFD, 0x36, 4, 4,
                0xFD, 0x36, 5, 5,
                0xFD, 0x36, 6, 6,
                0xFD, 0x36, 7, 7,
                0,0,0,0,0,0,0,0,0
        );

        cpu.getEngine().IY = 28;
        stepAndCheckAndIncMemory(29, values);
    }

    @Test
    public void testLD_A__I() {
        resetProgram(0xED, 0x57, 0xED, 0x57);

        setRegisters(10);
        cpu.getEngine().I = 0xFF;
        cpu.getEngine().IFF[1] = true;

        setFlags(FLAG_H_N_C);
        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(FLAG_S_PV_C);
        checkNotFlags(FLAG_Z_H_N);

        cpu.getEngine().I = 0;
        cpu.getEngine().IFF[1] = false;

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkFlags(FLAG_Z_C);
        checkNotFlags(FLAG_S_H_PV_N);
    }

    @Test
    public void testLD_A__R() {
        resetProgram(0xED, 0x5F, 0xED, 0x5F);

        setRegisters(10);
        cpu.getEngine().R = 0xFE;
        cpu.getEngine().IFF[1] = true;

        setFlags(FLAG_H_N_C);
        stepAndCheck(0xFF, EmulatorEngine.REG_A);
        checkFlags(FLAG_S_PV_C);
        checkNotFlags(FLAG_Z_H_N);

        cpu.getEngine().IFF[1] = false;

        stepAndCheck(0, EmulatorEngine.REG_A);
        checkFlags(FLAG_Z_C);
        checkNotFlags(FLAG_S_H_PV_N);
    }

    @Test
    public void testLD_I__A() {
        resetProgram(0xED, 0x47);
        setRegisters(0x34);

        stepWithAssert();
        assertEquals(0x34, cpu.getEngine().I);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);
    }

    @Test
    public void testLD_R__A() {
        resetProgram(0xED, 0x4F);
        setRegisters(0x34);

        stepWithAssert();
        assertEquals(0x34, cpu.getEngine().R);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);
    }

    @Test
    public void testLD_IX__nn() {
        resetProgram(0xDD, 0x21, 0x12, 0x34);

        stepAndCheckIX(0x3412);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);
    }

    @Test
    public void testLD_IY__nn() {
        resetProgram(0xFD, 0x21, 0x12, 0x34);

        stepAndCheckIY(0x3412);
        checkNotFlags(FLAG_S_Z_H_PV_N_C);
    }

    @Test
    public void testLD_dd__mnn() {
        resetProgram(
                0xED, 0x4B, 0x11, 00,
                0xED, 0x5B, 0x11, 00,
                0xED, 0x6B, 0x11, 00,
                0xED, 0x7B, 0x11, 00,
                0,
                0x12,0x34 // address 0x11
        );

        stepWithAssert();
        checkRegister(REG_B, 0x34);
        checkRegister(REG_C, 0x12);

        stepWithAssert();
        checkRegister(REG_D, 0x34);
        checkRegister(REG_E, 0x12);

        stepWithAssert();
        checkRegister(REG_H, 0x34);
        checkRegister(REG_L, 0x12);

        stepWithAssert();
        assertEquals(0x3412, cpu.getEngine().SP);
    }

    @Test
    public void testLD_IX__mnn() {
        resetProgram(
                0xDD, 0x2A, 5, 00,
                0,
                0x12, 0x34 // address 0x5
        );

        stepAndCheckIX(0x3412);
    }

    @Test
    public void testLD_IY__mnn() {
        resetProgram(
                0xFD, 0x2A, 5, 00,
                0,
                0x12,0x34 // address 0x5
        );

        stepAndCheckIY(0x3412);
    }
    
    @Test
    public void testLD_mnn__dd() {
        resetProgram(
                0xED, 0x43, 0x11, 00,
                0xED, 0x53, 0x11, 00,
                0xED, 0x63, 0x11, 00,
                0xED, 0x73, 0x11, 00,
                0,
                0,0 // address 0x11
        );

        setRegisters(0, 0x12, 0x23, 0x34, 0x45, 0x56, 0x67);
        stepAndCheckMemory(0x23, 0x11);
        checkMemory(0x12, 0x12);

        stepAndCheckMemory(0x45, 0x11);
        checkMemory(0x34, 0x12);

        stepAndCheckMemory(0x67, 0x11);
        checkMemory(0x56, 0x12);

        cpu.getEngine().SP = 0x7889;
        stepAndCheckMemory(0x89, 0x11);
        checkMemory(0x78, 0x12);
    }

    @Test
    public void testLD_mnn__IX() {
        resetProgram(
                0xDD, 0x22, 5, 00,
                0,
                0, 0 // address 0x5
        );

        setRegisterIX(0x3412);
        stepAndCheckMemory(0x12, 5);
        checkMemory(0x34, 6);
    }

    @Test
    public void testLD_mnn__IY() {
        resetProgram(
                0xFD, 0x22, 5, 00,
                0,
                0, 0 // address 0x5
        );

        setRegisterIY(0x3412);
        stepAndCheckMemory(0x12, 5);
        checkMemory(0x34, 6);
    }

    @Test
    public void testLD_SP__IX() {
        resetProgram(0xDD, 0xF9);

        setRegisterIX(0x1234);
        stepWithAssert();

        assertEquals(0x1234, cpu.getEngine().SP);
    }

    @Test
    public void testLD_SP__IY() {
        resetProgram(0xFD, 0xF9);

        setRegisterIY(0x1234);
        stepWithAssert();

        assertEquals(0x1234, cpu.getEngine().SP);
    }

    @Test
    public void testEX_AF__AF2() {
        resetProgram(8);

        setRegisters(0x12);
        setFlags(0x34);
        cpu.getEngine().flags2 = 0xFF;
        cpu.getEngine().regs2[REG_A] = 0xFF;

        stepAndCheckAccAndFlags(0xFF, 0xFF, 0);
        assertEquals(0x34, cpu.getEngine().flags2);
        assertEquals(0x12, cpu.getEngine().regs2[REG_A]);
    }

    @Test
    public void testEXX() {
        resetProgram(0xD9);

        setRegisters(0, 0x12, 0x34, 0x45, 0x56, 0x67, 0x78);

        cpu.getEngine().regs2[REG_B] = 0x21;
        cpu.getEngine().regs2[REG_C] = 0x32;
        cpu.getEngine().regs2[REG_D] = 0x43;
        cpu.getEngine().regs2[REG_E] = 0x54;
        cpu.getEngine().regs2[REG_H] = 0x65;
        cpu.getEngine().regs2[REG_L] = 0x76;

        stepAndCheck(0x21, REG_B);
        checkRegister(REG_C, 0x32);
        checkRegister(REG_D, 0x43);
        checkRegister(REG_E, 0x54);
        checkRegister(REG_H, 0x65);
        checkRegister(REG_L, 0x76);

        assertEquals(0x12, cpu.getEngine().regs2[REG_B]);
        assertEquals(0x34, cpu.getEngine().regs2[REG_C]);
        assertEquals(0x45, cpu.getEngine().regs2[REG_D]);
        assertEquals(0x56, cpu.getEngine().regs2[REG_E]);
        assertEquals(0x67, cpu.getEngine().regs2[REG_H]);
        assertEquals(0x78, cpu.getEngine().regs2[REG_L]);
    }

    @Test
    public void testEX_mSP__IX() {
        resetProgram(
                0xDD, 0xE3, 0,
                0x34, 0x12 // address 3
        );

        cpu.getEngine().SP = 3;
        setRegisterIX(0x5678);

        stepAndCheckIX(0x1234);

        checkMemory(0x78, 3);
        checkMemory(0x56, 4);
    }

    @Test
    public void testEX_mSP__IY() {
        resetProgram(
                0xFD, 0xE3, 0,
                0x34, 0x12 // address 3
        );

        cpu.getEngine().SP = 3;
        setRegisterIY(0x5678);

        stepAndCheckIY(0x1234);

        checkMemory(0x78, 3);
        checkMemory(0x56, 4);
    }

    @Test
    public void testLDI() {
        resetProgram(0xED, 0xA0, 0xED, 0xA0,
                0,
                0x12, 0x34, 0x56, 0x78
        );

        setRegisters(0, 0, 2, 0, 5, 0, 7);

        stepAndCheckMemory(0x56, 5);
        checkFlags(FLAG_PV);
        setFlags(FLAG_H_N);
        stepAndCheckMemory(0x78, 6);
        checkRegister(REG_B, 0);
        checkRegister(REG_C, 0);
        checkNotFlags(FLAG_H_PV_N);
    }

    @Test
    public void testLDIR() {
        resetProgram(
                0xED, 0xB0,
                0,
                0x12, 0x34, 0x56, 0x78
        );

        setRegisters(0, 0, 2, 0, 3, 0, 5);

        setFlags(FLAG_H_PV_N);
        stepAndCheckMemory(0x56, 3);
        setFlags(FLAG_S_Z_H_PV_N_C);
        stepAndCheckMemory(0x78, 4);
        checkRegister(REG_B, 0);
        checkRegister(REG_C, 0);
        checkFlags(FLAG_S_Z_C);
        checkNotFlags(FLAG_H_PV_N);
    }

    @Test
    public void testLDD() {
        resetProgram(0xED, 0xA8, 0xED, 0xA8,
                0,
                0x12, 0x34, 0x56, 0x78
        );

        setRegisters(0, 0, 2, 0, 6, 0, 8);

        stepAndCheckMemory(0x78, 6);
        checkFlags(FLAG_PV);
        setFlags(FLAG_H_N);
        stepAndCheckMemory(0x56, 5);
        checkRegister(REG_B, 0);
        checkRegister(REG_C, 0);
        checkNotFlags(FLAG_H_PV_N);
    }

    @Test
    public void testLDDR() {
        resetProgram(0xED, 0xB8,
                0,
                0x12, 0x34, 0x56, 0x78
        );

        setRegisters(0, 0, 2, 0, 4, 0, 6);

        stepAndCheckMemory(0x78, 4);
        setFlags(FLAG_S_Z_H_PV_N_C);
        stepAndCheckMemory(0x56, 3);
        checkRegister(REG_B, 0);
        checkRegister(REG_C, 0);
        checkFlags(FLAG_S_Z_C);
        checkNotFlags(FLAG_H_PV_N);
    }

}
