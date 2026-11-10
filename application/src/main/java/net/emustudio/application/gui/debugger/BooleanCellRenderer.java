/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static net.emustudio.application.gui.framework.EmuStudioGui.loadIcon;
import static net.emustudio.emulib.ui.StandardIcons.ICON_BREAKPOINT;
import static net.emustudio.emulib.ui.Constants.TABLE_COLOR_ROW_EVEN;
import static net.emustudio.emulib.ui.Constants.TABLE_COLOR_ROW_ODD;

class BooleanCellRenderer extends DefaultTableCellRenderer {
    private static final Icon BOOLEAN_ICON = loadIcon(ICON_BREAKPOINT);

    public BooleanCellRenderer() {
        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {

        setBackground((row % 2 == 0) ? TABLE_COLOR_ROW_ODD : TABLE_COLOR_ROW_EVEN);
        super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
        setIcon((value != null && (Boolean) value) ? BOOLEAN_ICON : null);
        return this;
    }
}
