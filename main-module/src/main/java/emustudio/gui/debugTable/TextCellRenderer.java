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
package emustudio.gui.debugTable;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

class TextCellRenderer extends JLabel implements TableCellRenderer {

    private final DebugTableModel model;

    TextCellRenderer(DebugTableModel model) {
        super();
        this.model = model;
        setOpaque(true);
        setFont(getFont().deriveFont(getFont().getStyle() & ~java.awt.Font.BOLD));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        if (model.isRowAtCurrentInstruction(row)) {
            setBackground(Colors.CURRENT_INSTRUCTION_COLOR);
            setForeground(Color.WHITE);
        } else {
            setBackground((row % 2 == 0) ? Colors.ODD_ROW_COLOR : Colors.EVEN_ROW_COLOR);
            setForeground(Color.BLACK);
        }
        if (value != null) {
            setText(" " + value.toString());
        } else {
            setText(" ");
        }
        return this;
    }

}
