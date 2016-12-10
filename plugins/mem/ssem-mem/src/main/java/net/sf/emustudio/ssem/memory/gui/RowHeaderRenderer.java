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
package net.sf.emustudio.ssem.memory.gui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import static net.sf.emustudio.ssem.memory.gui.Constants.BOLD_FONT;
import static net.sf.emustudio.ssem.memory.gui.Constants.CHAR_HEIGHT;
import static net.sf.emustudio.ssem.memory.gui.Constants.CHAR_WIDTH;

class RowHeaderRenderer extends JLabel implements ListCellRenderer {
    private final static int NO_COLUMN_WIDTH = CHAR_WIDTH * 4;
    
    private int height;

    RowHeaderRenderer() {
        super.setOpaque(true);
        super.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        super.setHorizontalAlignment(CENTER);
        super.setFont(BOLD_FONT);
    }
    
    public void setup(JTable table) {
        JTableHeader header = table.getTableHeader();
        this.height = header.getPreferredSize().height + CHAR_HEIGHT;
        setForeground(header.getForeground());
        setBackground(header.getBackground());
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setPreferredSize(new Dimension(NO_COLUMN_WIDTH, height));
        setText((value == null) ? "" : value.toString());
        return this;
    }
    
}
