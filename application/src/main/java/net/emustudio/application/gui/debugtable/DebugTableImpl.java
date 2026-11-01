/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.debugger.BreakpointState;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.emulib.runtime.ui.debugger.AddressColumn;
import net.emustudio.emulib.runtime.ui.debugger.MnemoColumn;
import net.emustudio.emulib.runtime.ui.debugger.OpcodeColumn;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
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
            TableColumn tableColumn = getColumn(tableModel.getColumnName(i));
            DebuggerColumn<?> debugColumn = tableModel.getColumnAt(i);

            if (debugColumn.getClassType() == Boolean.class) {
                tableColumn.setCellEditor(new BooleanCellEditor());
                tableColumn.setMinWidth(32);
            }
            if (debugColumn.getDefaultWidth() != -1) {
                tableColumn.setPreferredWidth(debugColumn.getDefaultWidth());
            } else if (debugColumn.getClassType() == Boolean.class) {
                tableColumn.setMaxWidth(32);
                tableColumn.setPreferredWidth(32);
            } else if (debugColumn instanceof AddressColumn) {
                tableColumn.setPreferredWidth(80);
            } else if (debugColumn instanceof MnemoColumn) {
                tableColumn.setPreferredWidth(220);
            } else if (debugColumn instanceof OpcodeColumn) {
                tableColumn.setPreferredWidth(130);
            }
        }
        for (int i = 0; i < getColumnCount(); i++) {
            if (getColumnClass(i) == BreakpointState.class) {
                TableColumn column = getColumnModel().getColumn(i);
                BreakpointCell cell = new BreakpointCell();
                column.setCellRenderer(cell);
                column.setCellEditor(cell);
                column.setMinWidth(36);
                column.setMaxWidth(36);
                column.setPreferredWidth(36);
            }
        }
    }

    public DebugTableModel instructionModel() {
        return tableModel;
    }

    public void showInstructions(TableModel model) {
        setModel(model);
        setupRenderers();
        setupBooleanCellEditorAndDefaultWidth();
    }

    private void setupRenderers() {
        super.setDefaultRenderer(Object.class, textRenderer);
        super.setDefaultRenderer(String.class, textRenderer);
        super.setDefaultRenderer(Boolean.class, boolRenderer);
    }
}
