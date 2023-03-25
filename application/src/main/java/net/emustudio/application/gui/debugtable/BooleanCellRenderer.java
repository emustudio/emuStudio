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
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static net.emustudio.application.gui.debugtable.BooleanComponent.BOOLEAN_ICON;

class BooleanCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {

    public BooleanCellRenderer() {
        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {

        setBackground((row % 2 == 0) ? Constants.DEBUGTABLE_COLOR_ROW_ODD : Constants.DEBUGTABLE_COLOR_ROW_EVEN);
        super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);

        boolean isBreakpointSet = (value == null) ? false : (Boolean) value;
        if (isBreakpointSet) {
            setIcon(BOOLEAN_ICON);
        } else {
            setIcon(null);
        }
        return this;
    }
}
