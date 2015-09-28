/*
 * KISS, YAGNI, DRY
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
import emustudio.architecture.Computer;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Objects;

public class DebugTableImpl extends JTable  implements DebugTable {
    private final DebugTableModel debugModel;
    private final TextCellRenderer textRenderer;
    private final BooleanCellRenderer boolRenderer;

    private static final Color EVEN_ROW_COLOR = new Color(241, 245, 250);
    private static final Color TABLE_GRID_COLOR = new Color(0xd9d9d9);

    private static class BooleanCellRenderer extends JLabel implements TableCellRenderer {

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

    private static class BooleanEditComponent extends JCheckBox {

        @Override
        public void paint(Graphics g) {
        }
    }

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

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (debugModel.isCurrent(row)) {
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

    public DebugTableImpl(DebugTableModel debugModel) {
        super();
        this.debugModel = Objects.requireNonNull(debugModel);
        setModel(this.debugModel);
        textRenderer = new TextCellRenderer();
        boolRenderer = new BooleanCellRenderer();
        setDefaultRenderer(Boolean.class, boolRenderer);
        setDefaultRenderer(Object.class, textRenderer);

        setAllBooleanCellEditor();
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int breakIndex = debugModel.getBreakpointColumnIndex();
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
        int columnCount = debugModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            if (debugModel.getColumnClass(i) == Boolean.class) {
                this.getColumn(this.getColumnName(i)).setCellEditor(
                        new DefaultCellEditor(new BooleanEditComponent()));
            }
        }
    }

    public void fireResized(int height) {
        repaint();
    }

    /**
     * Move to the first page.
     */
    public void firstPage() {
        debugModel.firstPage();
        refresh();
    }

    /**
     * Seeks the page backward by specified number of pages.
     *
     * @param value number of pages to backward
     */
    public void pageSeekBackward(int value) {
        debugModel.seekBackwardPage(value);
        refresh();
    }

    /**
     * Move to previous page.
     */
    public void previousPage() {
        debugModel.previousPage();
        refresh();
    }

    /**
     * Move to the page with actual PC pointer.
     */
    public void currentPage() {
        debugModel.currentPage();
        refresh();
    }

    /**
     * Move to next page.
     */
    public void nextPage() {
        debugModel.nextPage();
        refresh();
    }

    /**
     * Seeks the page forward by specified number of pages.
     *
     * @param value number of pages to forward
     */
    public void pageSeekForward(int value) {
        debugModel.seekForwardPage(value);
        refresh();
    }

    /**
     * Move to the last page.
     */
    public void lastPage() {
        debugModel.lastPage();
        refresh();
    }

    @Override
    public void refresh() {
        if (isEnabled()) {
            revalidate();
            repaint();
        }
    }

}
