/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.ULA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;

import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.SCREEN_IMAGE_HEIGHT;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.SCREEN_IMAGE_WIDTH;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.ZOOM;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow.MARGIN;

/**
 * Host-ZX Keyboard mapping visual representation.
 */
public class KeyboardCanvas extends JComponent implements KeyboardDispatcher.OnKeyListener {
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

    public static final int KEYBOARD_WIDTH = 13 * (bw + s) + bsw + 10 + 10;
    public static final int KEYBOARD_HEIGHT = 5 * (bh + s) + 2 * margin - s;
    public static final int INTERACTIVE_ALPHA_THRESHOLD = 10;
    public static final int OVERLAY_TOP = (int) (ZOOM * SCREEN_IMAGE_HEIGHT - KEYBOARD_HEIGHT + MARGIN);

    private static final int X_SHIFT = (int) ((ZOOM * SCREEN_IMAGE_WIDTH + 2 * MARGIN - KEYBOARD_WIDTH) / 2.0);
    private static final int X_SHIFT_L = X_SHIFT + margin;
    private static final int Y_SHIFT_T = margin;
    private static final int STROKE_WIDTH = 3;
    private static final int Y_ROW_1 = Y_SHIFT_T + bh + s;
    private static final int Y_ROW_2 = Y_SHIFT_T + 2 * (bh + s);
    private static final int Y_ROW_3 = Y_SHIFT_T + 3 * (bh + s);
    private static final int Y_ROW_4 = Y_SHIFT_T + 4 * (bh + s);
    private static final int SHIFT_GROUP = groupId(0, 1);
    private static final int SYM_SHIFT_GROUP = groupId(7, 2);
    private static final Shape KEYBOARD_OUTLINE = roundButton(X_SHIFT, -STROKE_WIDTH, KEYBOARD_WIDTH, KEYBOARD_HEIGHT);

