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

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.emustudio.plugins.device.zxspectrum.ula.gui.Keyboard;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Uncommitted Logic Array (ULA).
 * <p>
 * References:
 * - <a href="https://worldofspectrum.org/faq/reference/48kreference.htm">ZX-Spctrum 48K Technical Reference</a>
 * - <a href="http://www.breakintoprogram.co.uk/hardware/computers/zx-spectrum/screen-memory-layout">Screen Memory layout</a>
 *
 * <p>
 * OUT:
 * 7   6   5   4   3   2   1   0
 * +-------------------------------+
 * |   |   |   | E | M |   Border  |
 * +-------------------------------+
 * <p>
 * Keyboard matrix:
 * - on host SHIFT + letter/number = ZX "shift" + letter / number
 * - on host CTRL + letter/number = ZX symbol "shift" + letter/number
 * - on host plain letter/number = ZX letter/number
 * <p>
 * IN: (bit 0 to bit 4 inclusive)
 * 0xfefe  SHIFT, Z, X, C, V            0xeffe  0, 9, 8, 7, 6
 * 0xfdfe  A, S, D, F, G                0xdffe  P, O, I, U, Y
 * 0xfbfe  Q, W, E, R, T                0xbffe  ENTER, L, K, J, H
 * 0xf7fe  1, 2, 3, 4, 5                0x7ffe  SPACE, SYM SHIFT, M, N, B
 * <p>
 * The colour attribute data overlays the monochrome bitmap data and is arranged in a linear fashion from left to right,
 * top to bottom.
 * Each attribute byte colours is 8x8 character on the screen and is encoded as follows:
 * <p>
 * 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
 * F | B | P2| P1| P0| I2| I1| I0|
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

    private final byte[] keymap = new byte[8]; // keyboard state
    public final byte[][] videoMemory = new byte[SCREEN_WIDTH][SCREEN_HEIGHT];
    public final byte[][] attributeMemory = new byte[SCREEN_WIDTH][ATTRIBUTE_HEIGHT];
    private final static int[] lineStartOffsets = computeLineStartOffsets();

    // maps host characters to ZX Spectrum key "commands" (keymap index, "zero" value)
    private final static Map<Character, Byte[]> CHAR_MAPPING = new HashMap<>();

    static {
        CHAR_MAPPING.put('z', new Byte[]{0, 2}); // z, COPY, ":"
        CHAR_MAPPING.put('x', new Byte[]{0, 4}); // x, CLEAR, "£"
        CHAR_MAPPING.put('c', new Byte[]{0, 8}); // c, CONT, "?"
        CHAR_MAPPING.put('v', new Byte[]{0, 16}); // v, CLS, "/"
        CHAR_MAPPING.put('b', new Byte[]{7, 16}); // b, BORDER, "*"
        CHAR_MAPPING.put('n', new Byte[]{7, 8}); // n, NEXT, ","
        CHAR_MAPPING.put('m', new Byte[]{7, 4}); // m, PAUSE, "."
        CHAR_MAPPING.put(' ', new Byte[]{7, 1}); // " "
        CHAR_MAPPING.put('a', new Byte[]{1, 1}); // a, NEW, "STOP"
        CHAR_MAPPING.put('s', new Byte[]{1, 2}); // s, SAVE, "NOT"
        CHAR_MAPPING.put('d', new Byte[]{1, 4}); // d, DIM, "STEP"
        CHAR_MAPPING.put('f', new Byte[]{1, 8}); // f, FOR, "TO"
        CHAR_MAPPING.put('g', new Byte[]{1, 16}); // g, GOTO, "THEN"
        CHAR_MAPPING.put('h', new Byte[]{6, 16}); // h, GOSUB, "↑"
        CHAR_MAPPING.put('j', new Byte[]{6, 8}); // j, LOAD, "-"
        CHAR_MAPPING.put('k', new Byte[]{6, 4}); // k, LIST, "+"
        CHAR_MAPPING.put('l', new Byte[]{6, 2}); // l, LET, "="
        CHAR_MAPPING.put('q', new Byte[]{2, 1}); // q, PLOT, "<="
        CHAR_MAPPING.put('w', new Byte[]{2, 2}); // w, DRAW, "<>"
        CHAR_MAPPING.put('e', new Byte[]{2, 4}); // e, REM, ">="
        CHAR_MAPPING.put('r', new Byte[]{2, 8}); // r, RUN, "<"
        CHAR_MAPPING.put('t', new Byte[]{2, 16}); // t, RAND, ">"
        CHAR_MAPPING.put('y', new Byte[]{5, 16}); // y, RETURN, "AND"
        CHAR_MAPPING.put('u', new Byte[]{5, 8}); // u, IF, "OR"
        CHAR_MAPPING.put('i', new Byte[]{5, 4}); // i, INPUT, "AT"
        CHAR_MAPPING.put('o', new Byte[]{5, 2}); // o, POKE, ";"
        CHAR_MAPPING.put('p', new Byte[]{5, 1}); // p, PRINT, "
        CHAR_MAPPING.put('1', new Byte[]{3, 1}); // 1, "!"
        CHAR_MAPPING.put('2', new Byte[]{3, 2}); // 2, "@"
        CHAR_MAPPING.put('3', new Byte[]{3, 4}); // 3, "#"
        CHAR_MAPPING.put('4', new Byte[]{3, 8}); // 4, "$"
        CHAR_MAPPING.put('5', new Byte[]{3, 16}); // 5, "%"
        CHAR_MAPPING.put('6', new Byte[]{4, 16}); // 6, "&"
        CHAR_MAPPING.put('7', new Byte[]{4, 8}); // 7, "'"
        CHAR_MAPPING.put('8', new Byte[]{4, 4}); // 8, "("
        CHAR_MAPPING.put('9', new Byte[]{4, 2}); // 9, ")"
        CHAR_MAPPING.put('0', new Byte[]{4, 1}); // 0, "_"
    }

    // The Spectrum's 'FLASH' effect is also produced by the ULA: Every 16 frames, the ink and paper of all flashing
    // bytes is swapped; ie a normal to inverted to normal cycle takes 32 frames, which is (good as) 0.64 seconds.
    public boolean videoFlash = false;
    private int flashFramesCount = 0;

    private final ZxSpectrumBus bus;

    private int borderColor;
    private boolean microphoneAndEarOut; // TODO: audio


    public ULA(ZxSpectrumBus bus) {
        this.bus = Objects.requireNonNull(bus);
        Arrays.fill(keymap, (byte) 0xBF);
    }

    public void reset() {
        borderColor = 7;
        microphoneAndEarOut = false;
        Arrays.fill(keymap, (byte) 0xBF);
    }

    public void onNextFrame() {
        if (flashFramesCount == 15) {
            videoFlash = !videoFlash;
        }
        flashFramesCount = (flashFramesCount + 1) % 16;
    }

    public void readScreen() {
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            for (int y = 0; y < SCREEN_HEIGHT; y++) {
                videoMemory[x][y] = bus.readMemoryNotContended(0x4000 + lineStartOffsets[y] + x);
                if (y < ATTRIBUTE_HEIGHT) {
                    int off = ((y >>> 3) << 8) | (((y & 0x07) << 5) | x);
                    int attributeAddress = 0x5800 + off;
                    attributeMemory[x][y] = bus.readMemoryNotContended(attributeAddress);
                }
            }
        }
    }

    public void triggerInterrupt() {
        bus.signalInterrupt(RST_7);
    }

    public void readLine(int y) {
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            videoMemory[x][y] = bus.readMemoryNotContended(0x4000 + lineStartOffsets[y] + x);
            if (y < ATTRIBUTE_HEIGHT) {
                int off = ((y >>> 3) << 8) | (((y & 0x07) << 5) | x);
                int attributeAddress = 0x5800 + off;
                attributeMemory[x][y] = bus.readMemoryNotContended(attributeAddress);
            }
        }
    }

    public int getBorderColor() {
        return borderColor;
    }

    public ZxSpectrumBus getBus() {
        return bus;
    }

    @Override
    public byte read(int portAddress) {
        // A zero in one of the five lowest bits means that the corresponding key is pressed.
        // If more than one address line is made low, the result is the logical AND of all single inputs

        byte result = (byte) 0xBF; // 1011 1111   // no EAR input
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

        // LINE IN?
        if ((bus.readData() & 1) == 1) {
            result |= 0x40;
        }
        return result;
    }

    @Override
    public void write(int portAddress, byte data) {
        this.borderColor = data & 7;
        // the EAR and MIC sockets are connected only by resistors, so activating one activates the other
        microphoneAndEarOut = ((data & 0x10) == 0x10) || ((data & 0x8) == 0);
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
    public void onKeyUp(KeyEvent evt) {
        int keyCode = evt.getExtendedKeyCode();

        if (keyCode == KeyEvent.VK_CONTROL) {
            keymap[7] |= 0x2; // symshift
        } else if (evt.getKeyCode() == KeyEvent.VK_SHIFT) {
            keymap[0] |= 0x1; // shift
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            keymap[6] |= 0x1; // ENTER
        } else {
            char c = (char) Character.toLowerCase(keyCode);
            Byte[] command = CHAR_MAPPING.get(c);
            if (command != null) {
                keymap[command[0]] |= command[1];
            }
        }
    }

    @Override
    public void onKeyDown(KeyEvent evt) {
        int keyCode = evt.getExtendedKeyCode();

        if (keyCode == KeyEvent.VK_CONTROL) {
            keymap[7] &= (byte) 0xFD; // symshift
        } else if (keyCode == KeyEvent.VK_SHIFT) {
            keymap[0] &= (byte) 0xFE; // shift
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            keymap[6] &= (byte) 0xFE; // ENTER
        } else {
            char c = (char) Character.toLowerCase(keyCode);
            Byte[] command = CHAR_MAPPING.get(c);
            if (command != null) {
                keymap[command[0]] &= (byte) ((~command[1]) & 0xFF);
            }
        }
    }

    private static int[] computeLineStartOffsets() {
        final int[] result = new int[SCREEN_HEIGHT];
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            result[y] = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2);
        }
        return result;
    }
}
