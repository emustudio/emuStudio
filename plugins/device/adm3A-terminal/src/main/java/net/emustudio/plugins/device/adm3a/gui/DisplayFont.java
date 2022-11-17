package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.TerminalSettings;

import java.util.Objects;

public class DisplayFont {

    public static final DisplayFont FONT_ORIGINAL = new DisplayFont(
        "/net/emustudio/plugins/device/adm3a/gui/adm-3a.ttf",
        2, 0.3, 3, 5, 12, 5
    );

    public static final DisplayFont FONT_MODERN = new DisplayFont(
        "/net/emustudio/plugins/device/adm3a/gui/terminal.ttf",
        2, 0, 3, 0, 15, 0
    );

    public final String path;
    public final int xCursorOffset;
    public final double xCursorMultiplierOffset;
    public final int yCursorOffset;
    public final int yCursorExtend;
    public final int yLineHeightMultiplierOffset;
    public final int fontSize;

    public DisplayFont(String path, int xCursorOffset, double xCursorMultiplierOffset, int yCursorOffset,
                       int yCursorExtend, int fontSize, int yLineHeightMultiplierOffset) {
        this.path = Objects.requireNonNull(path);
        this.xCursorOffset = xCursorOffset;
        this.xCursorMultiplierOffset = xCursorMultiplierOffset;
        this.yCursorOffset = yCursorOffset;
        this.yCursorExtend = yCursorExtend;
        this.fontSize = fontSize;
        this.yLineHeightMultiplierOffset = yLineHeightMultiplierOffset;
    }

    public static DisplayFont fromTerminalFont(TerminalSettings.TerminalFont font) {
        if (font == TerminalSettings.TerminalFont.ORIGINAL) {
            return FONT_ORIGINAL;
        }
        return FONT_MODERN;
    }
}
