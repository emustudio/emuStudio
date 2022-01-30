/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.cpu.zilogZ80;

class EmulatorTables {
    private final static int FLAG_S_SHIFT = 7;
    private final static int FLAG_Z_SHIFT = 6;
    private final static int FLAG_H_SHIFT = 4;
    private final static int FLAG_PV_SHIFT = 2;
    private final static int FLAG_N_SHIFT = 1;
    private final static int FLAG_C_SHIFT = 0;

    // INDEX IS:
    //  - for ADD: A+B
    //  - for ADC: A+B+carry
    //  - for SUB: A+(~B + 1)
    //  - for SBC: A+(~B + 1)-carry
    //
    // carryIns = *acc ^ a ^ b;
    // carryOut = 1 IFF (a + b > 0xFF) or,
    //   equivalently, but avoiding overflow in C: (a > 0xFF - b).
    // overflowOut = (carryIns >> 7) ^ carryOut;
    // halfCarryOut = (carryIns >> 4) & 1;
    // zeroOut = sum == 0
    // signOut = sum & 0x80
//    final static byte[] SZHPC_TABLE = new byte[511];
//
//    public static String intToFlags(int flags) {
//        String flagsString = "";
//        if ((flags & FLAG_S) == FLAG_S) {
//            flagsString += "S";
//        }
//        if ((flags & FLAG_Z) == FLAG_Z) {
//            flagsString += "Z";
//        }
//        if ((flags & FLAG_H) == FLAG_H) {
//            flagsString += "H";
//        }
//        if ((flags & FLAG_PV) == FLAG_PV) {
//            flagsString += "P";
//        }
//        if ((flags & FLAG_N) == FLAG_N) {
//            flagsString += "N";
//        }
//        if ((flags & FLAG_C) == FLAG_C) {
//            flagsString += "C";
//        }
//        return flagsString;
//    }
//
//    static {
//        for (int a = 0; a < 256; a++) {
//            for (int b = 0; b < 256; b++) {
//                int sum = (a + b) & 0xFF; // index to the array
//
//                int carryOut = (a > 0xFF - b) ? FLAG_C : 0;
//                int carryIns = sum ^ a ^ b;
//                int halfCarryOut = (carryIns >> 4) & 1;
//                int overflowOut = (((carryIns >> 7) ^ carryOut) != 0) ? FLAG_PV : 0;
//
//                int result = carryOut | (halfCarryOut << FLAG_H_SHIFT) | (overflowOut) | (sum == 0 ? FLAG_Z : 0) | (sum & 0x80);
//
//                if (a == 0x65 && b == 0x1C) {
//                    System.out.println("TABLE  = " + intToFlags(result));
//                }
//                if (sum == (0x65 + 0x1C)) {
//                    System.out.println("TABLE(SUM)  = " + intToFlags(result));
//                    System.out.println("  a = " + Integer.toHexString(a));
//                    System.out.println("  b = " + Integer.toHexString(b));
//                }
//
//                SZHPC_TABLE[(a + b) & 0x1FF] = (byte)result;
//            }
//        }
//    }

    final static short[] SIGN_ZERO_TABLE = new short[]{
        64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128
    };

    final static short[] PARITY_TABLE = {
        4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4,
        0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0,
        0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0,
        4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4, 4, 0, 0, 4, 0, 4, 4, 0, 4, 0, 0, 4, 0, 4, 4, 0, 0, 4, 4, 0, 4, 0, 0, 4
    };

    final static short[] INC_TABLE = new short[]{
        80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 148, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 144, 128, 128, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 144, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 144, 128, 128, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 144, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 144, 128,
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 144, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128,
        144, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128
    };

    final static short[] DEC_TABLE = new short[]{
        82, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 2, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
        18, 2, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 2, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
        18, 18, 18, 2, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 2, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
        18, 18, 18, 18, 18, 2, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 2, 18, 18, 18, 18, 18, 18, 18, 18,
        18, 18, 18, 18, 18, 18, 18, 6, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 130, 146, 146, 146, 146, 146, 146,
        146, 146, 146, 146, 146, 146, 146, 146, 146, 130, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 130, 146, 146, 146, 146,
        146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 130, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 130, 146, 146,
        146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 130, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 130,
        146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 146, 130
    };