    private static final KeyboardButton[] BUTTONS = new KeyboardButton[]{
            decorativeButton(roundButton(X_SHIFT_L, Y_SHIFT_T, bw, bh)),
            keyButton(roundButton(X_SHIFT_L + (bw + s), Y_SHIFT_T, bw, bh), 3, 1, false, "1", "EDIT", "!"),
            keyButton(roundButton(X_SHIFT_L + 2 * (bw + s), Y_SHIFT_T, bw, bh), 3, 2, false, "2", "CAPSL", "@"),
            keyButton(roundButton(X_SHIFT_L + 3 * (bw + s), Y_SHIFT_T, bw, bh), 3, 4, false, "3", "TRUE V.", "#"),
            keyButton(roundButton(X_SHIFT_L + 4 * (bw + s), Y_SHIFT_T, bw, bh), 3, 8, false, "4", "INV.V", "$"),
            keyButton(roundButton(X_SHIFT_L + 5 * (bw + s), Y_SHIFT_T, bw, bh), 3, 16, false, "5", "⇦", "%"),
            keyButton(roundButton(X_SHIFT_L + 6 * (bw + s), Y_SHIFT_T, bw, bh), 4, 16, false, "6", "⇩", "&"),
            keyButton(roundButton(X_SHIFT_L + 7 * (bw + s), Y_SHIFT_T, bw, bh), 4, 8, false, "7", "⇧", "'"),
            keyButton(roundButton(X_SHIFT_L + 8 * (bw + s), Y_SHIFT_T, bw, bh), 4, 4, false, "8", "⇨", "("),
            keyButton(roundButton(X_SHIFT_L + 9 * (bw + s), Y_SHIFT_T, bw, bh), 4, 2, false, "9", "GRAPH", ")"),
            keyButton(roundButton(X_SHIFT_L + 10 * (bw + s), Y_SHIFT_T, bw, bh), 4, 1, false, "0", "DELETE", "_"),
            decorativeButton(roundButton(X_SHIFT_L + 11 * (bw + s), Y_SHIFT_T, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + 12 * (bw + s), Y_SHIFT_T, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + 13 * (bw + s), Y_SHIFT_T, bsw, bh)),

            decorativeButton(roundButton(X_SHIFT_L, Y_ROW_1, tabw, bh)),
            keyButton(roundButton(X_SHIFT_L + tabw + s, Y_ROW_1, bw, bh), 2, 1, false, "PLOT", "Q", "<="),
            keyButton(roundButton(X_SHIFT_L + tabw + s + (bw + s), Y_ROW_1, bw, bh), 2, 2, false, "DRAW", "W", "<>"),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 2 * (bw + s), Y_ROW_1, bw, bh), 2, 4, false, "REM", "E", ">="),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 3 * (bw + s), Y_ROW_1, bw, bh), 2, 8, false, "RUN", "R", "<"),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 4 * (bw + s), Y_ROW_1, bw, bh), 2, 16, false, "RAND", "T", ">"),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 5 * (bw + s), Y_ROW_1, bw, bh), 5, 16, false, "RETURN", "Y", "AND"),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 6 * (bw + s), Y_ROW_1, bw, bh), 5, 8, false, "IF", "U", "OR"),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 7 * (bw + s), Y_ROW_1, bw, bh), 5, 4, false, "INPUT", "I", "AT"),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 8 * (bw + s), Y_ROW_1, bw, bh), 5, 2, false, "POKE", "O", ";"),
            keyButton(roundButton(X_SHIFT_L + tabw + s + 9 * (bw + s), Y_ROW_1, bw, bh), 5, 1, false, "PRINT", "P", "\""),
            decorativeButton(roundButton(X_SHIFT_L + tabw + s + 10 * (bw + s), Y_ROW_1, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + tabw + s + 11 * (bw + s), Y_ROW_1, bw, bh)),
            keyButton(enterPolygon(), 6, 1, false, "↵", "↵", "↵"),

            decorativeButton(roundButton(X_SHIFT_L, Y_ROW_2, bsw, bh)),
            keyButton(roundButton(X_SHIFT_L + bsw + s, Y_ROW_2, bw, bh), 1, 1, false, "NEW", "A", "STOP"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + (bw + s), Y_ROW_2, bw, bh), 1, 2, false, "SAVE", "S", "NOT"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + 2 * (bw + s), Y_ROW_2, bw, bh), 1, 4, false, "DIM", "D", "STEP"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + 3 * (bw + s), Y_ROW_2, bw, bh), 1, 8, false, "FOR", "F", "TO"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + 4 * (bw + s), Y_ROW_2, bw, bh), 1, 16, false, "GOTO", "G", "THEN"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + 5 * (bw + s), Y_ROW_2, bw, bh), 6, 16, false, "GOSUB", "H", "↑"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + 6 * (bw + s), Y_ROW_2, bw, bh), 6, 8, false, "LOAD", "J", "-"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + 7 * (bw + s), Y_ROW_2, bw, bh), 6, 4, false, "LIST", "K", "+"),
            keyButton(roundButton(X_SHIFT_L + bsw + s + 8 * (bw + s), Y_ROW_2, bw, bh), 6, 2, false, "LET", "L", "="),
            decorativeButton(roundButton(X_SHIFT_L + bsw + s + 9 * (bw + s), Y_ROW_2, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + bsw + s + 10 * (bw + s), Y_ROW_2, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + bsw + s + 11 * (bw + s), Y_ROW_2, bw, bh)),

            keyButton(roundButton(X_SHIFT_L, Y_ROW_3, lshiftw, bh), 0, 1, true, "SHIFT", "SHIFT", "SHIFT"),
            decorativeButton(roundButton(X_SHIFT_L + lshiftw + s, Y_ROW_3, bw, bh)),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + (bw + s), Y_ROW_3, bw, bh), 0, 2, false, "COPY", "Z", ":"),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + 2 * (bw + s), Y_ROW_3, bw, bh), 0, 4, false, "CLEAR", "X", "£"),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + 3 * (bw + s), Y_ROW_3, bw, bh), 0, 8, false, "CONT", "C", "?"),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + 4 * (bw + s), Y_ROW_3, bw, bh), 0, 16, false, "CLS", "V", "/"),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + 5 * (bw + s), Y_ROW_3, bw, bh), 7, 16, false, "BORDER", "B", "*"),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + 6 * (bw + s), Y_ROW_3, bw, bh), 7, 8, false, "NEXT", "N", ","),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + 7 * (bw + s), Y_ROW_3, bw, bh), 7, 4, false, "PAUSE", "M", "."),
            decorativeButton(roundButton(X_SHIFT_L + lshiftw + s + 8 * (bw + s), Y_ROW_3, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + lshiftw + s + 9 * (bw + s), Y_ROW_3, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + lshiftw + s + 10 * (bw + s), Y_ROW_3, bw, bh)),
            keyButton(roundButton(X_SHIFT_L + lshiftw + s + 11 * (bw + s), Y_ROW_3, rshiftw, bh), 0, 1, true, "SHIFT", "SHIFT", "SHIFT"),

            keyButton(roundButton(X_SHIFT_L, Y_ROW_4, tabw, bh), 7, 2, true, "SYM", "SYM", "SYM"),
            decorativeButton(roundButton(X_SHIFT_L + tabw + s, Y_ROW_4, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + tabw + bw + 2 * s, Y_ROW_4, tabw, bh)),
            keyButton(roundButton(X_SHIFT_L + 2 * tabw + bw + 4 * s, Y_ROW_4, brakew, bh), 7, 1, false),
            decorativeButton(roundButton(X_SHIFT_L + 2 * tabw + bw + brakew + 6 * s, Y_ROW_4, tabw, bh)),
            keyButton(roundButton(X_SHIFT_L + 3 * tabw + bw + brakew + 7 * s, Y_ROW_4, bw, bh), 7, 2, true, "SYM", "SYM", "SYM"),
            decorativeButton(roundButton(X_SHIFT_L + 3 * tabw + 2 * bw + brakew + 8 * s, Y_ROW_4, bw, bh)),
            decorativeButton(roundButton(X_SHIFT_L + 3 * tabw + 3 * bw + brakew + 9 * s, Y_ROW_4, tabw, bh))
    };
    private final BasicStroke outlineStroke = new BasicStroke(STROKE_WIDTH);
    private final Color usableButtonColor;
    private final Color activeButtonColor;
    private final Color outlineColor;
    private final Color brightColor;
    private int alpha;

    private final Set<Integer> activeMouseGroups = new HashSet<>();

    private boolean hostSymShift = false;
    private boolean hostShift = false;
    private int pressedMouseGroup = -1;

    public KeyboardCanvas(int alpha) {
        setDoubleBuffered(true);
        this.alpha = alpha;
        this.usableButtonColor = new Color(
                Color.LIGHT_GRAY.getRed(),
                Color.LIGHT_GRAY.getGreen(),
                Color.LIGHT_GRAY.getBlue(), alpha);
        this.activeButtonColor = new Color(148, 178, 209, alpha);
        this.outlineColor = new Color(0, 0, 0, alpha);
        this.brightColor = new Color(255, 255, 255, alpha);
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

        boolean changed = releasePressedMouseGroup(ula);
        if (button.toggle) {
            changed |= toggleMouseGroup(button, ula);
        } else {
            changed |= activateMouseGroup(button, ula);
            pressedMouseGroup = button.group;
        }
        return changed;
    }

    public boolean handleMouseReleased(ULA ula) {
        return releasePressedMouseGroup(ula);
    }

    public boolean releaseMouseKeys(ULA ula) {
        boolean changed = releasePressedMouseGroup(ula);
        if (activeMouseGroups.isEmpty()) {
            return changed;
        }

        Integer[] groups = activeMouseGroups.toArray(new Integer[0]);
        for (Integer group : groups) {
            changed |= deactivateMouseGroup(group, ula);
        }
        return changed;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        drawKeyboard(g2d);
        drawActiveKeys(g2d);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.setStroke(outlineStroke);
        g2d.setColor(adjustAlpha(brightColor));
        FontMetrics fontMetrics = g2d.getFontMetrics();
        boolean symShiftActive = isSymShiftActive();
        boolean shiftActive = isShiftActive();

        for (KeyboardButton button : BUTTONS) {
            if (!button.hasLabel()) {
                continue;
            }
            String text = button.label(symShiftActive, shiftActive);
            g2d.setColor(adjustAlpha(outlineColor));
            g2d.drawString(text, labelX(button, fontMetrics, text), labelY(button, fontMetrics));
        }
    }

    private void drawActiveKeys(Graphics2D g) {
        if (activeMouseGroups.isEmpty()) {
            return;
        }

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(2.0f));
        for (KeyboardButton button : BUTTONS) {
            if (button.interactive && activeMouseGroups.contains(button.group)) {
                g.setColor(adjustAlpha(activeButtonColor));
                g.fill(button.shape);
                g.setColor(adjustAlpha(outlineColor));
                g.draw(button.shape);
            }
        }
        g.setStroke(oldStroke);
    }

    private void drawKeyboard(Graphics2D g) {
        BasicStroke stroke = new BasicStroke(2.0f);
        Color adjUsableButtonColor = adjustAlpha(usableButtonColor);
        Color adjOutlineColor = adjustAlpha(outlineColor);

        g.setStroke(stroke);
        g.setColor(adjOutlineColor);
        g.draw(KEYBOARD_OUTLINE);
        for (KeyboardButton button : BUTTONS) {
            if (button.filled) {
                g.setColor(adjUsableButtonColor);
                g.fill(button.shape);
            }
            g.setColor(adjOutlineColor);
            g.draw(button.shape);
        }
    }

    private Color adjustAlpha(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private int labelX(KeyboardButton button, FontMetrics fontMetrics, String text) {
        return (int) Math.round(button.labelBounds.getCenterX() - fontMetrics.stringWidth(text) / 2.0);
    }

    private int labelY(KeyboardButton button, FontMetrics fontMetrics) {
        return (int) Math.round(
                button.labelBounds.getCenterY() + (fontMetrics.getAscent() - fontMetrics.getDescent()) / 2.0
        );
    }

    private KeyboardButton findButton(int x, int y) {
        for (KeyboardButton button : BUTTONS) {
            if (button.interactive && button.shape.contains(x, y)) {
                return button;
            }
        }
        return null;
    }

    private boolean activateMouseGroup(KeyboardButton button, ULA ula) {
        if (activeMouseGroups.add(button.group)) {
            ula.pressOverlayKey(button.keyLine, button.keyValue);
            return true;
        }
        return false;
    }

    private boolean toggleMouseGroup(KeyboardButton button, ULA ula) {
        if (activeMouseGroups.contains(button.group)) {
            return deactivateMouseGroup(button.group, ula);
        }
        return activateMouseGroup(button, ula);
    }

    private boolean deactivateMouseGroup(int group, ULA ula) {
        if (activeMouseGroups.remove(group)) {
            ula.releaseOverlayKey(groupLine(group), groupValue(group));
            return true;
        }
        return false;
    }

    private boolean releasePressedMouseGroup(ULA ula) {
        if (pressedMouseGroup == -1) {
            return false;
        }

        int group = pressedMouseGroup;
        pressedMouseGroup = -1;
        return deactivateMouseGroup(group, ula);
    }

    private boolean isShiftActive() {
        return hostShift || activeMouseGroups.contains(SHIFT_GROUP);
    }

    private boolean isSymShiftActive() {
        return hostSymShift || activeMouseGroups.contains(SYM_SHIFT_GROUP);
    }

    private static Shape roundButton(int x, int y, int width, int height) {
        return new RoundRectangle2D.Double(x, y, width, height, arc, arc);
    }

    private static Shape enterPolygon() {
        int x0 = X_SHIFT_L + 12 * (bw + s) + tabw + s;
        int y0 = Y_ROW_1;
        return new Polygon(
                new int[]{x0, x0 + tabw - 2 * s, x0 + tabw - 2 * s, x0 + 2 * s, x0 + 2 * s, x0},
                new int[]{y0, y0, y0 + 2 * bh + s, y0 + 2 * bh + s, y0 + bh, y0 + bh},
                6
        );
    }

    private static KeyboardButton decorativeButton(Shape shape) {
        return new KeyboardButton(shape, false, false, (byte) 0, (byte) 0, false, null, null, null);
    }

    private static KeyboardButton keyButton(Shape shape, int keyLine, int keyValue, boolean toggle) {
        return new KeyboardButton(shape, true, true, (byte) keyLine, (byte) keyValue, toggle, null, null, null);
    }

    private static KeyboardButton keyButton(Shape shape, int keyLine, int keyValue, boolean toggle,
                                            String noShiftLabel, String shiftLabel, String symShiftLabel) {
        return new KeyboardButton(
                shape, true, true, (byte) keyLine, (byte) keyValue, toggle, noShiftLabel, shiftLabel, symShiftLabel
        );
    }

    private static int groupId(int keyLine, int keyValue) {
        return ((keyLine & 0xFF) << 8) | (keyValue & 0xFF);
    }

    private static byte groupLine(int group) {
        return (byte) ((group >>> 8) & 0xFF);
    }

    private static byte groupValue(int group) {
        return (byte) (group & 0xFF);
    }

    private static final class KeyboardButton {
        private final Shape shape;
        private final Rectangle labelBounds;
        private final boolean filled;
        private final boolean interactive;
        private final byte keyLine;
        private final byte keyValue;
        private final boolean toggle;
        private final int group;
        private final String noShiftLabel;
        private final String shiftLabel;
        private final String symShiftLabel;

        private KeyboardButton(Shape shape, boolean filled, boolean interactive, byte keyLine, byte keyValue,
                               boolean toggle, String noShiftLabel, String shiftLabel, String symShiftLabel) {
            this.shape = shape;
            this.labelBounds = shape.getBounds();
            this.filled = filled;
            this.interactive = interactive;
            this.keyLine = keyLine;
            this.keyValue = keyValue;
            this.toggle = toggle;
            this.group = interactive ? groupId(keyLine, keyValue) : -1;
            this.noShiftLabel = noShiftLabel;
            this.shiftLabel = shiftLabel;
            this.symShiftLabel = symShiftLabel;
        }

        private boolean hasLabel() {
            return noShiftLabel != null;
        }

        private String label(boolean symShift, boolean shift) {
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
