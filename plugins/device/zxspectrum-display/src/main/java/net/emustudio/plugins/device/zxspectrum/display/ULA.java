/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.zxspectrum.display;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.zxspectrum.display.gui.Keyboard;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Objects;

/**
 * https://worldofspectrum.org/faq/reference/48kreference.htm
 * <p>
 * OUT:
 * Bit   7   6   5   4   3   2   1   0
 * +-------------------------------+
 * |   |   |   | E | M |   Border  |
 * +-------------------------------+
 * <p>
 * IN: (bit 0 to bit 4 inclusive)
 * 0xfefe  SHIFT, Z, X, C, V            0xeffe  0, 9, 8, 7, 6
 * 0xfdfe  A, S, D, F, G                0xdffe  P, O, I, U, Y
 * 0xfbfe  Q, W, E, R, T                0xbffe  ENTER, L, K, J, H
 * 0xf7fe  1, 2, 3, 4, 5                0x7ffe  SPACE, SYM SHFT, M, N, B
 */
public class ULA implements Context8080.CpuPortDevice, Keyboard.OnKeyListener {
    public static final int SCREEN_WIDTH = 32; // in bytes; each byte represents 8 pixels in a row, reversed
    public static final int SCREEN_HEIGHT = 192;

    private final static byte[] RST_7 = new byte[0x38]; // works for IM1 and IM2 modes

    private final MemoryContext<Byte> memory;
    private final Context8080 cpu;
    private final byte[] keymap = new byte[8];
    public final byte[][] videoMemory = new byte[SCREEN_WIDTH][SCREEN_HEIGHT];
    private final static int[] lineStartOffsets = computeLineStartOffsets();

