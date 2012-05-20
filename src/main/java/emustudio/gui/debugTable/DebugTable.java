/**
 * DebugTable.java
 *
 * Created on Piatok, 2007, november 9, 8:20
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Debug table.
 * 
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTable extends JTable {
    private DebugTableModel debug_model;
  
    /** 
     * Creates a new instance of DebugTable
     */
    public DebugTable() {
        debug_model = new DebugTableModel(Main.currentArch.getComputer().getCPU());
        setModel(debug_model);
        setDefaultRenderer(Object.class, new DebugCellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * This class does the painting of all debug table cells.
     */
    public class DebugCellRenderer extends JLabel implements TableCellRenderer {

        /**
         * The constructor creates the renderer instance.
         */
        public DebugCellRenderer() {
            super();
            setOpaque(true);
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
                this.setBackground(Color.RED);
                this.setForeground(Color.WHITE);
            } else { 
                this.setBackground(Color.WHITE);
                this.setForeground(Color.BLACK);
            }
            if (value != null)
                setText(" " + value.toString());
            else
                setText(" ");
            return this;
        }
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
