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
package emustudio.gui.editor;

import javax.swing.plaf.FontUIResource;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;

/**
 * This class represents a style used within the syntax highlighting process.
 *
 * For each token type an instance of this class is created and used in the
 * proper places.
 *
 */
class HighlightStyle extends SimpleAttributeSet {

    HighlightStyle(boolean italic, boolean bold, Color color) {
        setStyle(italic, bold, color);
    }

    private void setStyle(boolean italic, boolean bold, Color color) {
        StyleConstants.setFontFamily(this, FontUIResource.MONOSPACED);
        StyleConstants.setFontSize(this, 12);
        StyleConstants.setBackground(this, Color.white);
        StyleConstants.setItalic(this, italic);
        StyleConstants.setForeground(this, color);
        StyleConstants.setBold(this, bold);
    }
}
