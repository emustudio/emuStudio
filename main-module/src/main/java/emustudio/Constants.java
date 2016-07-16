/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package emustudio;

import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;

public class Constants {

    public static final Font MONOSPACED_PLAIN_12 = new Font(FontUIResource.MONOSPACED, Font.PLAIN, 12);

    public static final Color DEBUGTABLE_COLOR_ROW_EVEN = new Color(241, 245, 250);
    public static final Color DEBUGTABLE_COLOR_ROW_ODD = Color.WHITE;
    public static final Color DEBUGTABLE_COLOR_TABLE_GRID = new Color(0xd9d9d9);
    public static final Color DEBUGTABLE_COLOR_CURRENT_INSTRUCTION = Color.RED;

    public static final Color TOKEN_COMMENT = new Color(0,128,0);
    public static final Color TOKEN_RESERVED = Color.BLACK;
    public static final Color TOKEN_IDENTIFIER = Color.BLACK;
    public static final Color TOKEN_LITERAL = new Color(0,0,128);
    public static final Color TOKEN_LABEL = new Color(0,128,128);
    public static final Color TOKEN_REGISTER = new Color(128,0,0);
    public static final Color TOKEN_PREPROCESSOR = new Color(80,80,80);
    public static final Color TOKEN_SEPARATOR = Color.BLACK;
    public static final Color TOKEN_OPERATOR = new Color(0,0,128);
    public static final Color TOKEN_ERROR = Color.RED;
}
