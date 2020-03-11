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
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.Constants;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;

public class DebugTableImpl extends JTable implements DebuggerTable {

    public DebugTableImpl() {
        super(DebugTableModel.EMPTY);

        BooleanCellRenderer boolRenderer = new BooleanCellRenderer();
        super.setDefaultRenderer(Boolean.class, boolRenderer);

        setupBooleanCellEditorAndDefaultWidth();

        super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.setOpaque(false);
        super.setGridColor(Constants.DEBUGTABLE_COLOR_TABLE_GRID);
        super.setIntercellSpacing(new Dimension(0, 0));
        // turn off grid painting as we'll handle this manually
        super.setShowGrid(false);
    }

    private void setupBooleanCellEditorAndDefaultWidth() {
        DebugTableModel model = (DebugTableModel) getModel();

        int columnCount = model.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tableColumn = getColumn(getColumnName(i));
            DebuggerColumn<?> debugColumn = model.getColumnAt(i);

            if (debugColumn.getClassType() == Boolean.class) {
                tableColumn.setCellEditor(new DefaultCellEditor(new InvisibleJCheckBox()));
            }
            if (debugColumn.getDefaultWidth() != -1)
                tableColumn.setPreferredWidth(debugColumn.getDefaultWidth());
        }
    }

    @Override
    public void setModel(TableModel dataModel) {
        if (!(dataModel instanceof DebugTableModel)) {
            throw new IllegalArgumentException("Table model must be instance of DebugTableModel");
        }

        super.setModel(dataModel);

        TextCellRenderer textRenderer = new TextCellRenderer((DebugTableModel) dataModel);
        setDefaultRenderer(Object.class, textRenderer);
        setDefaultRenderer(String.class, textRenderer);
    }

    @Override
    public void setDebuggerColumns(List<DebuggerColumn<?>> columns) {
        ((DebugTableModel)getModel()).setColumns(columns);
        setupBooleanCellEditorAndDefaultWidth();
    }
}
