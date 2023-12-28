package net.emustudio.plugins.device.zxspectrum.ula.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.font.GlyphVector;

import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.SCREEN_IMAGE_WIDTH;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.ZOOM;
import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow.MARGIN;

public class KeyboardCanvas extends JComponent implements Keyboard.OnKeyListener {
    private final static int bw = 45; // button width
    private final static int bh = 33; // button height
    private final static int bsw = 70; // backspace width
    private final static int tabw = 60; // tab width
    private final static int lshiftw = 55; // left shift width
    private final static int brakew = 300; // break width
    private final static int s = 5; // space between buttons
    private final static int arc = 15; // arc radius
    private final static int margin = 10;
    private final static int rshiftw = 3 * bw - 3 * s - margin; // right shift width

    public final static int KEYBOARD_WIDTH = 13 * (bw + s) + 10 + bsw + 10;
    public final static int KEYBOARD_HEIGHT = 5 * (bh + s) + 2 * margin;

    private final static int X_SHIFT = (int) ((ZOOM * SCREEN_IMAGE_WIDTH + 2 * MARGIN - KEYBOARD_WIDTH) / 2.0);
    private final static int X_SHIFT_L = X_SHIFT + margin;
    private final static int Y_SHIFT_T = margin;

    private final static Color USABLE_BUTTON_COLOR = Color.LIGHT_GRAY;


    private final static double[][] KEY_MAP = new double[][]{
            new double[]{bw + s + bw / 2.0, bh}, // 1
            new double[]{bw + s, 0}, // 2
            new double[]{bw + s, 0}, // 3
            new double[]{bw + s, 0}, // 4
            new double[]{bw + s, 0}, // 5
            new double[]{bw + s, 0}, // 6
            new double[]{bw + s, 0}, // 7
            new double[]{bw + s, 0}, // 8
            new double[]{bw + s, 0}, // 9
            new double[]{bw + s, 0}, // 0,
            new double[]{-(11 * bw + s) + tabw + s, bh + s}, // Q
            new double[]{bw + s, 0}, // W
            new double[]{bw + s, 0}, // E
            new double[]{bw + s, 0}, // R
            new double[]{bw + s, 0}, // T
            new double[]{bw + s, 0}, // Y
            new double[]{bw + s, 0}, // U
            new double[]{bw + s, 0}, // I
            new double[]{bw + s, 0}, // O
            new double[]{bw + s, 0}, // P
            new double[]{-(10 * bw + s) - tabw + bsw + s, bh + s}, // A
            new double[]{bw + s, 0}, // S
            new double[]{bw + s, 0}, // D
            new double[]{bw + s, 0}, // F
            new double[]{bw + s, 0}, // G
            new double[]{bw + s, 0}, // H
            new double[]{bw + s, 0}, // J
            new double[]{bw + s, 0}, // K
            new double[]{bw + s, 0}, // L
            new double[]{(bw + s) * 4, 0}, // ENTER
            new double[]{-(bw + s) * 12 - bsw - bw / 2.0 + lshiftw / 2.0 - s, bh + s}, // LSHIFT
            new double[]{lshiftw / 2.0 + 2 * s + bw + bw / 2.0, 0}, // Z
            new double[]{bw + s, 0}, // X
            new double[]{bw + s, 0}, // C
            new double[]{bw + s, 0}, // V
            new double[]{bw + s, 0}, // B
            new double[]{bw + s, 0}, // N
            new double[]{bw + s, 0}, // M
            new double[]{4 * (bw + s) - bw / 2.0 + rshiftw / 2.0, 0}, // RSHIFT
            new double[]{-(bw + s) * 12 - rshiftw / 2.0 + tabw / 2.0 - 2 * s, bh + s}, // LCTRL
            new double[]{tabw / 2.0 + s + bw + s + tabw + 2 * s + brakew + 2 * s + tabw + s + bw / 2.0, 0}, // RCTRL
    };

    private final static String[] NO_SHIFT_LABELS = new String[]{
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
            "PLOT", "DRAW", "REM", "RUN", "RAND", "RETURN", "IF", "INPUT", "POKE", "PRINT",
            "NEW", "SAVE", "DIM", "FOR", "GOTO", "GOSUB", "LOAD", "LIST", "LET", "↵",
            "SHIFT", "COPY", "CLEAR", "CONT", "CLS", "BORDER", "NEXT", "PAUSE", "SHIFT", "SYM", "SYM"
    };

