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
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.zxspectrum.ula.gui.Keyboard;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Objects;

/**
 * https://worldofspectrum.org/faq/reference/48kreference.htm
 * http://www.breakintoprogram.co.uk/hardware/computers/zx-spectrum/screen-memory-layout
 * <p>
 * Uncommitted Logic Array (ULA).
 *
 * <p>
 * OUT:
 * 7   6   5   4   3   2   1   0
 * +-------------------------------+
 * |   |   |   | E | M |   Border  |
 * +-------------------------------+
 * <p>
 * IN: (bit 0 to bit 4 inclusive)
 * 0xfefe  SHIFT, Z, X, C, V            0xeffe  0, 9, 8, 7, 6
 * 0xfdfe  A, S, D, F, G                0xdffe  P, O, I, U, Y
 * 0xfbfe  Q, W, E, R, T                0xbffe  ENTER, L, K, J, H
 * 0xf7fe  1, 2, 3, 4, 5                0x7ffe  SPACE, SYM SHFT, M, N, B
 * <p>
 * The colour attribute data overlays the monochrome bitmap data and is arranged in a linear fashion from left to right,
 * top to bottom.
 * Each attribute byte colours an 8x8 character on the screen and is encoded as follows:
 * <p>
 * 7   6   5   4   3   2   1   0
 * +-------------------------------+
 * | F | B | P2| P1| P0| I2| I1| I0|
 * +-------------------------------+
 * <p>
 * - F sets the attribute FLASH mode
 * - B sets the attribute BRIGHTNESS mode
 * - P2 to P0 is the PAPER colour
 * - I2 to I0 is the INK colour
 */
public class ULA implements Context8080.CpuPortDevice, Keyboard.OnKeyListener {
    public static final int SCREEN_WIDTH = 32; // in bytes; each byte represents 8 pixels in a row, reversed
    public static final int SCREEN_HEIGHT = 192;
    public static final int ATTRIBUTE_HEIGHT = SCREEN_HEIGHT / 8;

    private final static byte[] RST_7 = new byte[0x38]; // works for IM1 and IM2 modes

    private final byte[] keymap = new byte[8];
    public final byte[][] videoMemory = new byte[SCREEN_WIDTH][SCREEN_HEIGHT];
    public final byte[][] attributeMemory = new byte[SCREEN_WIDTH][ATTRIBUTE_HEIGHT];
    private final static int[] lineStartOffsets = computeLineStartOffsets();

    private final MemoryContext<Byte> memory;
    private final Context8080 cpu;