    private static int[] computeLineStartOffsets() {
        final int[] result = new int[SCREEN_HEIGHT];
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            result[y] = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2);
        }
        return result;
    }

    public ULA(MemoryContext<Byte> memory, Context8080 cpu) {
        this.memory = Objects.requireNonNull(memory);
        this.cpu = Objects.requireNonNull(cpu);
        Arrays.fill(keymap, (byte) 0xFF);
    }

    // little hack..
    public Context8080 getCpu() {
        return cpu;
    }

    public void readScreen() {
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            for (int x = 0; x < SCREEN_WIDTH; x++) {
                videoMemory[x][y] = memory.read(0x4000 + lineStartOffsets[y] + x);
            }
        }
        cpu.signalInterrupt(RST_7);
    }

    @Override
    public byte read(int portAddress) {
        if (portAddress == 0xfefe) {
            // SHIFT, Z, X, C, V
            return keymap[0];
        } else if (portAddress == 0xfdfe) {
            // A, S, D, F, G
            return keymap[1];
        } else if (portAddress == 0xfbfe) {
            // Q, W, E, R, T
            return keymap[2];
        } else if (portAddress == 0xf7fe) {
            // 1, 2, 3, 4, 5
            return keymap[3];
        } else if (portAddress == 0xeffe) {
            // 0, 9, 8, 7, 6
            return keymap[4];
        } else if (portAddress == 0xdffe) {
            // P, O, I, U, Y
            return keymap[5];
        } else if (portAddress == 0xbffe) {
            // ENTER, L, K, J, H
            return keymap[6];
        } else if (portAddress == 0x7ffe) {
            // SPACE, SYM SHFT, M, N, B
            return keymap[7];
        }

        return (byte) 0xFF;
    }

    @Override
    public void write(int portAddress, byte data) {
        // from CPU
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "ZX-Spectrum48K ULA";
    }

    @Override
    public void onKeyDown(byte data) {
        switch (data) {
            case KeyEvent.VK_SHIFT:
                keymap[0] |= 0x1;
                break;
            case 'Z':
                keymap[0] |= 0x2;
                break;
            case 'X':
                keymap[0] |= 0x4;
                break;
            case 'C':
                keymap[0] |= 0x8;
                break;
            case 'V':
                keymap[0] |= 0x10;
                break;
            case 'A':
                keymap[1] |= 0x1;
                break;
            case 'S':
                keymap[1] |= 0x2;
                break;
            case 'D':
                keymap[1] |= 0x4;
                break;
            case 'F':
                keymap[1] |= 0x8;
                break;
            case 'G':
                keymap[1] |= 0x10;
                break;
            case 'Q':
                keymap[2] |= 0x1;
                break;
            case 'W':
                keymap[2] |= 0x2;
                break;
            case 'E':
                keymap[2] |= 0x4;
                break;
            case 'R':
                keymap[2] |= 0x8;
                break;
            case 'T':
                keymap[2] |= 0x10;
                break;
            case '1':
                keymap[3] |= 0x1;
                break;
            case '2':
                keymap[3] |= 0x2;
                break;
            case '3':
                keymap[3] |= 0x4;
                break;
            case '4':
                keymap[3] |= 0x8;
                break;
            case '5':
                keymap[3] |= 0x10;
                break;
            case '0':
                keymap[4] |= 0x1;
                break;
            case '9':
                keymap[4] |= 0x2;
                break;
            case '8':
                keymap[4] |= 0x4;
                break;
            case '7':
                keymap[4] |= 0x8;
                break;
            case '6':
                keymap[4] |= 0x10;
                break;
            case 'P':
                keymap[5] |= 0x1;
                break;
            case 'O':
                keymap[5] |= 0x2;
                break;
            case 'I':
                keymap[5] |= 0x4;
                break;
            case 'U':
                keymap[5] |= 0x8;
                break;
            case 'Y':
                keymap[5] |= 0x10;
                break;
            case KeyEvent.VK_ENTER:
                keymap[6] |= 0x1;
                break;
            case 'L':
                keymap[6] |= 0x2;
                break;
            case 'K':
                keymap[6] |= 0x4;
                break;
            case 'J':
                keymap[6] |= 0x8;
                break;
            case 'H':
                keymap[6] |= 0x10;
                break;
            case ' ':
                keymap[7] |= 0x1;
                break;
            // SYM SHFT ???
            case 'M':
                keymap[7] |= 0x4;
                break;
            case 'N':
                keymap[7] |= 0x8;
                break;
            case 'B':
                keymap[7] |= 0x10;
                break;
        }
    }

    @Override
    public void onKeyUp(byte data) {
        switch (data) {
            case KeyEvent.VK_SHIFT:
                keymap[0] &= 0xFE;
                break;
            case 'Z':
                keymap[0] &= 0xFD;
                break;
            case 'X':
                keymap[0] &= 0xFB;
                break;
            case 'C':
                keymap[0] &= 0xF7;
                break;
            case 'V':
                keymap[0] &= 0xEF;
                break;
            case 'A':
                keymap[1] &= 0xFE;
                break;
            case 'S':
                keymap[1] &= 0xFD;
                break;
            case 'D':
                keymap[1] &= 0xFB;
                break;
            case 'F':
                keymap[1] &= 0xF7;
                break;
            case 'G':
                keymap[1] &= 0xEF;
                break;
            case 'Q':
                keymap[2] &= 0xFE;
                break;
            case 'W':
                keymap[2] &= 0xFD;
                break;
            case 'E':
                keymap[2] &= 0xFB;
                break;
            case 'R':
                keymap[2] &= 0xF7;
                break;
            case 'T':
                keymap[2] &= 0xEF;
                break;
            case '1':
                keymap[3] &= 0xFE;
                break;
            case '2':
                keymap[3] &= 0xFD;
                break;
            case '3':
                keymap[3] &= 0xFB;
                break;
            case '4':
                keymap[3] &= 0xF7;
                break;
            case '5':
                keymap[3] &= 0xEF;
                break;
            case '0':
                keymap[4] &= 0xFE;
                break;
            case '9':
                keymap[4] &= 0xFD;
                break;
            case '8':
                keymap[4] &= 0xFB;
                break;
            case '7':
                keymap[4] &= 0xF7;
                break;
            case '6':
                keymap[4] &= 0xEF;
                break;
            case 'P':
                keymap[5] &= 0xFE;
                break;
            case 'O':
                keymap[5] &= 0xFD;
                break;
            case 'I':
                keymap[5] &= 0xFB;
                break;
            case 'U':
                keymap[5] &= 0xF7;
                break;
            case 'Y':
                keymap[5] &= 0xEF;
                break;
            case KeyEvent.VK_ENTER:
                keymap[6] &= 0xFE;
                break;
            case 'L':
                keymap[6] &= 0xFD;
                break;
            case 'K':
                keymap[6] &= 0xFB;
                break;
            case 'J':
                keymap[6] &= 0xF7;
                break;
            case 'H':
                keymap[6] &= 0xEF;
                break;
            case ' ':
                keymap[7] &= 0xFE;
                break;
            // SYM SHFT ???
            case 'M':
                keymap[7] &= 0xFB;
                break;
            case 'N':
                keymap[7] &= 0xF7;
                break;
            case 'B':
                keymap[7] &= 0xEF;
                break;
        }
    }

}
