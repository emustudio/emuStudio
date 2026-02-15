/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.Constants;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.*;

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
            setBackground((row % 2 == 0) ? TABLE_COLOR_ROW_ODD : TABLE_COLOR_ROW_EVEN);
            setForeground(Color.BLACK);
        }
        setText((value != null) ? value.toString() : "");
        return this;
    }
}
