/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.memory.ssem.gui.table;

import net.emustudio.plugins.memory.ssem.gui.Constants;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

import static net.emustudio.plugins.memory.ssem.gui.Constants.DEFAULT_FONT;

class MemoryRowHeaderRenderer extends JLabel implements ListCellRenderer<String> {

    MemoryRowHeaderRenderer(JTableHeader header) {
        setBorder(header.getBorder());
        setHorizontalAlignment(CENTER);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
        setFont(DEFAULT_FONT);
        setOpaque(true);
        setDoubleBuffered(true);
        this.setPreferredSize(new Dimension(4 * Constants.CHAR_WIDTH, header.getPreferredSize().height + 3));
    }

    @Override
    public Component getListCellRendererComponent(JList list, String value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        setText((value == null) ? "" : value);
        return this;
    }
}

