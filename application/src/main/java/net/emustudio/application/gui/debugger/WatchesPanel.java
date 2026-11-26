/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.gui.debugger.DebuggerWidgets.Operation;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugin.debugger.ValueView;
import net.emustudio.emulib.ui.components.DialogBase;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Watch expressions with their current values; edits are applied through the debugger session.
 */
final class WatchesPanel extends JPanel {
    private static final String ICONS = "/net/emustudio/application/gui/icons/inspection/";
    private static final String DEFAULT_FORMAT = "Hexadecimal";
    private static final List<String> VALUE_FORMATS = List.of("Decimal", DEFAULT_FORMAT, "Octal", "Binary", "ASCII");

    private final DebuggerSession session;
    private final Consumer<Operation> update;
    private final WatchTableModel model = new WatchTableModel();
    private final JTable watches = new JTable(model);
    private final List<String> rowFormats = new ArrayList<>();

    /**
     * @param live   runs toolbar actions (skipping them after disposal and reporting failures)
     * @param update applies a configuration change and refreshes the views afterwards
     */
    WatchesPanel(EmuStudioGui gui, DebuggerSession session, Consumer<Operation> live, Consumer<Operation> update) {
        super(new BorderLayout());
        this.session = Objects.requireNonNull(session);
        this.update = Objects.requireNonNull(update);
        watches.setTableHeader(null);
        watches.setFillsViewportHeight(true);
        watches.setShowGrid(false);
        watches.setRowHeight(Math.max(watches.getRowHeight(), watches.getFontMetrics(watches.getFont()).getHeight() + 4));
        watches.getAccessibleContext().setAccessibleName("Watch expressions and values");
        watches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watches.putClientProperty("terminateEditOnFocusLost", true);
        watches.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                showFormatMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                showFormatMenu(event);
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2
                        && watches.columnAtPoint(event.getPoint()) == 0)
                    edit(watches.rowAtPoint(event.getPoint()));
            }
        });
        watches.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                Map.Entry<String, String> entry = model.rows.get(table.convertRowIndexToModel(row));
                setToolTipText(entry.getKey() + " = " + entry.getValue());
                return this;
            }
        });
        watches.getDefaultEditor(String.class).addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent event) {
                refresh();
            }

            @Override
            public void editingCanceled(ChangeEvent event) {
                SwingUtilities.invokeLater(WatchesPanel.this::refresh);
            }
        });
        watches.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (watches.isEditing()) watches.getCellEditor().cancelCellEditing();
            }
        });
        add(gui.scrollPane(watches));

        JToolBar tools = gui.toolBar();
        tools.add(gui.toolbarButton(event -> live.accept(() -> showAddDialog(gui)), ICONS + "add-watch.png", "Add watch"));
        JButton remove = gui.toolbarButton(event -> live.accept(this::removeSelected), ICONS + "remove-watch.png", "Remove watch");
        remove.setEnabled(false);
        watches.getSelectionModel().addListSelectionListener(event -> remove.setEnabled(watches.getSelectedRow() >= 0));
        tools.add(remove);
        tools.add(gui.toolbarButton(event -> live.accept(() -> update.accept(() -> {})), ICONS + "refresh-watch.png", "Refresh"));
        tools.addSeparator();
        tools.add(gui.toolbarButton(event -> live.accept(() -> WatchHelp.show(this, gui, session.debug)),
                ICONS + "watch-help.png", "Show watch help"));
        add(gui.toolbarScrollPane(tools), BorderLayout.NORTH);
        setPreferredSize(new Dimension(280, 150));
    }

    private void showAddDialog(EmuStudioGui gui) {
        new DialogBase(SwingUtilities.getWindowAncestor(this), "Add watch", true) {
            {
                setResizable(false);
                buildContent();
            }

            @Override
            protected JComponent initializeComponents() {
                JPanel content = gui.panel("insets dialog, fillx", "[][grow]", "");
                JTextField expression = gui.textField("", 24);
                content.add(gui.label("Expression", expression));
                content.add(expression, "growx, wrap");
                JButton add = gui.button("Add");
                add.addActionListener(event -> {
                    String text = expression.getText().strip();
                    if (text.isEmpty()) {
                        expression.requestFocusInWindow();
                        return;
                    }
                    update.accept(() -> session.editWatches(expressions -> expressions.add(text)));
                    dispose();
                });
                content.add(add, "span, align right");
                getRootPane().setDefaultButton(add);
                SwingUtilities.invokeLater(expression::requestFocusInWindow);
                return content;
            }
        }.setVisible(true);
    }

    private void removeSelected() {
        int row = watches.getSelectedRow();
        if (row < 0) return;
        if (watches.isEditing()) watches.getCellEditor().cancelCellEditing();
        update.accept(() -> {
            session.editWatches(expressions -> expressions.remove(row));
            rowFormats.remove(row);
        });
    }

    private void showFormatMenu(MouseEvent event) {
        if (!event.isPopupTrigger() || watches.columnAtPoint(event.getPoint()) != 1) return;
        if (watches.isEditing() && !watches.getCellEditor().stopCellEditing()) return;
        int row = watches.rowAtPoint(event.getPoint());
        if (row < 0) return;
        watches.setRowSelectionInterval(row, row);
        String expression = model.rows.get(row).getKey();
        ValueView view = session.watchValues().get(expression);
        JPopupMenu menu = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();
        for (String format : VALUE_FORMATS) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(format, format.equals(rowFormats.get(row)));
            item.setEnabled(formatValue(view, format) != null);
            item.addActionListener(action -> {
                if (row < model.getRowCount() && expression.equals(model.rows.get(row).getKey())) {
                    rowFormats.set(row, format);
                    refresh();
                }
            });
            group.add(item);
            menu.add(item);
        }
        menu.show(watches, event.getX(), event.getY());
    }

    private static String formatValue(ValueView view, String format) {
        if (view == null) return null;
        for (ValueView.Format option : view.formats) {
            if (option.label.equals(format)) return option.text;
        }
        if (format.equals("Octal")) {
            String decimal = formatValue(view, "Decimal");
            if (decimal == null) return null;
            try {
                return "0o" + Long.toOctalString(Long.parseLong(decimal));
            } catch (NumberFormatException ignored) {
                // Custom decimal presentations may not have a corresponding octal value.
            }
        }
        return null;
    }

    void refresh() {
        if (watches.isEditing()) return;
        int selectedRow = watches.getSelectedRow();
        String selectedExpression = selected();
        Map<String, ValueView> values = session.watchValues();
        Map<String, String> errors = session.watchErrors();
        List<String> expressions = session.config.getWatches();
        while (rowFormats.size() < expressions.size()) rowFormats.add(DEFAULT_FORMAT);
        rowFormats.subList(expressions.size(), rowFormats.size()).clear();
        List<Map.Entry<String, String>> watchText = new ArrayList<>();
        for (int row = 0; row < expressions.size(); row++) {
            String expression = expressions.get(row);
            ValueView view = values.get(expression);
            if (errors.containsKey(expression))
                watchText.add(Map.entry(expression, "Inactive: " + errors.get(expression)));
            else if (view == null) watchText.add(Map.entry(expression, "Pending"));
            else {
                String formatted = Objects.requireNonNullElse(formatValue(view, rowFormats.get(row)), view.text);
                watchText.add(Map.entry(expression, formatted + (view.changed ? "  * changed" : "")));
            }
        }
        if (model.rows.equals(watchText)) return;
        model.rows = watchText;
        model.fireTableDataChanged();
        if (selectedRow >= 0 && selectedRow < watchText.size() && watchText.get(selectedRow).getKey().equals(selectedExpression)) {
            watches.setRowSelectionInterval(selectedRow, selectedRow);
        } else if (selectedExpression != null) {
            for (int row = 0; row < watchText.size(); row++)
                if (watchText.get(row).getKey().equals(selectedExpression)) {
                    watches.setRowSelectionInterval(row, row);
                    break;
                }
        }
    }

    private void edit(int row) {
        if (row >= 0 && watches.editCellAt(row, 0)) {
            JTextField editor = (JTextField) watches.getEditorComponent();
            editor.requestFocusInWindow();
            editor.selectAll();
        }
    }

    private String selected() {
        int row = watches.getSelectedRow();
        return row < 0 ? null : model.rows.get(watches.convertRowIndexToModel(row)).getKey();
    }

    private final class WatchTableModel extends AbstractTableModel {
        private List<Map.Entry<String, String>> rows = List.of();

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Expression" : "Value";
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return column == 0 ? rows.get(row).getKey() : rows.get(row).getValue();
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != 0) return;
            String original = rows.get(row).getKey();
            String expression = value.toString();
            if (original.equals(expression)) return;
            rows = new ArrayList<>(rows);
            rows.set(row, Map.entry(expression, "Pending"));
            fireTableRowsUpdated(row, row);
            update.accept(() -> session.editWatches(expressions -> {
                if (row < expressions.size() && expressions.get(row).equals(original))
                    expressions.set(row, expression);
            }));
        }
    }
}
