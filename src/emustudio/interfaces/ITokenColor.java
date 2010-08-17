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
 *
 * @author vbmacher
 */
public class ITokenColor {
    public final static Color COMMENT = new Color(0,128,0); // green 
    public final static Color RESERVED = Color.BLACK;
    public final static Color IDENTIFIER = Color.BLACK;
    public final static Color LITERAL = new Color(0,0,128); // blue
    public final static Color LABEL = new Color(0,128,128); // weird blue
    public final static Color REGISTER = new Color(128,0,0);// brown
    
    public final static Color PREPROCESSOR = new Color(80,80,80); // weird gray
    public final static Color SEPARATOR = Color.BLACK;
    public final static Color OPERATOR = new Color(0,0,128);
    public final static Color ERROR = Color.RED;
}