    private final static String[] SHIFT_LABELS = new String[]{
            "EDIT", "CAPSL", "TRUE V.", "INV.V", "⇦", "⇩", "⇧", "⇨", "GRAPH", "DELETE",
            "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
            "A", "S", "D", "F", "G", "H", "J", "K", "L", "↵",
            "SHIFT", "Z", "X", "C", "V", "B", "N", "M", "SHIFT", "SYM", "SYM"
    };

    private final static String[] SYM_SHIFT_LABELS = new String[]{
            "!", "@", "#", "$", "%", "&", "'", "(", ")", "_",
            "<=", "<>", ">=", "<", ">", "AND", "OR", "AT", ";", "\"",
            "STOP", "NOT", "STEP", "TO", "THEN", "↑", "-", "+", "=", "↵",
            "SHIFT", ":", "£", "?", "/", "*", ",", ".", "SHIFT", "SYM", "SYM"
    };

    private final BasicStroke outlineStroke = new BasicStroke(3.0f);

    private boolean symShift = false;
    private boolean shift = false;

    public KeyboardCanvas(Keyboard keyboard) {
        setDoubleBuffered(true);
        keyboard.addOnKeyListener(this);
    }

    @Override
    public void onKeyDown(KeyEvent evt) {
        int keyCode = evt.getExtendedKeyCode();
        if (keyCode == KeyEvent.VK_CONTROL) {
            symShift = true;
        } else if (keyCode == KeyEvent.VK_SHIFT) {
            shift = true;
        }
        repaint();
    }

    @Override
    public void onKeyUp(KeyEvent evt) {
        int keyCode = evt.getExtendedKeyCode();
        if (keyCode == KeyEvent.VK_CONTROL) {
            symShift = false;
        } else if (evt.getKeyCode() == KeyEvent.VK_SHIFT) {
            shift = false;
        }
        repaint();
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        // we must set RenderingHints before any drawing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawKeyboard(g2d);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.setStroke(outlineStroke);
        g2d.setColor(Color.WHITE);
        g2d.translate(X_SHIFT_L, 0);

        for (int i = 0; i < KEY_MAP.length; i++) {
            String text;
            if (symShift) {
                text = SYM_SHIFT_LABELS[i];
            } else if (shift) {
                text = SHIFT_LABELS[i];
            } else {
                text = NO_SHIFT_LABELS[i];
            }

            GlyphVector glyphVector = g2d.getFont().createGlyphVector(g2d.getFontRenderContext(), text);
            Shape textShape = glyphVector.getOutline();

            int sw = g2d.getFontMetrics().stringWidth(text);

            g2d.translate(KEY_MAP[i][0] - sw / 2.0, KEY_MAP[i][1]);
            g2d.setColor(Color.BLACK);
            g2d.fill(textShape);
            g2d.translate(sw / 2.0, 0);
        }
    }

