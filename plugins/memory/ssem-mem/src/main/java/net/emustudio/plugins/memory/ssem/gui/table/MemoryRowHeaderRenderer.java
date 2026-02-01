/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.table;

import net.emustudio.plugins.memory.ssem.gui.Constants;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

class MemoryRowHeaderRenderer extends JLabel implements ListCellRenderer<String> {

    MemoryRowHeaderRenderer(JTableHeader header) {
        setBorder(header.getBorder());
        setHorizontalAlignment(CENTER);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
        setFont(FONT_MONOSPACED);
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

