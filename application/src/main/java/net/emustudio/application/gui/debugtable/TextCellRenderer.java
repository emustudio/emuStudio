/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.Constants;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.application.Constants.FONT_MONOSPACED;

class TextCellRenderer extends JLabel implements TableCellRenderer {

    private final DebugTableModel model;

    TextCellRenderer(DebugTableModel model) {
        this.model = Objects.requireNonNull(model);
        setFont(FONT_MONOSPACED);
        setDoubleBuffered(true);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        if (model.isRowAtCurrentInstruction(row)) {
            setBackground(Constants.DEBUGTABLE_COLOR_CURRENT_INSTRUCTION);
            setForeground(Color.WHITE);
        } else {
            setBackground((row % 2 == 0) ? Constants.DEBUGTABLE_COLOR_ROW_ODD : Constants.DEBUGTABLE_COLOR_ROW_EVEN);
            setForeground(Color.BLACK);
        }
        if (value != null) {
            setText(value.toString());
        } else {
            setText("");
        }
        return this;
    }
}