    private void drawKeyboard(Graphics2D g) {
        BasicStroke stroke = new BasicStroke(2.0f);


        // keyboard shape
        g.setStroke(stroke);
        g.drawRoundRect(X_SHIFT, 0, KEYBOARD_WIDTH, KEYBOARD_HEIGHT, arc, arc);

        // top row
        for (int i = 0; i < 13; i++) {
            if (i >= 1 && i <= 10) {
                g.setColor(USABLE_BUTTON_COLOR);
                g.fillRoundRect(X_SHIFT_L + i * (bw + s), Y_SHIFT_T, bw, bh, arc, arc);
                g.setColor(Color.BLACK);
            }
            g.drawRoundRect(X_SHIFT_L + i * (bw + s), Y_SHIFT_T, bw, bh, arc, arc);
        }

        // backspace
        g.drawRoundRect(X_SHIFT_L + 13 * (bw + s), Y_SHIFT_T, bsw, bh, arc, arc);

        // tab
        int y1 = Y_SHIFT_T + bh + s;
        g.drawRoundRect(X_SHIFT_L, y1, tabw, bh, arc, arc);
        for (int i = 0; i < 12; i++) {
            if (i < 10) {
                g.setColor(USABLE_BUTTON_COLOR);
                g.fillRoundRect(X_SHIFT_L + i * (bw + s) + tabw + s, y1, bw, bh, arc, arc);
                g.setColor(Color.BLACK);
            }
            g.drawRoundRect(X_SHIFT_L + i * (bw + s) + tabw + s, y1, bw, bh, arc, arc);
        }

        // enter
        int x0 = X_SHIFT_L + 12 * (bw + s) + tabw + s;
        int y0 = Y_SHIFT_T + bh + s;
        Polygon enterPolygon = new Polygon(
                new int[]{x0, x0 + tabw - s, x0 + tabw - s, x0 + 2 * s, x0 + 2 * s, x0},
                new int[]{y0, y0, y0 + 2 * bh + s, y0 + 2 * bh + s, y0 + bh, y0 + bh},
                6
        );

        g.setColor(USABLE_BUTTON_COLOR);
        g.fillPolygon(enterPolygon);
        g.setColor(Color.BLACK);
        g.drawPolygon(enterPolygon);

        // caps lock
        int y2 = Y_SHIFT_T + (bh + s) * 2;
        g.drawRoundRect(X_SHIFT_L, y2, bsw, bh, arc, arc);
        for (int i = 0; i < 12; i++) {
            if (i < 9) {
                g.setColor(USABLE_BUTTON_COLOR);
                g.fillRoundRect(X_SHIFT_L + i * (bw + s) + bsw + s, y2, bw, bh, arc, arc);
                g.setColor(Color.BLACK);
            }
            g.drawRoundRect(X_SHIFT_L + i * (bw + s) + bsw + s, y2, bw, bh, arc, arc);
        }

        // l shift
        int y3 = Y_SHIFT_T + (bh + s) * 3;

        g.setColor(USABLE_BUTTON_COLOR);
        g.fillRoundRect(X_SHIFT_L, y3, lshiftw, bh, arc, arc);
        g.setColor(Color.BLACK);
        g.drawRoundRect(X_SHIFT_L, y3, lshiftw, bh, arc, arc);
        for (int i = 0; i < 11; i++) {
            if (i >= 1 && i < 8) {
                g.setColor(USABLE_BUTTON_COLOR);
                g.fillRoundRect(X_SHIFT_L + i * (bw + s) + lshiftw + s, y3, bw, bh, arc, arc);
                g.setColor(Color.BLACK);
            }
            g.drawRoundRect(X_SHIFT_L + i * (bw + s) + lshiftw + s, y3, bw, bh, arc, arc);
        }
        g.setColor(USABLE_BUTTON_COLOR);
        g.fillRoundRect(X_SHIFT_L + 11 * (bw + s) + lshiftw + s, y3, rshiftw, bh, arc, arc);
        g.setColor(Color.BLACK);
        g.drawRoundRect(X_SHIFT_L + 11 * (bw + s) + lshiftw + s, y3, rshiftw, bh, arc, arc);

        // l ctrl
        int y4 = Y_SHIFT_T + (bh + s) * 4;
        g.setColor(USABLE_BUTTON_COLOR);
        g.fillRoundRect(X_SHIFT_L, y4, tabw, bh, arc, arc);
        g.setColor(Color.BLACK);
        g.drawRoundRect(X_SHIFT_L, y4, tabw, bh, arc, arc);
        g.drawRoundRect(X_SHIFT_L + tabw + s, y4, bw, bh, arc, arc);
        g.drawRoundRect(X_SHIFT_L + tabw + bw + 2 * s, y4, tabw, bh, arc, arc);

        g.setColor(USABLE_BUTTON_COLOR);
        g.fillRoundRect(X_SHIFT_L + 2 * (tabw + s) + bw + 2 * s, y4, brakew, bh, arc, arc);
        g.setColor(Color.BLACK);
        g.drawRoundRect(X_SHIFT_L + 2 * (tabw + s) + bw + 2 * s, y4, brakew, bh, arc, arc);
        g.drawRoundRect(X_SHIFT_L + 2 * (tabw + s) + bw + 4 * s + brakew, y4, tabw, bh, arc, arc);

        g.setColor(USABLE_BUTTON_COLOR);
        g.fillRoundRect(X_SHIFT_L + 3 * (tabw + s) + bw + 4 * s + brakew, y4, bw, bh, arc, arc);
        g.setColor(Color.BLACK);
        g.drawRoundRect(X_SHIFT_L + 3 * (tabw + s) + bw + 4 * s + brakew, y4, bw, bh, arc, arc); // RCTRL
        g.drawRoundRect(X_SHIFT_L + 3 * (tabw + s) + 2 * bw + 5 * s + brakew, y4, bw, bh, arc, arc);
        g.drawRoundRect(X_SHIFT_L + 3 * (tabw + s) + 3 * bw + 6 * s + brakew, y4, tabw, bh, arc, arc);
    }
}
