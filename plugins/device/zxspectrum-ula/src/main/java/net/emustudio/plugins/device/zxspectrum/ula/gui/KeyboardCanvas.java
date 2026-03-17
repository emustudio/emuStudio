/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;

import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.SCREEN_IMAGE_WIDTH;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.ZOOM;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow.MARGIN;

/**
 * Host-ZX Keyboard mapping visual representation.
 */
public class KeyboardCanvas extends Canvas implements KeyboardDispatcher.OnKeyListener {
    // Minimum alpha % so the buttons start to be "interactive" (can be clicked on with mouse)
    public static final int INTERACTIVE_ALPHA_THRESHOLD = 10;
    private static final int STROKE_WIDTH = 3;

    private static final int bw = 42; // button width
    private static final int bh = 33; // button height
    private static final int bsw = 70; // backspace width
    private static final int tabw = 60; // tab width
    private static final int lshiftw = 55; // left shift width
    private static final int brakew = 270; // break width
    private static final int s = 5; // space between buttons
    private static final int arc = 15; // arc radius
    private static final int margin = 10;
    private static final int rshiftw = 3 * bw - 3 * s - margin + s / 2; // right shift width

    private static final int bwS = bw + s; // button width + space
    private static final int bhS = bh + s; // button height + space

    public static final int KEYBOARD_WIDTH = 13 * bwS + bsw + 10 + 10; // longest row
    public static final int KEYBOARD_HEIGHT = 5 * bhS + 2 * margin - s;

    private static final int X_OUTER = (int) ((ZOOM * SCREEN_IMAGE_WIDTH + 2 * MARGIN - KEYBOARD_WIDTH) / 2.0);
    private static final int X_INNER = X_OUTER + margin;
    private static final int Y_INNER = margin;

    private static final int Y_ROW_1 = Y_INNER + bhS;
    private static final int Y_ROW_2 = Y_INNER + 2 * bhS;
    private static final int Y_ROW_3 = Y_INNER + 3 * bhS;
    private static final int Y_ROW_4 = Y_INNER + 4 * bhS;
    private static final int SHIFT_KEY_ID = keyId((byte) 0, (byte) 1);
    private static final int SYM_SHIFT_KEY_ID = keyId((byte) 7, (byte) 2);
    private static final Shape KEYBOARD_OUTLINE = roundButton(X_OUTER, -STROKE_WIDTH, KEYBOARD_WIDTH, KEYBOARD_HEIGHT);