    final static int[] DAA_NOT_C_NOT_H_TABLE = new int[]{
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6,
        6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6,
        6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 358, 358, 358, 358, 358, 358, 352, 352,
        352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358,
        358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352,
        352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352,
        352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358,
    };

    final static int[] DAA_NOT_C_H_TABLE = new int[]{
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
    };

    final static int[] DAA_C_NOT_H_TABLE = new int[]{
        352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358,
        358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352,
        352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352,
        352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358,
        358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352,
        352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352,
        352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358,
        358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352,
        352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352, 352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358, 352, 352, 352,
        352, 352, 352, 352, 352, 352, 352, 358, 358, 358, 358, 358, 358,
    };

    final static int[] DAA_C_H_TABLE = new int[]{
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
        358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358, 358,
    };

    final static int[] DAA_NOT_N_NOT_H_FOR_H_TABLE = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16,
        16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16,
        16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16,
        16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16,
    };

    final static int[] DAA_NOT_N_H_FOR_H_TABLE = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16,
        16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16,
        16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16,
        16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16,
    };

    final static int[] DAA_N_NOT_H_FOR_H_TABLE = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    final static int[] DAA_N_H_FOR_H_TABLE = new int[]{16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16,
        16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16,
        16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 16, 16,
        16, 16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    final static int[] AND_OR_XOR_TABLE = new int[]{
        84, 16, 16, 20, 16, 20, 20, 16, 16, 20, 20, 16, 20, 16, 16, 20, 16, 20, 20, 16, 20, 16, 16, 20, 20, 16, 16, 20, 16, 20,
        20, 16, 16, 20, 20, 16, 20, 16, 16, 20, 20, 16, 16, 20, 16, 20, 20, 16, 20, 16, 16, 20, 16, 20, 20, 16, 16, 20, 20, 16,
        20, 16, 16, 20, 16, 20, 20, 16, 20, 16, 16, 20, 20, 16, 16, 20, 16, 20, 20, 16, 20, 16, 16, 20, 16, 20, 20, 16, 16, 20,
        20, 16, 20, 16, 16, 20, 20, 16, 16, 20, 16, 20, 20, 16, 16, 20, 20, 16, 20, 16, 16, 20, 16, 20, 20, 16, 20, 16, 16, 20,
        20, 16, 16, 20, 16, 20, 20, 16, 144, 148, 148, 144, 148, 144, 144, 148, 148, 144, 144, 148, 144, 148, 148, 144, 148, 144, 144, 148, 144, 148,
        148, 144, 144, 148, 148, 144, 148, 144, 144, 148, 148, 144, 144, 148, 144, 148, 148, 144, 144, 148, 148, 144, 148, 144, 144, 148, 144, 148, 148, 144,
        148, 144, 144, 148, 148, 144, 144, 148, 144, 148, 148, 144, 148, 144, 144, 148, 144, 148, 148, 144, 144, 148, 148, 144, 148, 144, 144, 148, 144, 148,
        148, 144, 148, 144, 144, 148, 148, 144, 144, 148, 144, 148, 148, 144, 144, 148, 148, 144, 148, 144, 144, 148, 148, 144, 144, 148, 144, 148, 148, 144,
        148, 144, 144, 148, 144, 148, 148, 144, 144, 148, 148, 144, 148, 144, 144, 148,};

    static final int[] NEG_TABLE = {
        66, 65411, 65155, 64899, 64643, 64387, 64131, 63875, 63619, 63363, 63107, 62851, 62595, 62339, 62083, 61827, 61571, 61315, 61059, 60803, 60547, 60291,
        60035, 59779, 59523, 59267, 59011, 58755, 58499, 58243, 57987, 57731, 57475, 57219, 56963, 56707, 56451, 56195, 55939, 55683, 55427, 55171, 54915,
        54659, 54403, 54147, 53891, 53635, 53379, 53123, 52867, 52611, 52355, 52099, 51843, 51587, 51331, 51075, 50819, 50563, 50307, 50051, 49795, 49539,
        49283, 49027, 48771, 48515, 48259, 48003, 47747, 47491, 47235, 46979, 46723, 46467, 46211, 45955, 45699, 45443, 45187, 44931, 44675, 44419, 44163,
        43907, 43651, 43395, 43139, 42883, 42627, 42371, 42115, 41859, 41603, 41347, 41091, 40835, 40579, 40323, 40067, 39811, 39555, 39299, 39043, 38787,
        38531, 38275, 38019, 37763, 37507, 37251, 36995, 36739, 36483, 36227, 35971, 35715, 35459, 35203, 34947, 34691, 34435, 34179, 33923, 33667, 33411,
        33155, 32903, 32515, 32259, 32003, 31747, 31491, 31235, 30979, 30723, 30467, 30211, 29955, 29699, 29443, 29187, 28931, 28675, 28419, 28163, 27907,
        27651, 27395, 27139, 26883, 26627, 26371, 26115, 25859, 25603, 25347, 25091, 24835, 24579, 24323, 24067, 23811, 23555, 23299, 23043, 22787, 22531,
        22275, 22019, 21763, 21507, 21251, 20995, 20739, 20483, 20227, 19971, 19715, 19459, 19203, 18947, 18691, 18435, 18179, 17923, 17667, 17411, 17155,
        16899, 16643, 16387, 16131, 15875, 15619, 15363, 15107, 14851, 14595, 14339, 14083, 13827, 13571, 13315, 13059, 12803, 12547, 12291, 12035, 11779,
        11523, 11267, 11011, 10755, 10499, 10243, 9987, 9731, 9475, 9219, 8963, 8707, 8451, 8195, 7939, 7683, 7427, 7171, 6915, 6659, 6403, 6147, 5891,
        5635, 5379, 5123, 4867, 4611, 4355, 4099, 3843, 3587, 3331, 3075, 2819, 2563, 2307, 2051, 1795, 1539, 1283, 1027, 771, 515, 259
    };

    // rrcaTable[i] = ((i & 1) << 7)|(i>>1);
    final static short[] RRCA_TABLE = {
        0, 128, 1, 129, 2, 130, 3, 131, 4, 132, 5, 133, 6, 134, 7, 135, 8, 136, 9, 137, 10, 138, 11, 139, 12, 140, 13, 141, 14, 142, 15, 143, 16, 144, 17, 145, 18, 146, 19, 147, 20, 148, 21, 149, 22, 150,
        23, 151, 24, 152, 25, 153, 26, 154, 27, 155, 28, 156, 29, 157, 30, 158, 31, 159, 32, 160, 33, 161, 34, 162, 35, 163, 36, 164, 37, 165, 38, 166, 39, 167, 40, 168, 41, 169, 42, 170, 43, 171, 44, 172, 45,
        173, 46, 174, 47, 175, 48, 176, 49, 177, 50, 178, 51, 179, 52, 180, 53, 181, 54, 182, 55, 183, 56, 184, 57, 185, 58, 186, 59, 187, 60, 188, 61, 189, 62, 190, 63, 191, 64, 192, 65, 193, 66, 194, 67, 195,
        68, 196, 69, 197, 70, 198, 71, 199, 72, 200, 73, 201, 74, 202, 75, 203, 76, 204, 77, 205, 78, 206, 79, 207, 80, 208, 81, 209, 82, 210, 83, 211, 84, 212, 85, 213, 86, 214, 87, 215, 88, 216, 89, 217, 90,
        218, 91, 219, 92, 220, 93, 221, 94, 222, 95, 223, 96, 224, 97, 225, 98, 226, 99, 227, 100, 228, 101, 229, 102, 230, 103, 231, 104, 232, 105, 233, 106, 234, 107, 235, 108, 236, 109, 237, 110, 238, 111, 239, 112, 240,
        113, 241, 114, 242, 115, 243, 116, 244, 117, 245, 118, 246, 119, 247, 120, 248, 121, 249, 122, 250, 123, 251, 124, 252, 125, 253, 126, 254, 127, 255
    };
}
