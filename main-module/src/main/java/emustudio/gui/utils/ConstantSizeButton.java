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
package emustudio.gui.utils;

import javax.swing.*;
import java.awt.*;

/**
 * A button with the constant size.
 */
public class ConstantSizeButton extends JButton {

    private final static int NB_WIDTH = 95;
    private static int NB_HEIGHT = 30;

    private void setHeight() {
        FontMetrics metrics = this.getFontMetrics(getFont());
        NB_HEIGHT = metrics.getHeight() + 9;
    }

    public ConstantSizeButton() {
        setHeight();
        Dimension d = getPreferredSize();
        d.setSize(NB_WIDTH, NB_HEIGHT);
        this.setPreferredSize(d);
        this.setSize(NB_WIDTH, NB_HEIGHT);
        this.setMinimumSize(d);
        this.setMaximumSize(d);
    }
}
