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
package net.emustudio.plugins.cpu.brainduck.gui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.Enumeration;

import static net.emustudio.plugins.cpu.brainduck.gui.Constants.MONOSPACED_PLAIN;

public class ColumnsRepainter {

    private static class UBorder extends LineBorder {
        private final boolean upper;
        private final Stroke stroke;

        public UBorder(Color color, int thickness, boolean upper) {
            super(color, thickness);
            this.stroke = new BasicStroke(thickness);
            this.upper = upper;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if ((thickness > 0) && (g instanceof Graphics2D)) {
                Graphics2D g2d = (Graphics2D) g;

                Color oldColor = g2d.getColor();
                g2d.setColor(this.lineColor);

                Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(stroke);

                g2d.drawLine(x, y + height, x, y);
                g2d.drawLine(x + width, y, x + width, y + height);

                if (upper) {
                    g2d.drawLine(x, y + height, x + width, y + height);
                } else {
                    g2d.drawLine(x, y, x + width, y);
                    g2d.drawLine(x + width - 1, y, x + width - 1, y + height);
                    g2d.drawLine(x, y + height, x + width, y + height); // separator
                }

                g2d.setStroke(oldStroke);
                g2d.setColor(oldColor);
            }
        }

    }

    private static class MainRenderer extends JLabel implements TableCellRenderer {

        private MainRenderer(boolean upper) {
            setFont(MONOSPACED_PLAIN);
            setBorder(new UBorder(Color.BLACK, 3, upper));
            setHorizontalAlignment(CENTER);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int rowIndex, int vColIndex) {
            setText(value.toString());
            setToolTipText((String) value);
            return this;
        }

    }

    public void setMainColumn(int column, JTable table) {
        TableColumn tableColumn = table.getColumnModel().getColumn(table.convertColumnIndexToModel(column));
        if (tableColumn != null) {
            tableColumn.setHeaderRenderer(new MainRenderer(false));
            tableColumn.setCellRenderer(new MainRenderer(true));
        }
    }

    public void repaint(JTable table) {
        TableModel tableModel = table.getModel();
        Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            column.setHeaderValue(tableModel.getColumnName(column.getModelIndex()));
        }
        JTableHeader header = table.getTableHeader();
        header.revalidate();
        header.repaint();
    }

}
