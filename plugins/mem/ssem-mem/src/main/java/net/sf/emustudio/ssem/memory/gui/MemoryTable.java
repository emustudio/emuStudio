/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.ssem.memory.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import static net.sf.emustudio.ssem.memory.gui.Constants.CHAR_HEIGHT;
import static net.sf.emustudio.ssem.memory.gui.Constants.COLUMN_WIDTH;
import static net.sf.emustudio.ssem.memory.gui.Constants.DEFAULT_FONT;

class MemoryTable extends JTable {
    private final MemoryTableModel model;
    private final CellRenderer cellRenderer;
    private final JScrollPane scrollPane;

    MemoryTable(MemoryTableModel model, JScrollPane scrollPane) {
        this.scrollPane = Objects.requireNonNull(scrollPane);
        this.model = Objects.requireNonNull(model);
        this.cellRenderer = new CellRenderer(model);
        
        super.setModel(this.model);
        super.setFont(DEFAULT_FONT);
        super.setCellSelectionEnabled(true);
        super.setFocusCycleRoot(true);
        super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.getTableHeader().setFont(DEFAULT_FONT);
    }
    
    public void setup() {
        cellRenderer.setup(this);
        setDefaultRenderer(Object.class, cellRenderer);
        scrollPane.setRowHeaderView(cellRenderer.getRowHeader());

        CellEditor editor = new CellEditor();
        editor.setup(this);
        
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn col = columnModel.getColumn(i);
            col.setPreferredWidth(COLUMN_WIDTH[i]);
            col.setCellEditor(editor);
        }
        setRowHeight(getRowHeight() + CHAR_HEIGHT);
        
        InputMap im = getInputMap(JTable.WHEN_FOCUSED);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent listener) {
                int row = getSelectedRow();
                int col = getSelectedColumn();
                
                if (row != -1 && col != -1) {
                    model.setValueAt("0", row, col);
                }
            }
        });
        
    }

}
