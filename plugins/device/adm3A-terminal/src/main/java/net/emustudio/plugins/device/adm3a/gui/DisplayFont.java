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
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.plugins.device.adm3a.TerminalSettings;

import java.util.Objects;

public class DisplayFont {

    public static final DisplayFont FONT_ORIGINAL = new DisplayFont(
            "/net/emustudio/plugins/device/adm3a/gui/adm-3a.ttf",
            2, 2, 5, 14, 5
    );

    public static final DisplayFont FONT_MODERN = new DisplayFont(
            "/net/emustudio/plugins/device/adm3a/gui/terminal.ttf",
            2, 3, 0, 15, 0
    );

    public final String path;
    public final int xCursorOffset;
    public final int yCursorOffset;
    public final int yCursorExtend;
    public final int yLineHeightMultiplierOffset;
    public final int fontSize;

    public DisplayFont(String path, int xCursorOffset, int yCursorOffset,
                       int yCursorExtend, int fontSize, int yLineHeightMultiplierOffset) {
        this.path = Objects.requireNonNull(path);
        this.xCursorOffset = xCursorOffset;
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
