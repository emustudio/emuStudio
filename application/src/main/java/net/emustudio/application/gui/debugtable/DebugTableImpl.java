/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

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
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import static net.emustudio.emulib.runtime.ui.Constants.TABLE_COLOR_TABLE_GRID;

public class DebugTableImpl extends JTable {
    private final DebugTableModel tableModel;
    private final BooleanCellRenderer boolRenderer = new BooleanCellRenderer();
    private final TextCellRenderer textRenderer;
    private IntPredicate breakpointRow = row -> false;
    private IntFunction<String> rowTooltip = row -> null;
    private IntFunction<Color> rowBackground = row -> null;
    private Color tooltipBackground;

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
        setupColumnWidths();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Rectangle vr = getVisibleRect();
                int rowHeight = getRowHeight();
                tableModel.setMaxRows(vr.height / rowHeight);
            }
        });
    }

    private void setupColumnWidths() {
        int columnCount = tableModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            int viewColumn;
            try {
                viewColumn = getColumnModel().getColumnIndex(tableModel.getColumnName(i));
            } catch (IllegalArgumentException missingColumn) {
                continue;
            }
            TableColumn tableColumn = getColumnModel().getColumn(viewColumn);
            DebuggerColumn<?> debugColumn = tableModel.getColumnAt(i);

            if (debugColumn.getClassType() == Boolean.class) {
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
    }

    public DebugTableModel instructionModel() {
        return tableModel;
    }

    public void showInstructions(TableModel model) {
        setModel(model);
        setupRenderers();
        setupColumnWidths();
    }

    public void setInstructionRowPresentation(IntPredicate breakpointRow, IntFunction<String> rowTooltip) {
        this.breakpointRow = Objects.requireNonNull(breakpointRow);
        this.rowTooltip = Objects.requireNonNull(rowTooltip);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setRowBackground(IntFunction<Color> background) {
        rowBackground = Objects.requireNonNull(background);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int modelRow = row < 0 ? -1 : convertRowIndexToModel(row);
        String tooltip = modelRow < 0 ? null : rowTooltip.apply(modelRow);
        tooltipBackground = tooltip == null ? null : rowBackground.apply(modelRow);
        return tooltip != null ? tooltip : super.getToolTipText(event);
    }

    @Override public JToolTip createToolTip() {
        JToolTip tooltip = super.createToolTip();
        if (tooltipBackground != null) tooltip.setBackground(tooltipBackground);
        return tooltip;
    }

    @Override
    public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int modelRow = convertRowIndexToModel(row);
        Color background = rowBackground.apply(modelRow);
        component.setBackground(isCellSelected(row, column) ? getSelectionBackground()
                : background != null ? background : getBackground());
        if (tableModel.isRowAtCurrentInstruction(modelRow)) {
            component.setBackground(new Color(255, 179, 179));
            component.setForeground(Color.BLACK);
        }
        Font font = component.getFont();
        component.setFont(font.deriveFont(breakpointRow.test(modelRow)
                ? font.getStyle() | Font.BOLD : font.getStyle() & ~Font.BOLD));
        return component;
    }

    private void setupRenderers() {
        super.setDefaultRenderer(Object.class, textRenderer);
        super.setDefaultRenderer(String.class, textRenderer);
        super.setDefaultRenderer(Boolean.class, boolRenderer);
    }
}
