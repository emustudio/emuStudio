/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.gui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * A table cell renderer that wraps text using a JTextArea.
 * When the text doesn't fit in the column width, it wraps to multiple lines
 * and the row height is adjusted accordingly.
 */
class WordWrapCellRenderer extends JTextArea implements TableCellRenderer {

    WordWrapCellRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        setText(value != null ? value.toString() : "");

        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        setFont(table.getFont());

        // Calculate the preferred height for the current column width
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        setSize(columnWidth, Short.MAX_VALUE);
        int preferredHeight = getPreferredSize().height + 4;

        // Update the row height if this cell needs more space
        if (table.getRowHeight(row) < preferredHeight) {
            table.setRowHeight(row, preferredHeight);
        }

        return this;
    }
}

