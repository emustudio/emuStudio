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
import net.emustudio.plugins.device.zxspectrum.ula.gui.KeyboardDispatcher;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static java.awt.event.KeyEvent.*;
import static net.emustudio.plugins.device.zxspectrum.ula.ZxParameters.*;

/**
 * Uncommitted Logic Array (ULA).
 * <p>
 * References:
 * - <a href="https://worldofspectrum.org/faq/reference/48kreference.htm">ZX-Spctrum 48K Technical Reference</a>
 * - <a href="http://www.breakintoprogram.co.uk/hardware/computers/zx-spectrum/screen-memory-layout">Screen Memory layout</a>
 *
 * <p>
 * The ULA component in emuStudio is a "mediator" of interaction between host and emulator. That means, it handles:
 * - keyboard
 * - audio
 * - video (maps RAM to video/attribute memory, video flash, border color)
 * <p>
 * From the ZX Spectrum point of view, it represents the port 0xFE (254).
 * <p>
 * Port 0xFE write:
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
 * Port 0xFE read (bit 0 to bit 4 inclusive):
 * 0xfefe  SHIFT, Z, X, C, V            0xeffe  0, 9, 8, 7, 6
 * 0xfdfe  A, S, D, F, G                0xdffe  P, O, I, U, Y
 * 0xfbfe  Q, W, E, R, T                0xbffe  ENTER, L, K, J, H
 * 0xf7fe  1, 2, 3, 4, 5                0x7ffe  SPACE, SYM SHIFT, M, N, B
 * <p>
 * The colour attribute data overlays the monochrome bitmap data and is arranged in a linear fashion from left to right,
 * top to bottom. Each attribute byte colours is 8x8 character on the screen and is encoded as follows:
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
public class ULA implements Context8080.CpuPortDevice, KeyboardDispatcher.OnKeyListener {
    private final static byte[] RST_7 = new byte[0x38]; // works for IM1 and IM2 modes
    private final static byte[] KEY_SHIFT = new byte[]{0, 1};
    private final static byte[] KEY_SYM_SHIFT = new byte[]{7, 2};
    private final static int[] LINE_OFFSETS = computeLineOffsets();

    private final byte[] keymap = new byte[8]; // keyboard state

    // accessible from outside
    public final byte[][] videoMemory = new byte[SCREEN_WIDTH][SCREEN_HEIGHT];
    public final byte[][] attributeMemory = new byte[SCREEN_WIDTH][ATTRIBUTE_HEIGHT];

    // maps host characters to ZX Spectrum key "commands"
    // Byte[] = {keymap index, "zero" value, shift, symshift}
    private final static Map<Integer, Byte[]> CHAR_MAPPING = new HashMap<>();

    static {
        CHAR_MAPPING.put(VK_Z, new Byte[]{0, 2, -1, -1}); // z, COPY, ":"
        CHAR_MAPPING.put(VK_COLON, new Byte[]{0, 2, 0, 1});
        CHAR_MAPPING.put(VK_X, new Byte[]{0, 4, -1, -1}); // x, CLEAR, "£"
        CHAR_MAPPING.put(VK_C, new Byte[]{0, 8, -1, -1}); // c, CONT, "?"
        CHAR_MAPPING.put(VK_V, new Byte[]{0, 16, -1, -1}); // v, CLS, "/"
        CHAR_MAPPING.put(VK_SLASH, new Byte[]{0, 16, 0, 1});
        CHAR_MAPPING.put(VK_B, new Byte[]{7, 16, -1, -1}); // b, BORDER, "*"
        CHAR_MAPPING.put(VK_ASTERISK, new Byte[]{7, 16, 0, 1});
        CHAR_MAPPING.put(VK_N, new Byte[]{7, 8, -1, -1}); // n, NEXT, ","
        CHAR_MAPPING.put(VK_COMMA, new Byte[]{7, 8, 0, 1});
        CHAR_MAPPING.put(VK_M, new Byte[]{7, 4, -1, -1}); // m, PAUSE, "."
        CHAR_MAPPING.put(VK_DECIMAL, new Byte[]{7, 4, 0, 1});
        CHAR_MAPPING.put(VK_PERIOD, new Byte[]{7, 4, 0, 1});
        CHAR_MAPPING.put(VK_SPACE, new Byte[]{7, 1, -1, -1}); // " "
        CHAR_MAPPING.put(VK_ENTER, new Byte[]{6, 1, -1, -1}); // ENTER
        CHAR_MAPPING.put(VK_A, new Byte[]{1, 1, -1, -1}); // a, NEW, "STOP"
        CHAR_MAPPING.put(VK_S, new Byte[]{1, 2, -1, -1}); // s, SAVE, "NOT"
        CHAR_MAPPING.put(VK_D, new Byte[]{1, 4, -1, -1}); // d, DIM, "STEP"
        CHAR_MAPPING.put(VK_F, new Byte[]{1, 8, -1, -1}); // f, FOR, "TO"
        CHAR_MAPPING.put(VK_G, new Byte[]{1, 16, -1, -1}); // g, GOTO, "THEN"
        CHAR_MAPPING.put(VK_H, new Byte[]{6, 16, -1, -1}); // h, GOSUB, "↑"
        CHAR_MAPPING.put(VK_UP, new Byte[]{6, 16, 0, 1});
        CHAR_MAPPING.put(VK_J, new Byte[]{6, 8, -1, -1}); // j, LOAD, "-"
        CHAR_MAPPING.put(VK_SUBTRACT, new Byte[]{6, 8, 0, 1});
        CHAR_MAPPING.put(VK_K, new Byte[]{6, 4, -1, -1}); // k, LIST, "+"
        CHAR_MAPPING.put(VK_ADD, new Byte[]{6, 4, 0, 1});
        CHAR_MAPPING.put(VK_L, new Byte[]{6, 2, -1, -1}); // l, LET, "="
        CHAR_MAPPING.put(VK_EQUALS, new Byte[]{6, 2, 0, 1});
        CHAR_MAPPING.put(VK_Q, new Byte[]{2, 1, -1, -1}); // q, PLOT, "<="
        CHAR_MAPPING.put(VK_W, new Byte[]{2, 2, -1, -1}); // w, DRAW, "<>"
        CHAR_MAPPING.put(VK_E, new Byte[]{2, 4, -1, -1}); // e, REM, ">="
        CHAR_MAPPING.put(VK_R, new Byte[]{2, 8, -1, -1}); // r, RUN, "<"
        CHAR_MAPPING.put(VK_LESS, new Byte[]{2, 8, 0, 1});
        CHAR_MAPPING.put(VK_T, new Byte[]{2, 16, -1, -1}); // t, RAND, ">"
        CHAR_MAPPING.put(VK_GREATER, new Byte[]{2, 16, 0, 1});
        CHAR_MAPPING.put(VK_Y, new Byte[]{5, 16, -1, -1}); // y, RETURN, "AND"
        CHAR_MAPPING.put(VK_U, new Byte[]{5, 8, -1, -1}); // u, IF, "OR"
        CHAR_MAPPING.put(VK_I, new Byte[]{5, 4, -1, -1}); // i, INPUT, "AT"
        CHAR_MAPPING.put(VK_O, new Byte[]{5, 2, -1, -1}); // o, POKE, ";"
        CHAR_MAPPING.put(VK_SEMICOLON, new Byte[]{5, 2, 0, 1});
        CHAR_MAPPING.put(VK_P, new Byte[]{5, 1, -1, -1}); // p, PRINT, "
        CHAR_MAPPING.put(VK_QUOTEDBL, new Byte[]{5, 1, 0, 1});
        CHAR_MAPPING.put(VK_1, new Byte[]{3, 1, -1, -1}); // 1, "!"
        CHAR_MAPPING.put(VK_EXCLAMATION_MARK, new Byte[]{3, 1, 0, 1});
        CHAR_MAPPING.put(VK_2, new Byte[]{3, 2, -1, -1}); // 2, "@"
        CHAR_MAPPING.put(VK_AT, new Byte[]{3, 2, 0, 1});
        CHAR_MAPPING.put(VK_3, new Byte[]{3, 4, -1, -1}); // 3, "#"
        CHAR_MAPPING.put(VK_NUMBER_SIGN, new Byte[]{3, 4, 0, 1});
        CHAR_MAPPING.put(VK_4, new Byte[]{3, 8, -1, -1}); // 4, "$"
        CHAR_MAPPING.put(VK_DOLLAR, new Byte[]{3, 8, 0, 1});
        CHAR_MAPPING.put(VK_5, new Byte[]{3, 16, -1, -1}); // 5, "%"
        CHAR_MAPPING.put(VK_6, new Byte[]{4, 16, -1, -1}); // 6, "&"
        CHAR_MAPPING.put(VK_AMPERSAND, new Byte[]{4, 16, 0, 1});
        CHAR_MAPPING.put(VK_7, new Byte[]{4, 8, -1, -1}); // 7, "'"
        CHAR_MAPPING.put(VK_QUOTE, new Byte[]{4, 8, 0, 1});
        CHAR_MAPPING.put(VK_8, new Byte[]{4, 4, -1, -1}); // 8, "("
        CHAR_MAPPING.put(VK_LEFT_PARENTHESIS, new Byte[]{4, 4, 0, 1});
        CHAR_MAPPING.put(VK_9, new Byte[]{4, 2, -1, -1}); // 9, ")"
        CHAR_MAPPING.put(VK_RIGHT_PARENTHESIS, new Byte[]{4, 2, 0, 1});
        CHAR_MAPPING.put(VK_0, new Byte[]{4, 1, -1, -1}); // 0, "_"
        CHAR_MAPPING.put(VK_UNDERSCORE, new Byte[]{4, 1, 0, 1});
        CHAR_MAPPING.put(VK_BACK_SPACE, new Byte[]{4, 1, 1, 0}); // backspace
        CHAR_MAPPING.put(VK_DELETE, new Byte[]{4, 1, 1, 0}); // delete
    }

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
        bus.signalInterrupt(RST_7);
        if (flashFramesCount == VIDEO_FLASH_FRAME) {
            videoFlash = !videoFlash;
        }
        flashFramesCount = (flashFramesCount + 1) % (VIDEO_FLASH_FRAME + 1);
    }

    public void readScreen() {
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            readLine(y);
        }
    }

    public void readLine(int y) {
        for (int x = 0; x < SCREEN_WIDTH; x++) {
            videoMemory[x][y] = bus.readMemoryNotContended(0x4000 + LINE_OFFSETS[y] + x);
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

    @Override
    public byte read(int portAddress) {
        // A zero in one of the five lowest bits means that the corresponding key is pressed.
        // If more than one address line is made low, the result is the logical AND of all single inputs

        byte result = (byte) 0xBF; // 1011 1111   // no EAR input
        if ((portAddress & 0xFE) == 0xFE) {
            int keyLine = 0;
            portAddress >>>= 8;
            while ((portAddress & 1) != 0) {
                portAddress >>>= 1;
                keyLine++;
            }

            // FE = 0  1111 1110
            // FD = 1  1111 1101
            // FB = 2  1111 1011
            // F7 = 3  1111 0111
            // EF = 4  1110 1111
            // DF = 5  1101 1111
            // BF = 6  1011 1111
            // 7F = 7  0111 1111
            result &= keymap[keyLine];
        }

        // LINE IN?
        return (byte) (result | ((bus.readData() & 1) << 6));
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
    public void onKeyEvent(KeyEvent e) {
        boolean pressed = e.getID() == KEY_PRESSED;
        if (!pressed && e.getID() != KEY_RELEASED) {
            return;
        }
        BiConsumer<Byte, Byte> keySet = pressed ? this::andKeyMap : this::orKeyMap;
        BiConsumer<Byte, Byte> keyUnset = pressed ? this::orKeyMap : this::andKeyMap;

        // shift / alt / ctrl are visible in modifiersEx only if pressed = true
        boolean symShift = (e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
        boolean shift = (e.getModifiersEx() & (KeyEvent.SHIFT_DOWN_MASK)) != 0;

        Byte[] command = CHAR_MAPPING.get(e.getKeyCode());
        if (command != null) {
            if (command[2] == 1 || (command[2] == -1 && shift)) {
                keySet.accept(KEY_SHIFT[0], KEY_SHIFT[1]);
            } else if (command[2] == 0 || !shift) {
                keyUnset.accept(KEY_SHIFT[0], KEY_SHIFT[1]);
            }
            if (command[3] == 1 || (command[3] == -1 && symShift)) {
                keySet.accept(KEY_SYM_SHIFT[0], KEY_SYM_SHIFT[1]);
            } else if (command[3] == 0 || !symShift) {
                keyUnset.accept(KEY_SYM_SHIFT[0], KEY_SYM_SHIFT[1]);
            }
            // TODO: shift/symshift are toggling for some reason
            keySet.accept(command[0], command[1]);
        } else {
            if (shift) {
                keySet.accept(KEY_SHIFT[0], KEY_SHIFT[1]);
            } else {
                keyUnset.accept(KEY_SHIFT[0], KEY_SHIFT[1]);
            }
            if (symShift) {
                keySet.accept(KEY_SYM_SHIFT[0], KEY_SYM_SHIFT[1]);
            } else {
                keyUnset.accept(KEY_SYM_SHIFT[0], KEY_SYM_SHIFT[1]);
            }
        }
    }

    private void andKeyMap(byte key, byte value) {
        keymap[key] &= (byte) ((~value) & 0xFF);
    }

    private void orKeyMap(byte key, byte value) {
        keymap[key] |= value;
    }

    /**
     * Computes address offsets for each line in the screen.
     * <p>
     * The Spectrum’s screen memory starts at 0x4000 so the most significant three bits of our address will always be 010.
     * The 5 least significant bits will always be the X (column) address. The 8 bits from 5-12 represent the pixel Y:
     * <p>
     * 15	14	13	12	11	10	9	8	7	6	5	4	3	2	1	0
     * 0	1	0	Y7	Y6	Y2	Y1	Y0	Y5	Y4	Y3	X4	X3	X2	X1	X0
     * <p>
     * This method sets all X bits to 0, and then sets the Y bits according to the line number.
     *
     * @return array of offsets
     */
    private static int[] computeLineOffsets() {
        final int[] result = new int[SCREEN_HEIGHT];
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            result[y] = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2);
        }
        return result;
    }
}
