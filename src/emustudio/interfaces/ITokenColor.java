/*
 * ITokenColor.java
 *
 * Created on Streda, 2007, september 5, 10:43
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.interfaces;

import java.awt.Color;

/**
 * This class holds Color constants for particular token types.
 *
 * @author vbmacher
 */
public class ITokenColor {
    /**
     * Comment token. Green color.
     */
    public final static Color COMMENT = new Color(0,128,0);

    /**
     * Reserved token. Black color.
     */
    public final static Color RESERVED = Color.BLACK;

    /**
     * Identifier token. Black color.
     */
    public final static Color IDENTIFIER = Color.BLACK;

    /**
     * Literal token. Blue color.
     */
    public final static Color LITERAL = new Color(0,0,128);

    /**
     * Label token. "Weird" blue color.
     */
    public final static Color LABEL = new Color(0,128,128);

    /**
     * Register token. Brown color.
     */
    public final static Color REGISTER = new Color(128,0,0);

    /**
     * Preprocessor token. "Weird" gray color.
     */
    public final static Color PREPROCESSOR = new Color(80,80,80);

    /**
     * Separator token. Black color.
     */
    public final static Color SEPARATOR = Color.BLACK;

    /**
     * Operator token. Blue color.
     */
    public final static Color OPERATOR = new Color(0,0,128);

    /**
     * Error token. Red color.
     */
    public final static Color ERROR = Color.RED;
}
