/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2020, Peter Jakubčo
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
package net.sf.emustudio.devices.adm3a.impl;

import java.util.Arrays;
import java.util.Objects;

import static net.sf.emustudio.devices.adm3a.impl.LoadCursorPosition.ExpectedSequence.*;

class LoadCursorPosition {
    private static final int[] ESC_CURSOR_CODES = new int[256];

    static {
        Arrays.fill(ESC_CURSOR_CODES, -1);

        ESC_CURSOR_CODES[' '] = 0;
        ESC_CURSOR_CODES['!'] = 1;
        ESC_CURSOR_CODES['\"'] = 2;
        ESC_CURSOR_CODES['#'] = 3;
        ESC_CURSOR_CODES['$'] = 4;
        ESC_CURSOR_CODES['%'] = 5;
        ESC_CURSOR_CODES['&'] = 6;
        ESC_CURSOR_CODES['\''] = 7;
        ESC_CURSOR_CODES['('] = 8;
        ESC_CURSOR_CODES[')'] = 9;
        ESC_CURSOR_CODES['*'] = 10;
        ESC_CURSOR_CODES['+'] = 11;
        ESC_CURSOR_CODES[','] = 12;
        ESC_CURSOR_CODES['-'] = 13;
        ESC_CURSOR_CODES['.'] = 14;
        ESC_CURSOR_CODES['/'] = 15;
        ESC_CURSOR_CODES['0'] = 16;
        ESC_CURSOR_CODES['1'] = 17;
        ESC_CURSOR_CODES['2'] = 18;
        ESC_CURSOR_CODES['3'] = 19;
        ESC_CURSOR_CODES['4'] = 20;
        ESC_CURSOR_CODES['5'] = 21;
        ESC_CURSOR_CODES['6'] = 22;
        ESC_CURSOR_CODES['7'] = 23;
        ESC_CURSOR_CODES['8'] = 24;
        ESC_CURSOR_CODES['9'] = 25;
        ESC_CURSOR_CODES[':'] = 26;
        ESC_CURSOR_CODES[';'] = 27;
        ESC_CURSOR_CODES['<'] = 28;
        ESC_CURSOR_CODES['='] = 29;
        ESC_CURSOR_CODES['>'] = 30;
        ESC_CURSOR_CODES['?'] = 31;
        ESC_CURSOR_CODES['@'] = 32;
        ESC_CURSOR_CODES['A'] = 33;
        ESC_CURSOR_CODES['B'] = 34;
        ESC_CURSOR_CODES['C'] = 35;
        ESC_CURSOR_CODES['D'] = 36;
        ESC_CURSOR_CODES['E'] = 37;
        ESC_CURSOR_CODES['F'] = 38;
        ESC_CURSOR_CODES['G'] = 39;
        ESC_CURSOR_CODES['H'] = 40;
        ESC_CURSOR_CODES['I'] = 41;
        ESC_CURSOR_CODES['J'] = 42;
        ESC_CURSOR_CODES['K'] = 43;
        ESC_CURSOR_CODES['L'] = 44;
        ESC_CURSOR_CODES['M'] = 45;
        ESC_CURSOR_CODES['N'] = 46;
        ESC_CURSOR_CODES['O'] = 47;
        ESC_CURSOR_CODES['P'] = 48;
        ESC_CURSOR_CODES['Q'] = 49;
        ESC_CURSOR_CODES['R'] = 50;
        ESC_CURSOR_CODES['S'] = 51;
        ESC_CURSOR_CODES['T'] = 52;
        ESC_CURSOR_CODES['U'] = 53;
        ESC_CURSOR_CODES['V'] = 54;
        ESC_CURSOR_CODES['W'] = 55;
        ESC_CURSOR_CODES['X'] = 56;
        ESC_CURSOR_CODES['Y'] = 57;
        ESC_CURSOR_CODES['Z'] = 58;
        ESC_CURSOR_CODES['['] = 59;
        ESC_CURSOR_CODES['\\'] = 60;
        ESC_CURSOR_CODES[']'] = 61;
        ESC_CURSOR_CODES['^'] = 62;
        ESC_CURSOR_CODES['_'] = 63;
        ESC_CURSOR_CODES['`'] = 64;
        ESC_CURSOR_CODES['a'] = 65;
        ESC_CURSOR_CODES['b'] = 66;
        ESC_CURSOR_CODES['c'] = 67;
        ESC_CURSOR_CODES['d'] = 68;
        ESC_CURSOR_CODES['e'] = 69;
        ESC_CURSOR_CODES['f'] = 70;
        ESC_CURSOR_CODES['g'] = 71;
        ESC_CURSOR_CODES['h'] = 72;
        ESC_CURSOR_CODES['i'] = 73;
        ESC_CURSOR_CODES['j'] = 74;
        ESC_CURSOR_CODES['k'] = 75;
        ESC_CURSOR_CODES['l'] = 76;
        ESC_CURSOR_CODES['m'] = 77;
        ESC_CURSOR_CODES['n'] = 78;
        ESC_CURSOR_CODES['o'] = 79;
    }

    enum ExpectedSequence {
        ESCAPE, ASSIGN, XY
    }

    private final Cursor cursor;

    private volatile ExpectedSequence expect = ESCAPE;

    LoadCursorPosition(Cursor cursor) {
        this.cursor = Objects.requireNonNull(cursor);
    }

    boolean accept(Short data) {
        short ESC = 0x1B;
        boolean accepted = false;

        if (expect == ESCAPE && data == ESC) {
            expect = ASSIGN;
            accepted = true;
        } else if (expect == ASSIGN && data == '=') {
            expect = XY;
            accepted = true;
        } else if (expect == XY) {
            int cursorPos = ESC_CURSOR_CODES[data];
            if (cursorPos != -1) {
                if (cursorPos < 24) {
                    cursor.set(cursorPos, cursorPos);
                } else {
                    cursor.set(cursorPos, cursor.getPoint().y);
                }
            }
            expect = ESCAPE;
            accepted = true;
        } else {
            expect = ESCAPE;
        }
        return accepted;
    }
}
