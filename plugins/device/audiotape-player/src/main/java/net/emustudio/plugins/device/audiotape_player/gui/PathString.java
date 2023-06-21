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
package net.emustudio.plugins.device.audiotape_player.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;

public class PathString {
    private final static int CHARS_SHOWN = 13; // chars shown from left and right
    private final static int MAX_FILE_NAME_SIZE = 2 * CHARS_SHOWN + 10;

    private final Path path;
    private final boolean showWholePath;
    private int maxStringLength = MAX_FILE_NAME_SIZE;
    private int charsShown = CHARS_SHOWN;

    public PathString(Path path, boolean showWholePath, Component component, int width) {
        this.path = Objects.requireNonNull(path);
        this.showWholePath = showWholePath;
        if (component != null) {
            deriveMaxStringLength(component, width);
        }
    }

    public PathString(Path path, boolean showWholePath) {
        this(path, showWholePath, null, 0);
    }

    public PathString(Path path) {
        this(path, false, null, 0);
    }


    public Path getPath() {
        return path;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public void deriveMaxStringLength(Component component, int componentWidth) {
        FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
        String baseName = getBaseName();
        int baseNameWidth = fontMetrics.stringWidth(baseName);

        maxStringLength = baseName.length() * Math.min(componentWidth, baseNameWidth) / baseNameWidth;
        charsShown = Math.max(maxStringLength - 5, 0) / 2;
    }

    public String getPathShortened() {
        String baseName = getBaseName();
        int len = baseName.length();
        // shorten file name with 3 dots if it is too long
        if (len > maxStringLength) {
            return baseName.substring(0, charsShown) + "..." + baseName.substring(len - charsShown);
        }
        return baseName;
    }

    private String getBaseName() {
        return (showWholePath ? path : path.getFileName()).toString();
    }

    @Override
    public String toString() {
        return getPathShortened();
    }
}
