/*
 * DebugTable.java
 *
 * Created on Piatok, 2007, november 9, 8:20
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2007-2012, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package emustudio.gui.debugTable;

import emustudio.main.Main;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;

/**
 * Debug table.
 * 
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTable extends JTable {
    private DebugTableModel debug_model;
    private DebugCellRenderer debug_renderer;
  
    /** 
     * Creates a new instance of DebugTable
     */
    public DebugTable() {
        super();
        debug_model = new DebugTableModel(Main.currentArch.getComputer().getCPU());
        setModel(debug_model);
        debug_renderer = new DebugCellRenderer();
        setDefaultRenderer(Object.class, debug_renderer);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        debug_model.setRowCount(debug_renderer.estimateRowCount(getPreferredSize().height));
    }

    /**
     * This class does the painting of all debug table cells.
     */
    public class DebugCellRenderer extends JLabel implements TableCellRenderer {
        public int height = 17;

        /**
         * The constructor creates the renderer instance.
         */
        public DebugCellRenderer() {
            super();
            setOpaque(true);
            setFont(getFont().deriveFont(getFont().getStyle() & ~java.awt.Font.BOLD));
        }

        /**
         * Overrided method. Check Javadoc of the TableCellRenderer.
         * 
         * @param table
         * @param value
         * @param isSelected
         * @param hasFocus
         * @param row
         * @param column
         * @return 
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (debug_model.isCurrent(row)) {
                setBackground(Color.RED);
                setForeground(Color.WHITE);
            } else { 
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            if (value != null) {
                setText(" " + value.toString());
            } else {
                setText(" ");
            }
            height = getPreferredSize().height + 1;
            return this;
        }
        
        /**
         * Estimates maximal number of rows that would fit in the debug table without using scrolls.
         * @return estimated row count in the debug table
         */
        public int estimateRowCount(int height) {
            int result = (height / this.height) - 2;
            if (result <= 0) {
                result = 1;
            }
            return result;
        }
    }
    
    public void fireResized(int height) {
        debug_model.setRowCount(debug_renderer.estimateRowCount(height));
        repaint();
    }
    
    /**
     * Move to the first page.
     */
    public void firstPage() {
        debug_model.firstPage();
        update();
    }
    
    /**
     * Move to previous page.
     */
    public void previousPage() {
        debug_model.previousPage();
        update();
    }

    /**
     * Move to the page with actual PC pointer.
     */
    public void currentPage() {
        debug_model.currentPage();
        update();
    }

    /**
     * Move to next page.
     */
    public void nextPage() {
        debug_model.nextPage();
        update();
    }
    
    /**
     * Move to the last page.
     */
    public void lastPage() {
        debug_model.lastPage();
        update();
    }
    
    /**
     * Update values in the debug table, if it is enabled and repaint it.
     */
    public void update() {
        if (isEnabled()) {
            revalidate();
            repaint();
        }
    }
    
}
