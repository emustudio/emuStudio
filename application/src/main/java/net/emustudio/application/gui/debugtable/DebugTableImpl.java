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

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Objects;

public class DebugTableImpl extends JTable  {
    private final DebugTableModel tableModel;

    public DebugTableImpl(DebugTableModel tableModel) {
        super(Objects.requireNonNull(tableModel));
        this.tableModel = tableModel;

        super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.setOpaque(false);
        super.setGridColor(Constants.DEBUGTABLE_COLOR_TABLE_GRID);
        super.setIntercellSpacing(new Dimension(0, 0));
        // turn off grid painting as we'll handle this manually
        super.setShowGrid(false);

        setupRenderers();
        setupBooleanCellEditorAndDefaultWidth();
    }

    private void setupBooleanCellEditorAndDefaultWidth() {
        int columnCount = tableModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tableColumn = getColumn(getColumnName(i));
            DebuggerColumn<?> debugColumn = tableModel.getColumnAt(i);

            if (debugColumn.getClassType() == Boolean.class) {
                tableColumn.setCellEditor(new DefaultCellEditor(new InvisibleJCheckBox()));
            }
            if (debugColumn.getDefaultWidth() != -1) {
                tableColumn.setPreferredWidth(debugColumn.getDefaultWidth());
            }
        }
    }

    private void setupRenderers() {
        BooleanCellRenderer boolRenderer = new BooleanCellRenderer();
        TextCellRenderer textRenderer = new TextCellRenderer(tableModel);
        super.setDefaultRenderer(Object.class, textRenderer);
        super.setDefaultRenderer(String.class, textRenderer);
        super.setDefaultRenderer(Boolean.class, boolRenderer);
    }
}
