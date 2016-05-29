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
package net.sf.emustudio.zilogZ80.assembler.treeAbstract;

import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;

public abstract class Expression {

    protected int value;

    public int getValue() {
        return value;
    }

    public static int getSize(int val) {
        if (val <= 255 && val >= -128) {
            return 1;
        } else if (val <= 65535 && val >= -32768) {
            return 2;
        } else if (val <= 16777215 && val >= -8388608) {
            return 3;
        }
        return 4;
    }

    public abstract int eval(Namespace env, int curr_addr) throws Exception;

    static String encodeValue(int val, int neededSize) {

        int size = getSize(val);
        if (size < neededSize) {
            size = neededSize;
        }
        String s;

        if (size == 1) {
            s = String.format("%02X", (val & 0xFF));
        } else if (size == 2) {
            s = String.format("%02X%02X", (val & 0xFF), ((val >> 8) & 0xFF));
        } else if (size == 3) {
            s = String.format("%02X%02X%02X",
                    (val & 0xFF), ((val >> 8) & 0xFF), ((val >> 16) & 0xFF));
        } else {
            s = String.format("%02X%02X%02X%02X",
                    (val & 0xFF), ((val >> 8) & 0xFF), ((val >> 16) & 0xFF),
                    ((val >> 24) & 0xFF));
        }
        return s;
    }

    public static int reverseBytes(int val, int neededSize) {
        int i = 0;
        int size = getSize(val);
        for (int j = 0; j < size; j++) {
            i += (val & 0xFF);
            val >>= 8;
            i <<= 8;
        }
        for (int j = size; j < neededSize; j++) {
            i <<= 8;
        }
        return i >>> 8;
    }

    public String encodeValue(int neededSize) {
        return encodeValue(value, neededSize);
    }
}
