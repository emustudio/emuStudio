/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.memory.gui;

import java.awt.*;

public final class Constants {
    public final static Color COLOR_CELL_BACK = Color.WHITE;
    public final static Color COLOR_CELL_BACK_MOD2 = new Color((int) (0xFF * 0.9), (int) (0xFF * 0.9), (int) (0xFF * 0.9));
    public final static Color COLOR_FORE = Color.BLACK;
    public final static Color COLOR_FORE_UNIMPORTANT = Color.DARK_GRAY;

    public final static Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 12);
    public final static Font BOLD_FONT = new Font("Monospaced", Font.BOLD, 12);

    public final static int CHAR_WIDTH = 17;
    public final static int CHAR_HEIGHT = 5;
    public final static int TWO_CHARS = 2 * CHAR_WIDTH;

    public final static int[] COLUMN_WIDTH = new int[]{
        TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
        TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
        TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
        TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS, TWO_CHARS,
        10 * CHAR_WIDTH, 5 * CHAR_WIDTH
    };
}