    private static final KeyboardButton[] BUTTONS = new KeyboardButton[]{
            // Row 0: Number row
            dec(col(0), Y_INNER),
            key(col(1), Y_INNER, 3, 1, "1", "EDIT", "!"),
            key(col(2), Y_INNER, 3, 2, "2", "CAPSL", "@"),
            key(col(3), Y_INNER, 3, 4, "3", "TRUE V.", "#"),
            key(col(4), Y_INNER, 3, 8, "4", "INV.V", "$"),
            key(col(5), Y_INNER, 3, 16, "5", "⇦", "%"),
            key(col(6), Y_INNER, 4, 16, "6", "⇩", "&"),
            key(col(7), Y_INNER, 4, 8, "7", "⇧", "'"),
            key(col(8), Y_INNER, 4, 4, "8", "⇨", "("),
            key(col(9), Y_INNER, 4, 2, "9", "GRAPH", ")"),
            key(col(10), Y_INNER, 4, 1, "0", "DELETE", "_"),
            dec(col(11), Y_INNER),
            dec(col(12), Y_INNER),
            dec(col(13), Y_INNER, bsw),

            // Row 1: QWERTY row
            dec(X_INNER, Y_ROW_1, tabw),
            key(tabCol(0), Y_ROW_1, 2, 1, "PLOT", "Q", "<="),
            key(tabCol(1), Y_ROW_1, 2, 2, "DRAW", "W", "<>"),
            key(tabCol(2), Y_ROW_1, 2, 4, "REM", "E", ">="),
            key(tabCol(3), Y_ROW_1, 2, 8, "RUN", "R", "<"),
            key(tabCol(4), Y_ROW_1, 2, 16, "RAND", "T", ">"),
            key(tabCol(5), Y_ROW_1, 5, 16, "RETURN", "Y", "AND"),
            key(tabCol(6), Y_ROW_1, 5, 8, "IF", "U", "OR"),
            key(tabCol(7), Y_ROW_1, 5, 4, "INPUT", "I", "AT"),
            key(tabCol(8), Y_ROW_1, 5, 2, "POKE", "O", ";"),
            key(tabCol(9), Y_ROW_1, 5, 1, "PRINT", "P", "\""),
            dec(tabCol(10), Y_ROW_1),
            dec(tabCol(11), Y_ROW_1),
            key(enterPolygon(), 6, 1, "↵", "↵", "↵"),

            // Row 2: ASDF row
            dec(X_INNER, Y_ROW_2, bsw),
            key(capsCol(0), Y_ROW_2, 1, 1, "NEW", "A", "STOP"),
            key(capsCol(1), Y_ROW_2, 1, 2, "SAVE", "S", "NOT"),
            key(capsCol(2), Y_ROW_2, 1, 4, "DIM", "D", "STEP"),
            key(capsCol(3), Y_ROW_2, 1, 8, "FOR", "F", "TO"),
            key(capsCol(4), Y_ROW_2, 1, 16, "GOTO", "G", "THEN"),
            key(capsCol(5), Y_ROW_2, 6, 16, "GOSUB", "H", "↑"),
            key(capsCol(6), Y_ROW_2, 6, 8, "LOAD", "J", "-"),
            key(capsCol(7), Y_ROW_2, 6, 4, "LIST", "K", "+"),
            key(capsCol(8), Y_ROW_2, 6, 2, "LET", "L", "="),
            dec(capsCol(9), Y_ROW_2),
            dec(capsCol(10), Y_ROW_2),
            dec(capsCol(11), Y_ROW_2),

            // Row 3: ZXCV row
            toggleKey(X_INNER, Y_ROW_3, lshiftw, 0, 1, "SHIFT"),
            dec(shiftCol(0), Y_ROW_3),
            key(shiftCol(1), Y_ROW_3, 0, 2, "COPY", "Z", ":"),
            key(shiftCol(2), Y_ROW_3, 0, 4, "CLEAR", "X", "£"),
            key(shiftCol(3), Y_ROW_3, 0, 8, "CONT", "C", "?"),
            key(shiftCol(4), Y_ROW_3, 0, 16, "CLS", "V", "/"),
            key(shiftCol(5), Y_ROW_3, 7, 16, "BORDER", "B", "*"),
            key(shiftCol(6), Y_ROW_3, 7, 8, "NEXT", "N", ","),
            key(shiftCol(7), Y_ROW_3, 7, 4, "PAUSE", "M", "."),
            dec(shiftCol(8), Y_ROW_3),
            dec(shiftCol(9), Y_ROW_3),
            dec(shiftCol(10), Y_ROW_3),
            toggleKey(shiftCol(11), Y_ROW_3, rshiftw, 0, 1, "SHIFT"),

            // Row 4: Bottom row
            toggleKey(X_INNER, Y_ROW_4, tabw, 7, 2, "SYM"),
            dec(X_INNER + tabw + s, Y_ROW_4),
            dec(X_INNER + tabw + bw + 2 * s, Y_ROW_4, tabw),
            key(X_INNER + 2 * tabw + bw + 4 * s, Y_ROW_4, brakew, 7, 1),
            dec(X_INNER + 2 * tabw + bw + brakew + 6 * s, Y_ROW_4, tabw),
            toggleKey(X_INNER + 3 * tabw + bw + brakew + 7 * s, Y_ROW_4, bw, 7, 2, "SYM"),
            dec(X_INNER + 3 * tabw + 2 * bw + brakew + 8 * s, Y_ROW_4),
            dec(X_INNER + 3 * tabw + 3 * bw + brakew + 9 * s, Y_ROW_4, tabw)
    };
    private int alpha;

