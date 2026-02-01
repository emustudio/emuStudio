/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.TABLE_COLOR_TABLE_GRID;

public class DebugTableImpl extends JTable {
    private final DebugTableModel tableModel;
    private final BooleanCellRenderer boolRenderer = new BooleanCellRenderer();
    private final TextCellRenderer textRenderer;

    public DebugTableImpl(DebugTableModel tableModel) {
        super(Objects.requireNonNull(tableModel));
        this.tableModel = tableModel;
        this.textRenderer = new TextCellRenderer(tableModel);

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setGridColor(TABLE_COLOR_TABLE_GRID);
        setIntercellSpacing(new Dimension(0, 0));
        // turn off grid painting as we'll handle this manually
        setShowGrid(false);
        setDoubleBuffered(true);
        setFillsViewportHeight(true);
        setOpaque(true);

        setupRenderers();
        setupBooleanCellEditorAndDefaultWidth();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Rectangle vr = getVisibleRect();
                int rowHeight = getRowHeight();
                tableModel.setMaxRows(vr.height / rowHeight);
            }
        });
    }

    private void setupBooleanCellEditorAndDefaultWidth() {
        int columnCount = tableModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tableColumn = getColumn(getColumnName(i));
            DebuggerColumn<?> debugColumn = tableModel.getColumnAt(i);

            if (debugColumn.getClassType() == Boolean.class) {
                tableColumn.setCellEditor(new BooleanCellEditor());
            }
            if (debugColumn.getDefaultWidth() != -1) {
                tableColumn.setPreferredWidth(debugColumn.getDefaultWidth());
            }
        }
    }

    private void setupRenderers() {
        super.setDefaultRenderer(Object.class, textRenderer);
        super.setDefaultRenderer(String.class, textRenderer);
        super.setDefaultRenderer(Boolean.class, boolRenderer);
    }
}
