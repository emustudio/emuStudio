/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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

import org.junit.Ignore;
import org.junit.Test;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;

public class FlagsTableGeneratorTest {
    private final static int MAX_INDENT = 25;

    /**
     * Get H flag after DAA
     *
     * According to the document: "Undocumented Z80"
     *
     * @param nf N flag before operation
     * @param hf half-carry before operation
     * @param value value
     * @return 8-bit (H or 0)
     */
    public static int daa_hf(boolean nf, boolean hf, byte value) {
//        NF HF low  HF’
//        0  *  0-9  0
//        0  *  a-f  1
//        1  0  *    0
//        1  1  6-f  0
//        1  1  0-5  1
        int low = value & 0xF;
        if (!nf && low <= 9) {
            return 0;
        } else if (!nf && low >= 0xA && low <= 0xF) {
            return FLAG_H;
        } else if (nf && !hf) {
            return 0;
        } else if (nf && hf && low >= 6 && low <= 0xF) {
            return 0;
        } else if (nf && hf && low <= 5) {
            return FLAG_H;
        }
        throw new RuntimeException();
    }

    /**
     * Get result after DAA plus C flag
     *
     * According to the document: "Undocumented Z80"
     *
     * @param cf carry before operation
     * @param hf half-carry before operation
     * @param value value
     * @return 16 bit value -> higher 8-bit = flags (C or 0), lower 8-bit = result
     */
    public static int daa(boolean cf, boolean hf, byte value) {
        int high = (value >>> 4) & 0xF;
        int low = value & 0xF;

 //       cf high hf low  diff
 //       0  0-9  0  0-9  00
 //       0  0-9  1  0-9  06
 //       0  0-8  *  a-f  06
 //       0  a-f  0  0-9  60
 //       1  *    0  0-9  60
 //       1  *    1  0-9  66
 //       1  *    *  a-f  66
 //       0  9-f  *  a-f  66
 //       0  a-f  1  0-9  66

 //       CF high low  CF’
//        0  0-9  0-9  0
//        0  0-8  a-f  0
//        0  9-f  a-f  1
//        0  a-f  0-9  1
//        1  *    *    1

        if (!cf && !hf && high <= 9 && low <= 9) {
            return 0;
        } else if (!cf && hf && high <= 9 && low <= 9) {
            return 6;
        } else if (!cf && high <= 8 && low >= 0xA && low <= 0xF) {
            return 6;
        } else if (!cf && !hf && high >= 0xA && high <= 0xF && low <= 9) {
            return 0x60 | (FLAG_C << 8);
        } else if (cf && !hf && low <= 9) {
            return 0x60 | (FLAG_C << 8);
        } else if (cf && hf && low <= 9) {
            return 0x66 | (FLAG_C << 8);
        } else if (cf && low >= 0xA && low <= 0xF) {
            return 0x66 | (FLAG_C << 8);
        } else if (!cf && high >= 9 && high <= 0xF && low >= 0xA && low <= 0xF) {
            return 0x66 | (FLAG_C << 8);
        } else if (!cf && hf && high >= 0xA && high <=0xF && low <= 9) {
            return 0x66 | (FLAG_C << 8);
        }
        throw new RuntimeException();
    }

    private void generateDiffAndC(boolean cf, boolean hf) {
        int indent = 0;

        StringBuilder sb = new StringBuilder()
            .append("private final static int[] DAA_")
            .append(cf ? "C_" : "NOT_C_")
            .append(hf ? "H_" : "NOT_H_")
            .append("TABLE = new int[] {\n");

        for (int i = 0; i < 256; i++) {
            sb.append(daa(cf, hf, (byte)i) + ", ");

            if (indent++ > MAX_INDENT) {
                sb.append("\n");
                indent = 0;
            }
        }
        sb.append("\n};");
        System.out.println(sb.toString());
    }

    private void generateH(boolean nf, boolean hf) {
        int indent = 0;

        StringBuilder sb = new StringBuilder()
            .append("private final static int[] DAA_")
            .append(nf ? "N_" : "NOT_N_")
            .append(hf ? "H_" : "NOT_H_")
            .append("FOR_H_TABLE = new int[] {");
        for (int i = 0; i < 256; i++) {
            sb.append(daa_hf(nf, hf, (byte)i) + ", ");

            if (indent++ > MAX_INDENT) {
                sb.append("\n");
                indent = 0;
            }
        }
        sb.append("\n};");
        System.out.println(sb.toString());
    }

    @Test
    @Ignore
    public void testGenerateDAAtable() {
        generateDiffAndC(false, false);
        generateDiffAndC(false, true);
        generateDiffAndC(true, false);
        generateDiffAndC(true, true);

        generateH(false, false);
        generateH(false, true);
        generateH(true, false);
        generateH(true, true);
    }

}
