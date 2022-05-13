/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ssem.gui;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

class RowHeaderRenderer extends JLabel implements ListCellRenderer<String> {
    private final static int NO_COLUMN_WIDTH = Constants.CHAR_WIDTH * 4;

    private int height;

    RowHeaderRenderer() {
        super.setBorder(UIManager.getBorder("Button.border"));
        super.setHorizontalAlignment(CENTER);
        super.setFont(Constants.DEFAULT_FONT);
    }

    public void setup(JTable table) {
        JTableHeader header = table.getTableHeader();
        this.height = header.getPreferredSize().height + Constants.CHAR_HEIGHT;
        setForeground(header.getForeground());
        setBackground(header.getBackground());
    }

    @Override
    public Component getListCellRendererComponent(JList list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        setPreferredSize(new Dimension(NO_COLUMN_WIDTH, height));
        setText((value == null) ? "" : value);
        return this;
    }
}
