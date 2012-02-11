/*
 * HighlightStyle.java
 *
 * Created on 7.2.2008, 9:53:55
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.gui.editor;

import java.awt.Color;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * This class represents a style used within the syntax highlighting process.
 *
 * For each token type an instance of this class is created and used in the
 * proper places.
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class HighlightStyle extends SimpleAttributeSet {

    /**
     * Creates new instance of the font style.
     *
     * @param italic true if the font should be italic, false otherwise
     * @param bold  true if the font should be bold, false otherwise
     * @param color color of the font. The color constant is taken from
     * the ITokenColor interface.
     */
    public HighlightStyle(boolean italic, boolean bold, Color color) {
        setStyle(italic, bold, color);
    }

    private void setStyle(boolean italic, boolean bold, Color color) {
        StyleConstants.setFontFamily(this, "Monospaced");
        StyleConstants.setFontSize(this, 12);
        StyleConstants.setBackground(this, Color.white);
        StyleConstants.setItalic(this, italic);
        StyleConstants.setForeground(this, color);
        StyleConstants.setBold(this, bold);
    }
}
