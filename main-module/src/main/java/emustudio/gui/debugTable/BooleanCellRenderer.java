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
package emustudio.gui.debugTable;

import emustudio.Constants;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

class BooleanCellRenderer extends JLabel implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        boolean boolValue = (value == null) ? false : (Boolean) value;

        if (boolValue) {
            setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/breakpoint.png")));
        } else {
            setIcon(null);
        }
        setBackground((row % 2 == 0) ? Constants.DEBUGTABLE_COLOR_ROW_ODD : Constants.DEBUGTABLE_COLOR_ROW_EVEN);
        setText(" ");
        this.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        return this;
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
    }
}
