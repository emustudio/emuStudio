/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.gui.framework.EmuStudioGui;

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
        boolean address = DebugTableModel.TITLE_ADDRESS.equals(table.getColumnName(column));
        int modelRow = table.convertRowIndexToModel(row);
        boolean foldControl = address && model.hasFoldControl(modelRow);
        setIcon(address ? (current ? CURRENT_INSTRUCTION : foldControl
                ? (model.isFolded(modelRow) ? COLLAPSED : EXPANDED) : EMPTY_MARKER) : null);
        setIconTextGap(4);
        Font tableFont = table.getFont();
        setFont(EmuStudioGui.monospaced(tableFont));
        String description = current ? "Current instruction (PC)" : foldControl ? model.foldTooltip(modelRow) : null;
        setToolTipText(description);
        getAccessibleContext().setAccessibleDescription(description);
        return this;
    }

    private static final Icon EMPTY_MARKER = new Marker(false);
    private static final Icon CURRENT_INSTRUCTION = new Marker(true);
    private static final Icon COLLAPSED = new FoldMarker(false);
    private static final Icon EXPANDED = new FoldMarker(true);

    private static final class FoldMarker implements Icon {
        private final boolean expanded;
        private FoldMarker(boolean expanded) { this.expanded = expanded; }
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 10; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            graphics.setColor(component.getForeground());
            if (expanded) graphics.fillPolygon(new int[] {x + 1, x + 9, x + 5}, new int[] {y + 3, y + 3, y + 8}, 3);
            else graphics.fillPolygon(new int[] {x + 3, x + 8, x + 3}, new int[] {y + 1, y + 5, y + 9}, 3);
        }
    }

    /** PC remains visible independently of the selected row and its keyboard focus border. */
    private static final class Marker implements Icon {
        private final boolean current;
        private Marker(boolean current) { this.current = current; }
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 14; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (current) {
                graphics.setColor(component.getForeground());
                graphics.fillPolygon(new int[] {x + 1, x + 11, x + 1}, new int[] {y + 1, y + 7, y + 13}, 3);
            }
        }
    }
}
