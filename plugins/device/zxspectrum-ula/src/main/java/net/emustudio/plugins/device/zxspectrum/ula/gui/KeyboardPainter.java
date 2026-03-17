/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.ula.gui.KeyboardCanvas.KeyboardButton;

import java.awt.*;
import java.util.Set;

/**
 * Responsible for painting the ZX Spectrum keyboard overlay.
 */
class KeyboardPainter {
    private static final int STROKE_WIDTH = 3;
    private static final Color USABLE_BUTTON_COLOR = Color.LIGHT_GRAY;
    private static final Color ACTIVE_BUTTON_COLOR = new Color(148, 178, 209);
    private static final Color OUTLINE_COLOR = Color.BLACK;

    private final BasicStroke outlineStroke = new BasicStroke(STROKE_WIDTH);
    private final KeyboardButton[] buttons;
    private final Shape keyboardOutline;
    private final Set<Integer> activeMouseKeys;

    private int alpha;
    private boolean shiftActive;
    private boolean symShiftActive;

    KeyboardPainter(int alpha, KeyboardButton[] buttons, Shape keyboardOutline, Set<Integer> activeMouseKeys) {
        this.alpha = alpha;
        this.buttons = buttons;
        this.keyboardOutline = keyboardOutline;
        this.activeMouseKeys = activeMouseKeys;
    }

    void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    void setShiftActive(boolean shiftActive) {
        this.shiftActive = shiftActive;
    }

    void setSymShiftActive(boolean symShiftActive) {
        this.symShiftActive = symShiftActive;
    }

    void drawKeyboard(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        drawOutlineAndButtons(g);
        drawActiveKeys(g);
        drawLabels(g);
    }

    private void drawOutlineAndButtons(Graphics2D g) {
        BasicStroke stroke = new BasicStroke(2.0f);
        Color adjUsableButtonColor = adjustAlpha(USABLE_BUTTON_COLOR);
        Color adjOutlineColor = adjustAlpha(OUTLINE_COLOR);

        g.setStroke(stroke);
        g.setColor(adjOutlineColor);
        g.draw(keyboardOutline);
        for (KeyboardButton button : buttons) {
            if (button.filled) {
                g.setColor(adjUsableButtonColor);
                g.fill(button.shape);
            }
            g.setColor(adjOutlineColor);
            g.draw(button.shape);
        }
    }

    private void drawActiveKeys(Graphics2D g) {
        if (activeMouseKeys.isEmpty()) {
            return;
        }

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(2.0f));
        for (KeyboardButton button : buttons) {
            if (button.interactive && activeMouseKeys.contains(button.keyId)) {
                g.setColor(adjustAlpha(ACTIVE_BUTTON_COLOR));
                g.fill(button.shape);
                g.setColor(adjustAlpha(OUTLINE_COLOR));
                g.draw(button.shape);
            }
        }
        g.setStroke(oldStroke);
    }

    private void drawLabels(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setStroke(outlineStroke);
        FontMetrics fontMetrics = g.getFontMetrics();

        for (KeyboardButton button : buttons) {
            if (!button.hasLabel()) {
                continue;
            }
            String text = button.label(symShiftActive, shiftActive);
            g.setColor(adjustAlpha(OUTLINE_COLOR));
            g.drawString(text, labelX(button, fontMetrics, text), labelY(button, fontMetrics));
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
}

