/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.debugger.AddressColumn;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Objects;

class TextCellRenderer extends DefaultTableCellRenderer {

    private final DebugTableModel model;

    TextCellRenderer(DebugTableModel model) {
        this.model = Objects.requireNonNull(model);
        setDoubleBuffered(true);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        boolean current = model.isRowAtCurrentInstruction(table.convertRowIndexToModel(row));
        boolean address = false;
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (model.getColumnAt(col) instanceof AddressColumn
                    && model.getColumnName(col).equals(table.getColumnName(column))) {
                address = true;
                break;
            }
        }
        setIcon(address ? (current ? CURRENT_INSTRUCTION : EMPTY_MARKER) : null);
        setIconTextGap(4);
        Font tableFont = table.getFont();
        setFont(new Font(Font.MONOSPACED, tableFont.getStyle(), tableFont.getSize()).deriveFont(tableFont.getSize2D()));
        setToolTipText(current ? "Current instruction (PC)" : null);
        getAccessibleContext().setAccessibleDescription(current ? "Current instruction (PC)" : null);
        return this;
    }

    private static final Icon EMPTY_MARKER = new Marker(false);
    private static final Icon CURRENT_INSTRUCTION = new Marker(true);

    /** PC remains visible independently of the selected row and its keyboard focus border. */
    private static final class Marker implements Icon {
        private final boolean current;
        private Marker(boolean current) { this.current = current; }
        @Override public int getIconWidth() { return 10; }
        @Override public int getIconHeight() { return 10; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (current) {
                graphics.setColor(component.getForeground());
                graphics.fillPolygon(new int[] {x + 1, x + 8, x + 1}, new int[] {y + 1, y + 5, y + 9}, 3);
            }
        }
    }
}
