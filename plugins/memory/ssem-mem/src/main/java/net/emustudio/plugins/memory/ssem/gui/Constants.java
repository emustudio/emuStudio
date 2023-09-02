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
package net.emustudio.plugins.memory.ssem.gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public final class Constants {
    public final static Font DEFAULT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public final static Font BOLD_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);

    public final static int CHAR_WIDTH = 17;
    public final static int TWO_CHARS = 2 * CHAR_WIDTH;

    public final static int[] COLUMN_WIDTH = new int[]{
            TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
            TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
            TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
            TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
            10 * CHAR_WIDTH, 10 * CHAR_WIDTH, 5 * CHAR_WIDTH
    };

    public static ImageIcon loadIcon(String resource) {
        URL url = Constants.class.getResource(resource);
        return url == null ? null : new ImageIcon(url);
    }
}
