/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.TerminalSettings;

import java.awt.RenderingHints;
import java.util.Objects;

public class DisplayFont {

    public static final DisplayFont FONT_ORIGINAL = new DisplayFont(
            "/net/emustudio/plugins/device/adm3a/gui/adm-3a.ttf",
            2, 2, 5, 14, 5,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
            RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.VALUE_FRACTIONALMETRICS_OFF
    );

    public static final DisplayFont FONT_MODERN = new DisplayFont(
            "/net/emustudio/plugins/device/adm3a/gui/terminal.ttf",
            2, 3, 0, 15, 0,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
            RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON
    );

    public final String path;
    public final int xCursorOffset;
    public final int yCursorOffset;
    public final int yCursorExtend;
    public final int yLineHeightMultiplierOffset;
    public final int fontSize;
    public final Object textAntiAliasing;
    public final Object antiAliasing;
    public final Object fractionalMetrics;

    public DisplayFont(String path, int xCursorOffset, int yCursorOffset,
                       int yCursorExtend, int fontSize, int yLineHeightMultiplierOffset,
                       Object textAntiAliasing, Object antiAliasing, Object fractionalMetrics) {
        this.path = Objects.requireNonNull(path);
        this.xCursorOffset = xCursorOffset;
        this.yCursorOffset = yCursorOffset;
        this.yCursorExtend = yCursorExtend;
        this.fontSize = fontSize;
        this.yLineHeightMultiplierOffset = yLineHeightMultiplierOffset;
        this.textAntiAliasing = Objects.requireNonNull(textAntiAliasing);
        this.antiAliasing = Objects.requireNonNull(antiAliasing);
        this.fractionalMetrics = Objects.requireNonNull(fractionalMetrics);
    }

    public static DisplayFont fromTerminalFont(TerminalSettings.TerminalFont font) {
        if (font == TerminalSettings.TerminalFont.ORIGINAL) {
            return FONT_ORIGINAL;
        }
        return FONT_MODERN;
    }
}
