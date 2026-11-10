/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.gui.debugger.DebuggerWidgets.Operation;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.plugin.debugger.ValueView;

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

import static net.emustudio.application.gui.debugger.DebuggerWidgets.*;
import static net.emustudio.emulib.ui.StandardIcons.*;

/**
 * Watch expressions with their current values; edits are applied through the debugger session.
 */
final class WatchesPanel extends JPanel {
    private final DebuggerSession session;
    private final Consumer<Operation> update;
    private final WatchTableModel model = new WatchTableModel();
    private final JTable watches = new JTable(model);
    private final JComboBox<String> format = new JComboBox<>();
    private boolean updatingFormats;

    /**
     * @param live   runs toolbar actions (skipping them after disposal and reporting failures)
     * @param update applies a configuration change asynchronously and refreshes the views afterwards
     */
    WatchesPanel(EmuStudioGui gui, DebuggerSession session, Consumer<Operation> live, Consumer<Operation> update) {
        super(new BorderLayout(4, 4));
        this.session = Objects.requireNonNull(session);
        this.update = Objects.requireNonNull(update);
        watches.setTableHeader(null);
        watches.setFont(EmuStudioGui.monospaced(watches));
        watches.getAccessibleContext().setAccessibleName("Watch expressions and values");
        watches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watches.putClientProperty("terminateEditOnFocusLost", true);
        watches.addMouseListener(new MouseAdapter() {
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

        JTextField expression = gui.textField("", 16);
        expression.setToolTipText("Machine expression, e.g. A or mem[0x1234]; invalid text is saved and shown inactive until corrected.");
        JButton add = gui.toolbarButton(event -> live.accept(() -> {
            String text = expression.getText();
            update.accept(() -> {
                session.config.watches.add(text);
                session.config.save();
            });
            expression.setText("");
        }), ICON_ADD, "Add");
        expression.addActionListener(e -> add.doClick());
        format.addActionListener(e -> {
            if (!updatingFormats) refresh();
        });
        format.setToolTipText("Watch value format");
        format.getAccessibleContext().setAccessibleName("Watch value format");
        JPanel entry = gui.panel("insets 4, fillx", "[][grow][]", "");
        entry.add(gui.label("Expression", expression));
        entry.add(expression, "growx");
        JToolBar entryActions = gui.toolBar();
        entryActions.add(add);
        entry.add(entryActions);
        JPanel tools = gui.panel("insets 4, fillx, hidemode 3", "[grow]", "");
        tools.add(entry, "span, growx, wrap");
        JToolBar watchTools = gui.toolbarRow(tools, gui.toolbarButton(event -> live.accept(() -> {
                    String selected = selected();
                    if (selected != null) update.accept(() -> {
                        session.config.watches.remove(selected);
                        session.config.save();
                    });
                }), ICON_REMOVE, "Remove"),
                gui.toolbarButton(event -> live.accept(() -> edit(watches.getSelectedRow())), ICON_RENAME, "Edit watch"),
                gui.toolbarButton(event -> live.accept(() -> update.accept(() -> {})), ICON_REFRESH, "Refresh"));
        watchTools.addSeparator();
        watchTools.add(format);
        add(gui.toolbarScrollPane(tools), BorderLayout.NORTH);
        setPreferredSize(new Dimension(280, 230));
    }

    void refresh() {
        if (watches.isEditing()) return;
        String selectedExpression = selected();
        Map<String, ValueView> values = session.watchValues();
        Map<String, String> errors = session.watchErrors();
        String selectedFormat = (String) format.getSelectedItem();
        Set<String> formats = new LinkedHashSet<>();
        formats.add("Default");
        values.values().forEach(value -> value.formats.forEach(item -> formats.add(item.label)));
        List<String> displayedFormats = new ArrayList<>();
        for (int i = 0; i < format.getItemCount(); i++) displayedFormats.add(format.getItemAt(i));
        if (!displayedFormats.equals(new ArrayList<>(formats))) {
            updatingFormats = true;
            try {
                format.removeAllItems();
                formats.forEach(format::addItem);
                if (formats.contains(selectedFormat)) format.setSelectedItem(selectedFormat);
            } finally {
                updatingFormats = false;
            }
        }
        List<Map.Entry<String, String>> watchText = new ArrayList<>();
        for (String expression : session.config.watches) {
            ValueView view = values.get(expression);
            if (errors.containsKey(expression))
                watchText.add(Map.entry(expression, "Inactive: " + errors.get(expression)));
            else if (view == null) watchText.add(Map.entry(expression, "Pending"));
            else {
                String formatted = view.text;
                for (ValueView.Format item : view.formats)
                    if (item.label.equals(selectedFormat)) formatted = item.text;
                watchText.add(Map.entry(expression, formatted + (view.changed ? "  * changed" : "")));
            }
        }
        if (model.rows.equals(watchText)) return;
        model.rows = watchText;
        model.fireTableDataChanged();
        if (selectedExpression != null)
            for (int row = 0; row < watchText.size(); row++)
                if (watchText.get(row).getKey().equals(selectedExpression)) {
                    watches.setRowSelectionInterval(row, row);
                    break;
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
            update.accept(() -> {
                if (row < session.config.watches.size() && session.config.watches.get(row).equals(original))
                    session.config.watches.set(row, expression);
                session.config.save();
            });
        }
    }
}