    private final Set<Integer> activeMouseKeys = new HashSet<>();
    private final KeyboardPainter painter;

    private boolean hostSymShift = false;
    private boolean hostShift = false;
    private KeyboardButton pressedMouseButton = null;

    public KeyboardCanvas(int alpha) {
        this.alpha = alpha;
        this.painter = new KeyboardPainter(alpha, BUTTONS, KEYBOARD_OUTLINE, activeMouseKeys);
    }

    @Override
    public boolean onKeyEvent(KeyEvent e) {
        boolean pressed = e.getID() == KEY_PRESSED;
        if (!pressed && e.getID() != KEY_RELEASED) {
            return false;
        }

        hostSymShift = (e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
        hostShift = (e.getModifiersEx() & (KeyEvent.SHIFT_DOWN_MASK)) != 0;
        return true;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public boolean isInteractiveInvisible() {
        return alpha <= INTERACTIVE_ALPHA_THRESHOLD;
    }

    public boolean handleMousePressed(int x, int y, ULA ula) {
        if (isInteractiveInvisible()) {
            return false;
        }

        KeyboardButton button = findButton(x, y);
        if (button == null) {
            return false;
        }

        // Release previously pressed non-toggle button
        boolean changed = false;
        if (pressedMouseButton != null) {
            changed = activeMouseKeys.remove(pressedMouseButton.keyId);
            if (changed) {
                ula.releaseKey(pressedMouseButton.keyLine, pressedMouseButton.keyValue);
            }
            pressedMouseButton = null;
        }

        if (button.toggle) {
            if (activeMouseKeys.remove(button.keyId)) {
                ula.releaseKey(button.keyLine, button.keyValue);
            } else {
                activeMouseKeys.add(button.keyId);
                ula.pressKey(button.keyLine, button.keyValue);
            }
            changed = true;
        } else {
            if (activeMouseKeys.add(button.keyId)) {
                ula.pressKey(button.keyLine, button.keyValue);
                changed = true;
            }
            pressedMouseButton = button;
        }
        return changed;
    }

    public boolean handleMouseReleased(ULA ula) {
        if (pressedMouseButton == null) {
            return false;
        }
        KeyboardButton button = pressedMouseButton;
        pressedMouseButton = null;
        if (activeMouseKeys.remove(button.keyId)) {
            ula.releaseKey(button.keyLine, button.keyValue);
            return true;
        }
        return false;
    }

    public void releaseMouseKeys(ULA ula) {
        pressedMouseButton = null;
        for (KeyboardButton button : BUTTONS) {
            if (button.interactive && activeMouseKeys.remove(button.keyId)) {
                ula.releaseKey(button.keyLine, button.keyValue);
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        painter.setAlpha(alpha);
        painter.setShiftActive(isShiftActive());
        painter.setSymShiftActive(isSymShiftActive());
        painter.drawKeyboard((Graphics2D) g);
    }

    private KeyboardButton findButton(int x, int y) {
        for (KeyboardButton button : BUTTONS) {
            if (button.interactive && button.shape.contains(x, y)) {
                return button;
            }
        }
        return null;
    }

    private boolean isShiftActive() {
        return hostShift || activeMouseKeys.contains(SHIFT_KEY_ID);
    }

    private boolean isSymShiftActive() {
        return hostSymShift || activeMouseKeys.contains(SYM_SHIFT_KEY_ID);
    }

    private static Shape roundButton(int x, int y, int width, int height) {
        return new RoundRectangle2D.Double(x, y, width, height, arc, arc);
    }

    private static Shape enterPolygon() {
        int x0 = X_INNER + 12 * bwS + tabw + s;
        int y0 = Y_ROW_1;
        return new Polygon(
                new int[]{x0, x0 + tabw - 2 * s, x0 + tabw - 2 * s, x0 + 2 * s, x0 + 2 * s, x0},
                new int[]{y0, y0, y0 + 2 * bh + s, y0 + 2 * bh + s, y0 + bh, y0 + bh},
                6
        );
    }

    // Column x-position helpers for each row layout
    private static int col(int n) { return X_INNER + n * bwS; }
    private static int tabCol(int n) { return X_INNER + tabw + s + n * bwS; }
    private static int capsCol(int n) { return X_INNER + bsw + s + n * bwS; }
    private static int shiftCol(int n) { return X_INNER + lshiftw + s + n * bwS; }

    // Button factory methods
    private static KeyboardButton dec(int x, int y) {
        return new KeyboardButton(roundButton(x, y, bw, bh), false, false, (byte) 0, (byte) 0, false, null, null, null);
    }

    private static KeyboardButton dec(int x, int y, int width) {
        return new KeyboardButton(roundButton(x, y, width, bh), false, false, (byte) 0, (byte) 0, false, null, null, null);
    }

    private static KeyboardButton key(int x, int y, int keyLine, int keyValue,
                                      String noShiftLabel, String shiftLabel, String symShiftLabel) {
        return new KeyboardButton(
                roundButton(x, y, bw, bh), true, true, (byte) keyLine, (byte) keyValue, false,
                noShiftLabel, shiftLabel, symShiftLabel
        );
    }

    private static KeyboardButton key(Shape shape, int keyLine, int keyValue,
                                      String noShiftLabel, String shiftLabel, String symShiftLabel) {
        return new KeyboardButton(
                shape, true, true, (byte) keyLine, (byte) keyValue, false,
                noShiftLabel, shiftLabel, symShiftLabel
        );
    }

    private static KeyboardButton key(int x, int y, int width, int keyLine, int keyValue) {
        return new KeyboardButton(
                roundButton(x, y, width, bh), true, true, (byte) keyLine, (byte) keyValue, false, null, null, null
        );
    }

    private static KeyboardButton toggleKey(int x, int y, int width, int keyLine, int keyValue, String label) {
        return new KeyboardButton(
                roundButton(x, y, width, bh), true, true, (byte) keyLine, (byte) keyValue, true, label, label, label
        );
    }

    private static int keyId(byte keyLine, byte keyValue) {
        return ((keyLine & 0xFF) << 8) | (keyValue & 0xFF);
    }

    static final class KeyboardButton {
        final Shape shape;
        final Rectangle labelBounds;
        final boolean filled;
        final boolean interactive;
        final byte keyLine;
        final byte keyValue;
        final boolean toggle;
        final int keyId;
        final String noShiftLabel;
        final String shiftLabel;
        final String symShiftLabel;

        KeyboardButton(Shape shape, boolean filled, boolean interactive, byte keyLine, byte keyValue,
                               boolean toggle, String noShiftLabel, String shiftLabel, String symShiftLabel) {
            this.shape = shape;
            this.labelBounds = shape.getBounds();
            this.filled = filled;
            this.interactive = interactive;
            this.keyLine = keyLine;
            this.keyValue = keyValue;
            this.toggle = toggle;
            this.keyId = KeyboardCanvas.keyId(keyLine, keyValue);
            this.noShiftLabel = noShiftLabel;
            this.shiftLabel = shiftLabel;
            this.symShiftLabel = symShiftLabel;
        }

        boolean hasLabel() {
            return noShiftLabel != null;
        }

        String label(boolean symShift, boolean shift) {
            if (symShift) {
                return symShiftLabel;
            }
            if (shift) {
                return shiftLabel;
            }
            return noShiftLabel;
        }
    }
}
