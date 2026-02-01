/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static net.emustudio.application.gui.debugtable.BooleanComponent.BOOLEAN_ICON;
import static net.emustudio.emulib.runtime.ui.Constants.TABLE_COLOR_ROW_EVEN;
import static net.emustudio.emulib.runtime.ui.Constants.TABLE_COLOR_ROW_ODD;

class BooleanCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {

    public BooleanCellRenderer() {
        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {

        setBackground((row % 2 == 0) ? TABLE_COLOR_ROW_ODD : TABLE_COLOR_ROW_EVEN);
        super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);

        boolean isBreakpointSet = value != null && (Boolean) value;
        if (isBreakpointSet) {
            setIcon(BOOLEAN_ICON);
        } else {
            setIcon(null);
        }
        return this;
    }
}
