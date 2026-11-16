/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntConsumer;

public class DebugTableImpl extends JTable {
    private static final Color TABLE_COLOR_TABLE_GRID = new Color(0xd9d9d9);
    private final DebugTableModel tableModel;
    private final BooleanCellRenderer boolRenderer = new BooleanCellRenderer();
    private final TextCellRenderer textRenderer;
    private final InstructionFlowColumn flowColumn;
    private IntConsumer flowNavigation = address -> {
    };
    private boolean flowGesture;
    private IntPredicate breakpointRow = row -> false;
    private IntFunction<String> rowTooltip = row -> null;
    private IntFunction<Color> rowBackground = row -> null;
    private Runnable restoreSelection = () -> {
    };
    private Color tooltipBackground;

    public DebugTableImpl(DebugTableModel tableModel) {
        super(Objects.requireNonNull(tableModel));
        this.tableModel = tableModel;
        this.textRenderer = new TextCellRenderer(tableModel);
        this.flowColumn = new InstructionFlowColumn(this, tableModel);

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
        getSelectionModel().addListSelectionListener(event -> repaint());
        ToolTipManager.sharedInstance().registerComponent(this);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Rectangle vr = getVisibleRect();
                int rowHeight = getRowHeight();
                tableModel.setMaxRows(vr.height / rowHeight);
            }
        });
    }

    void setupColumnWidths() {
        int columnCount = tableModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            int viewColumn = convertColumnIndexToView(i);
            if (viewColumn < 0) continue;
            TableColumn tableColumn = getColumnModel().getColumn(viewColumn);
            DebuggerColumn<?> debugColumn = tableModel.getColumnAt(i);

            if (i == tableModel.flowColumnIndex()) {
                tableColumn.setMinWidth(80);
                tableColumn.setPreferredWidth(160);
            } else if (debugColumn.getClassType() == Boolean.class) {
                tableColumn.setMinWidth(32);
            }
            if (debugColumn.getDefaultWidth() != -1) {
                tableColumn.setPreferredWidth(debugColumn.getDefaultWidth());
            } else if (debugColumn.getClassType() == Boolean.class) {
                tableColumn.setMaxWidth(32);
                tableColumn.setPreferredWidth(32);
            } else if (DebugTableModel.TITLE_LABEL.equals(debugColumn.getTitle())) {
                tableColumn.setPreferredWidth(140);
            } else if (DebugTableModel.TITLE_ADDRESS.equals(debugColumn.getTitle())) {
                tableColumn.setPreferredWidth(80);
            } else if (DebugTableModel.TITLE_MNEMO.equals(debugColumn.getTitle())) {
                tableColumn.setPreferredWidth(220);
            } else if (DebugTableModel.TITLE_OPCODE.equals(debugColumn.getTitle())) {
                tableColumn.setPreferredWidth(130);
            }
        }
    }

    public DebugTableModel instructionModel() {
        return tableModel;
    }

    public void setFlowNavigation(IntConsumer navigation) {
        flowNavigation = Objects.requireNonNull(navigation);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (flowColumn != null) flowColumn.paint((Graphics2D) graphics);
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        if (SwingUtilities.isLeftMouseButton(event)) {
            int column = columnAtPoint(event.getPoint());
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                boolean continuing = flowGesture && event.getClickCount() > 1 && column >= 0
                        && convertColumnIndexToModel(column) == tableModel.flowColumnIndex();
                int target = continuing || flowColumn == null ? -1 : flowColumn.targetAt(event.getPoint());
                flowGesture = continuing || target >= 0;
                if (target >= 0) {
                    requestFocusInWindow();
                    flowNavigation.accept(target);
                }
            }
            if (flowGesture && (event.getID() == MouseEvent.MOUSE_PRESSED
                    || event.getID() == MouseEvent.MOUSE_RELEASED || event.getID() == MouseEvent.MOUSE_CLICKED)) {
                event.consume();
                return;
            }
        }
        super.processMouseEvent(event);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        super.processMouseMotionEvent(event);
        if (event.getID() == MouseEvent.MOUSE_MOVED && flowColumn != null)
            setCursor(Cursor.getPredefinedCursor(flowColumn.targetAt(event.getPoint()) >= 0
                    ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void tableChanged(TableModelEvent event) {
        boolean selected = getSelectionModel() != null && getSelectedRow() >= 0;
        super.tableChanged(event);
        if (tableModel == null) return;
        if (event == null || event.getFirstRow() == TableModelEvent.HEADER_ROW) {
            setupRenderers();
            setupColumnWidths();
        }
        if (selected) restoreSelection.run();
    }

    public void setInstructionRowPresentation(IntPredicate breakpointRow, IntFunction<String> rowTooltip,
                                              Runnable restoreSelection) {
        this.breakpointRow = Objects.requireNonNull(breakpointRow);
        this.rowTooltip = Objects.requireNonNull(rowTooltip);
        this.restoreSelection = Objects.requireNonNull(restoreSelection);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setRowBackground(IntFunction<Color> background) {
        rowBackground = Objects.requireNonNull(background);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        String flowTooltip = flowColumn == null ? null : flowColumn.tooltipAt(event.getPoint());
        if (flowTooltip != null) {
            tooltipBackground = null;
            return flowTooltip;
        }
        int row = rowAtPoint(event.getPoint());
        int modelRow = row < 0 ? -1 : convertRowIndexToModel(row);
        String tooltip = modelRow < 0 ? null : rowTooltip.apply(modelRow);
        tooltipBackground = tooltip == null ? null : rowBackground.apply(modelRow);
        return tooltip != null ? tooltip : super.getToolTipText(event);
    }

    @Override
    public JToolTip createToolTip() {
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
