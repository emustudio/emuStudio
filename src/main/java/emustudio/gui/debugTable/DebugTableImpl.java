/*
 * DebugTableImpl.java
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

import emulib.emustudio.DebugTable;
import emustudio.main.Main;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

/**
 * Debug table.
 * 
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTableImpl extends JTable  implements DebugTable {
    private DebugTableModel debug_model;
    private TextCellRenderer text_renderer;
    private BooleanCellRenderer bool_renderer;

    private static final Color EVEN_ROW_COLOR = new Color(241, 245, 250);
    private static final Color TABLE_GRID_COLOR = new Color(0xd9d9d9);    

    private class BooleanCellRenderer extends JLabel implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            boolean boolValue = (value == null) ? false : (Boolean)value;
            
            if (boolValue) {
                setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/breakpoint.png")));
            } else {
                setIcon(null);
            }
            setBackground((row % 2 == 0) ? Color.WHITE : EVEN_ROW_COLOR);
            setText(" ");
            this.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            return this;
        }
        
        @Override
        public void paint(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paint(g);
        }
    }

    private class BooleanEditComponent extends JCheckBox {

        @Override
        public void paint(Graphics g) {
        }
    }

    /**
     * This class does the painting of all text/numbers debug table cells.
     */
    private class TextCellRenderer extends JLabel implements TableCellRenderer {
        public int height = 17;

        /**
         * The constructor creates the renderer instance.
         */
        public TextCellRenderer() {
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
                setBackground((row % 2 == 0) ? Color.WHITE : EVEN_ROW_COLOR);
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
            int result = (height / this.height) - 1;
            if (result <= 0) {
                result = 1;
            }
            return result;
        }
    }
    
    /** 
     * Creates a new instance of DebugTableImpl
     */
    public DebugTableImpl() {
        super();
        debug_model = new DebugTableModel(Main.architecture.getComputer().getCPU());
        setModel(debug_model);
        text_renderer = new TextCellRenderer();
        bool_renderer = new BooleanCellRenderer();
        setDefaultRenderer(Boolean.class, bool_renderer);
        setDefaultRenderer(Object.class, text_renderer);
        
        setAllBooleanCellEditor();
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        debug_model.setRowCount(text_renderer.estimateRowCount(getPreferredSize().height));
        int breakIndex = debug_model.getBreakpointColumnIndex();
        if (breakIndex >= 0) {
            getColumn(getColumnName(breakIndex)).setPreferredWidth(20);
        }
        setOpaque(false);
        setGridColor(TABLE_GRID_COLOR);
        setIntercellSpacing(new Dimension(0, 0));
        // turn off grid painting as we'll handle this manually in order to paint
        // grid lines over the entire viewport.
        setShowGrid(false);
    }
    
    private void setAllBooleanCellEditor() {
        int j = debug_model.getColumnCount();
        for (int i = 0; i < j; i++) {
            if (debug_model.getColumnClass(i) == Boolean.class) {
                this.getColumn(this.getColumnName(i)).setCellEditor(
                        new DefaultCellEditor(new BooleanEditComponent()));
            }
        }
    }

    public void fireResized(int height) {
        debug_model.setRowCount(text_renderer.estimateRowCount(height));
        repaint();
    }
    
    /**
     * Move to the first page.
     */
    public void firstPage() {
        debug_model.firstPage();
        refresh();
    }

    /**
     * Seeks the page backward by specified number of pages.
     * 
     * @param value number of pages to backward
     */
    public void pageSeekBackward(int value) {
        debug_model.seekBackwardPage(value);
        refresh();
    }
    
    /**
     * Move to previous page.
     */
    public void previousPage() {
        debug_model.previousPage();
        refresh();
    }

    /**
     * Move to the page with actual PC pointer.
     */
    public void currentPage() {
        debug_model.currentPage();
        refresh();
    }

    /**
     * Move to next page.
     */
    public void nextPage() {
        debug_model.nextPage();
        refresh();
    }
    
    /**
     * Seeks the page forward by specified number of pages.
     * 
     * @param value number of pages to forward
     */
    public void pageSeekForward(int value) {
        debug_model.seekForwardPage(value);
        refresh();
    }
    
    /**
     * Move to the last page.
     */
    public void lastPage() {
        debug_model.lastPage();
        refresh();
    }
    
    /**
     * Refresh the debug table.
     * 
     * If it is enabled, the method updates values and repaints it.
     */
    @Override
    public void refresh() {
        if (isEnabled()) {
            revalidate();
            repaint();
        }
    }
    
}