    private int borderColor;
    private boolean microphoneAndEar;

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
        Arrays.fill(keymap, (byte) 0xBF);
    }

    // little hack..
    public Context8080 getCpu() {
        return cpu;
    }

    public void readScreen() {
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            for (int y = 0; y < SCREEN_HEIGHT; y++) {
                videoMemory[x][y] = memory.read(0x4000 + lineStartOffsets[y] + x);
                if (y < ATTRIBUTE_HEIGHT) {
                    int off = ((y >>> 3) << 8) | (((y & 0x07) << 5) | x);
                    int attributeAddress = 0x5800 + off;
                    attributeMemory[x][y] = memory.read(attributeAddress);
                }
            }
        }
    }

    public void triggerInterrupt() {
        cpu.signalInterrupt(RST_7);
    }

    public void readLine(int y) {
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            videoMemory[x][y] = memory.read(0x4000 + lineStartOffsets[y] + x);
            if (y < ATTRIBUTE_HEIGHT) {
                int off = ((y >>> 3) << 8) | (((y & 0x07) << 5) | x);
                int attributeAddress = 0x5800 + off;
                attributeMemory[x][y] = memory.read(attributeAddress);
            }
        }
    }

    public void reset() {
        borderColor = 7;
        microphoneAndEar = false;
        Arrays.fill(keymap, (byte) 0xBF);
    }

    public int getBorderColor() {
        return borderColor;
    }

    @Override
    public byte read(int portAddress) {
        // A zero in one of the five lowest bits means that the corresponding key is pressed.
        // If more than one address line is made low, the result is the logical AND of all single inputs

        byte result = (byte) 0xBF; // 1011 1111   // EAR on
        if ((portAddress & 0xFEFE) == 0xFEFE) {
            // SHIFT, Z, X, C, V
            result &= keymap[0];
        } else if ((portAddress & 0xFDFE) == 0xFDFE) {
            // A, S, D, F, G
            result &= keymap[1];
        } else if ((portAddress & 0xFBFE) == 0xFBFE) {
            // Q, W, E, R, T
            result &= keymap[2];
        } else if ((portAddress & 0xF7FE) == 0xF7FE) {
            // 1, 2, 3, 4, 5
            result &= keymap[3];
        } else if ((portAddress & 0xEFFE) == 0xEFFE) {
            // 0, 9, 8, 7, 6
            result &= keymap[4];
        } else if ((portAddress & 0xDFFE) == 0xDFFE) {
            // P, O, I, U, Y
            result &= keymap[5];
        } else if ((portAddress & 0xBFFE) == 0xBFFE) {
            // ENTER, L, K, J, H
            result &= keymap[6];
        } else if ((portAddress & 0x7FFE) == 0x7FFE) {
            // SPACE, SYM SHFT, M, N, B
            result &= keymap[7];
        }
        if (!microphoneAndEar) {
            result |= 0x40;
        }
        return result;
    }

    @Override
    public void write(int portAddress, byte data) {
        this.borderColor = data & 7;
        // the EAR and MIC sockets are connected only by resistors, so activating one activates the other
        microphoneAndEar = ((data & 0x10) == 0x10) || ((data & 0x8) == 0);
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
    public void onKeyUp(byte data) {
        switch (data) {
            case KeyEvent.VK_SHIFT:
                keymap[0] |= 0x1;
                break;
            case ':':  // deactivates symshift
                keymap[7] |= 0x2;
            case 'z':
            case 'Z':
                keymap[0] |= 0x2;
                break;
            case 'x':
            case 'X':
                keymap[0] |= 0x4;
                break;
            case '?': // deactivates symshift
                keymap[7] |= 0x2;
            case 'c':
            case 'C':
                keymap[0] |= 0x8;
                break;
            case '/': // deactivates symshift
                keymap[7] |= 0x2;
            case 'v':
            case 'V':
                keymap[0] |= 0x10;
                break;
            case 'a':
            case 'A':
                keymap[1] |= 0x1;
                break;
            case 's':
            case 'S':
                keymap[1] |= 0x2;
                break;
            case 'd':
            case 'D':
                keymap[1] |= 0x4;
                break;
            case 'f':
            case 'F':
                keymap[1] |= 0x8;
                break;
            case 'g':
            case 'G':
                keymap[1] |= 0x10;
                break;
            case 'q':
            case 'Q':
                keymap[2] |= 0x1;
                break;
            case 'w':
            case 'W':
                keymap[2] |= 0x2;
                break;
            case 'e':
            case 'E':
                keymap[2] |= 0x4;
                break;
            case '<': // deactivates symshift
                keymap[7] |= 0x2;
            case 'r':
            case 'R':
                keymap[2] |= 0x8;
                break;
            case '>': // deactivates symshift
                keymap[7] |= 0x2;
            case 't':
            case 'T':
                keymap[2] |= 0x10;
                break;
            case '!': // deactivates symshift
                keymap[7] |= 0x2;
            case '1':
                keymap[3] |= 0x1;
                break;
            case '@': // deactivates symshift
                keymap[7] |= 0x2;
            case '2':
                keymap[3] |= 0x2;
                break;
            case '#': // deactivates symshift
                keymap[7] |= 0x2;
            case '3':
                keymap[3] |= 0x4;
                break;
            case '$': // deactivates symshift
                keymap[7] |= 0x2;
            case '4':
                keymap[3] |= 0x8;
                break;
            case '%': // deactivates symshift
                keymap[7] |= 0x2;
            case '5':
                keymap[3] |= 0x10;
                break;
            case '_': // deactivates symshift
                keymap[7] |= 0x2;
            case '0':
                keymap[4] |= 0x1;
                break;
            case ')': // deactivates symshift
                keymap[7] |= 0x2;
            case '9':
                keymap[4] |= 0x2;
                break;
            case '(': // deactivates symshift
                keymap[7] |= 0x2;
            case '8':
                keymap[4] |= 0x4;
                break;
            case '\'': // deactivates symshift
                keymap[7] |= 0x2;
            case '7':
                keymap[4] |= 0x8;
                break;
            case '&': // deactivates symshift
                keymap[7] |= 0x2;
            case '6':
                keymap[4] |= 0x10;
                break;
            case '\\': // deactivates symshift
                keymap[7] |= 0x2;
            case 'p':
            case 'P':
                keymap[5] |= 0x1;
                break;
            case '"': // deactivates symshift
                keymap[7] |= 0x2;
            case 'o':
            case 'O':
                keymap[5] |= 0x2;
                break;
            case ';': // deactivates symshift
                keymap[7] |= 0x2;
            case 'i':
            case 'I':
                keymap[5] |= 0x4;
                break;
            case 'u':
            case 'U':
                keymap[5] |= 0x8;
                break;
            case 'y':
            case 'Y':
                keymap[5] |= 0x10;
                break;
            case KeyEvent.VK_ENTER:
                keymap[6] |= 0x1;
                break;
            case '=': // deactivates symshift
                keymap[7] |= 0x2;
            case 'l':
            case 'L':
                keymap[6] |= 0x2;
                break;
            case '+': // deactivates symshift
                keymap[7] |= 0x2;
            case 'k':
            case 'K':
                keymap[6] |= 0x4;
                break;
            case '-': // deactivates symshift
                keymap[7] |= 0x2;
            case 'j':
            case 'J':
                keymap[6] |= 0x8;
                break;
            case '^': // deactivates symshift
                keymap[7] |= 0x2;
            case 'h':
            case 'H':
                keymap[6] |= 0x10;
                break;
            case ' ':
                keymap[7] |= 0x1;
                break;
            case '.': // deactivates symshift
                keymap[7] |= 0x2;
            case 'm':
            case 'M':
                keymap[7] |= 0x4;
                break;
            case ',': // deactivates symshift
                keymap[7] |= 0x2;
            case 'n':
            case 'N':
                keymap[7] |= 0x8;
                break;
            case '*': // deactivates symshift
                keymap[7] |= 0x2;
            case 'b':
            case 'B':
                keymap[7] |= 0x10;
                break;
        }
    }

    @Override
    public void onKeyDown(byte data) {
        switch (data) {
            case KeyEvent.VK_SHIFT:
                keymap[0] &= 0xFE;
                break;
            case ':': // activates symshift
                keymap[7] &= 0xFD;
            case 'z':
            case 'Z':
                keymap[0] &= 0xFD;
                break;
            case 'x':
            case 'X':
                keymap[0] &= 0xFB;
                break;
            case '?': // activates symshift
                keymap[7] &= 0xFD;
            case 'c':
            case 'C':
                keymap[0] &= 0xF7;
                break;
            case '/': // activates symshift
                keymap[7] &= 0xFD;
            case 'v':
            case 'V':
                keymap[0] &= 0xEF;
                break;
            case 'a':
            case 'A':
                keymap[1] &= 0xFE;
                break;
            case 's':
            case 'S':
                keymap[1] &= 0xFD;
                break;
            case 'd':
            case 'D':
                keymap[1] &= 0xFB;
                break;
            case 'f':
            case 'F':
                keymap[1] &= 0xF7;
                break;
            case 'g':
            case 'G':
                keymap[1] &= 0xEF;
                break;
            case 'q':
            case 'Q':
                keymap[2] &= 0xFE;
                break;
            case 'w':
            case 'W':
                keymap[2] &= 0xFD;
                break;
            case 'e':
            case 'E':
                keymap[2] &= 0xFB;
                break;
            case '<': // activates symshift
                keymap[7] &= 0xFD;
            case 'r':
            case 'R':
                keymap[2] &= 0xF7;
                break;
            case '>': // activates symshift
                keymap[7] &= 0xFD;
            case 't':
            case 'T':
                keymap[2] &= 0xEF;
                break;
            case '!': // activates symshift
                keymap[7] &= 0xFD;
            case '1':
                keymap[3] &= 0xFE;
                break;
            case '@': // activates symshift
                keymap[7] &= 0xFD;
            case '2':
                keymap[3] &= 0xFD;
                break;
            case '#': // activates symshift
                keymap[7] &= 0xFD;
            case '3':
                keymap[3] &= 0xFB;
                break;
            case '$': // activates symshift
                keymap[7] &= 0xFD;
            case '4':
                keymap[3] &= 0xF7;
                break;
            case '%': // activates symshift
                keymap[7] &= 0xFD;
            case '5':
                keymap[3] &= 0xEF;
                break;
            case '_': // activates symshift
                keymap[7] &= 0xFD;
            case '0':
                keymap[4] &= 0xFE;
                break;
            case ')': // activates symshift
                keymap[7] &= 0xFD;
            case '9':
                keymap[4] &= 0xFD;
                break;
            case '(': // activates symshift
                keymap[7] &= 0xFD;
            case '8':
                keymap[4] &= 0xFB;
                break;
            case '\'': // activates symshift
                keymap[7] &= 0xFD;
            case '7':
                keymap[4] &= 0xF7;
                break;
            case '&': // activates symshift
                keymap[7] &= 0xFD;
            case '6':
                keymap[4] &= 0xEF;
                break;
            case '\\': // activates symshift
                keymap[7] &= 0xFD;
            case 'p':
            case 'P':
                keymap[5] &= 0xFE;
                break;
            case '"': // activates symshift
                keymap[7] &= 0xFD;
            case 'o':
            case 'O':
                keymap[5] &= 0xFD;
                break;
            case ';': // activates symshift
                keymap[7] &= 0xFD;
            case 'i':
            case 'I':
                keymap[5] &= 0xFB;
                break;
            case 'u':
            case 'U':
                keymap[5] &= 0xF7;
                break;
            case 'y':
            case 'Y':
                keymap[5] &= 0xEF;
                break;
            case KeyEvent.VK_ENTER:
                keymap[6] &= 0xFE;
                break;
            case '=': // activates symshift
                keymap[7] &= 0xFD;
            case 'l':
            case 'L':
                keymap[6] &= 0xFD;
                break;
            case '+': // activates symshift
                keymap[7] &= 0xFD;
            case 'k':
            case 'K':
                keymap[6] &= 0xFB;
                break;
            case '-': // activates symshift
                keymap[7] &= 0xFD;
            case 'j':
            case 'J':
                keymap[6] &= 0xF7;
                break;
            case '^': // activates symshift
                keymap[7] &= 0xFD;
            case 'h':
            case 'H':
                keymap[6] &= 0xEF;
                break;
            case ' ':
                keymap[7] &= 0xFE;
                break;
            case '.': // activates symshift
                keymap[7] &= 0xFD;
            case 'm':
            case 'M':
                keymap[7] &= 0xFB;
                break;
            case ',': // activates symshift
                keymap[7] &= 0xFD;
            case 'n':
            case 'N':
                keymap[7] &= 0xF7;
                break;
            case '*': // activates symshift
                keymap[7] &= 0xFD;
            case 'b':
            case 'B':
                keymap[7] &= 0xEF;
                break;
        }
    }

}
