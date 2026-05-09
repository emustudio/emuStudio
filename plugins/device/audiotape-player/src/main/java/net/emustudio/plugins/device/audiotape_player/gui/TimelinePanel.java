/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.plugins.device.audiotape_player.AutomationEvent;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * JTable-based panel that shows a two-column timeline of automation events.
 */
class TimelinePanel extends JTable {
    private static final Color COLOR_ACTIVE = new Color(50, 150, 50);
    private static final Color COLOR_ACTIVE_TEXT = Color.WHITE;

    private final EventTableModel model;
    private int activeIndex = -1;

    TimelinePanel(List<AutomationEvent> events) {
        this.model = new EventTableModel(events);
        setModel(model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(25);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        getTableHeader().setReorderingAllowed(false);

        getColumnModel().getColumn(0).setMaxWidth(40);
        getColumnModel().getColumn(0).setMinWidth(30);
    }

    void refresh() {
        model.fireTableDataChanged();
    }

    void setActiveIndex(int index) {
        this.activeIndex = index;
        repaint();
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (row == activeIndex) {
            c.setBackground(COLOR_ACTIVE);
            c.setForeground(COLOR_ACTIVE_TEXT);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
        } else if (!isRowSelected(row)) {
            c.setBackground(getBackground());
            c.setForeground(getForeground());
        }
        return c;
    }

    private static class EventTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"#", "Event"};
        private final List<AutomationEvent> events;

        EventTableModel(List<AutomationEvent> events) {
            this.events = events;
        }

        @Override
        public int getRowCount() {
            return events.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return rowIndex + 1;
            }
            return events.get(rowIndex).getDescription();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
