/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

/** File search and a selectable replacement preview. */
final class FilesSearchDialog extends JDialog {
    private final TabbedEditor editor;
    private final JComboBox<String> scope = new JComboBox<>(new String[]{"Open files", "Folder"});
    private final JTextField folder = new JTextField(24), include = new JTextField("*", 15), exclude = new JTextField("*.class;*.jar;*.zip", 15);
    private final JLabel status = new JLabel("Search to preview matches");
    private final PreviewModel model = new PreviewModel();
    private final JTable table = new JTable(model);
    private SwingWorker<List<SearchWorkspace.Snapshot>, Void> worker;
    private final JButton apply = new JButton("Apply checked replacements"), find = new JButton("Search");
    private static final class Row {
        final SearchWorkspace.Snapshot snapshot; final EditorSearch.Match match; boolean checked = true;
        Row(SearchWorkspace.Snapshot snapshot, EditorSearch.Match match) { this.snapshot = snapshot; this.match = match; }

        String file() {
            if (snapshot.path == null) return "Untitled";
            Path parent = snapshot.path.getParent();
            return snapshot.path.getFileName() + (parent == null ? "" : " — " + parent);
        }

        String context(boolean replacement, boolean html) {
            int from = snapshot.text.lastIndexOf('\n', match.start - 1) + 1;
            int to = snapshot.text.indexOf('\n', match.end);
            if (to < 0) to = snapshot.text.length();
            String prefix = snapshot.text.substring(from, match.start);
            String value = replacement ? match.after : match.before;
            String suffix = snapshot.text.substring(match.end, to);
            return html ? "<html>" + escape(prefix) + "<b>" + escape(value) + "</b>" + escape(suffix) + "</html>"
                    : printable(prefix + value + suffix);
        }

        private static String printable(String text) {
            return text.replace("\r", "").replace("\n", "\\n").replace("\t", "    ");
        }

        private static String escape(String text) {
            return printable(text).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;");
        }
    }
    private static final class PreviewModel extends AbstractTableModel {
        final List<Row> rows = new ArrayList<>();
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return 5; }
        public String getColumnName(int column) { return new String[]{"Apply", "File", "Line", "Before", "After"}[column]; }
        public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : String.class; }
        public boolean isCellEditable(int row, int column) { return column == 0; }
        public void setValueAt(Object value, int row, int column) { rows.get(row).checked = Boolean.TRUE.equals(value); fireTableCellUpdated(row, column); }
        public Object getValueAt(int row, int column) {
            Row value = rows.get(row);
            switch (column) {
                case 0: return value.checked;
                case 1: return value.file();
                case 2: return Integer.toString(value.snapshot.line(value.match.start));
                case 3: return value.context(false, false);
                default: return value.context(true, false);
            }
        }
    }
    FilesSearchDialog(TabbedEditor editor, InlineSearchBar search) {
        super(SwingUtilities.getWindowAncestor(editor.getView()), "Find / replace in files", ModalityType.MODELESS);
        this.editor = editor;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel controls = new JPanel(new net.miginfocom.swing.MigLayout("insets dialog, fillx, hidemode 3", "[][grow,fill]", ""));
        JTextField query = new JTextField(search.find.getText()), replacement = new JTextField(search.replace.getText());
        JCheckBox regex = new JCheckBox("Regex", search.regex.isSelected()), matchCase = new JCheckBox("Match case", search.matchCase.isSelected()),
                word = new JCheckBox("Whole word", search.word.isSelected()), preserve = new JCheckBox("Preserve case", search.preserve.isSelected());
        field(controls, "Find", query);
        field(controls, "Replace", replacement);
        JPanel matching = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        matching.add(matchCase); matching.add(word); matching.add(regex); matching.add(preserve);
        controls.add(matching, "span, growx, wrap");
        field(controls, "Scope", scope);
        JPanel folderOptions = new JPanel(new net.miginfocom.swing.MigLayout("insets 0, fillx", "[][grow,fill]", ""));
        JPanel location = new JPanel(new BorderLayout()); location.add(folder);
        JButton browse = new JButton("Browse…"); location.add(browse, BorderLayout.EAST);
        field(folderOptions, "Folder", location).setLabelFor(folder);
        folder.getAccessibleContext().setAccessibleName("Search folder");
        field(folderOptions, "Include files", include);
        field(folderOptions, "Exclude files", exclude);
        include.setToolTipText("Glob patterns separated by semicolons, e.g. *.asm;*.txt");
        exclude.setToolTipText("Glob patterns separated by semicolons, e.g. *.class;*.jar;*.zip");
        controls.add(folderOptions, "span, growx, wrap");
        scope.addActionListener(e -> {
            folderOptions.setVisible(scope.getSelectedIndex() == 1);
            controls.revalidate();
            controls.repaint();
        });
        folderOptions.setVisible(false);
        editor.getCurrentFile().ifPresent(file -> folder.setText(file.getParent()));
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(folder.getText()); chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { folder.setText(chooser.getSelectedFile().getPath()); scope.setSelectedIndex(1); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING)); buttons.add(find); buttons.add(apply); buttons.add(status);
        buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        apply.setEnabled(false);
        add(controls, BorderLayout.NORTH); add(new JScrollPane(table), BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
        table.setAutoCreateRowSorter(true); table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.getAccessibleContext().setAccessibleName("Replacement preview");
        table.setFont(FONT_MONOSPACED);
        table.setFillsViewportHeight(true);
        table.setRowHeight(Math.max(table.getRowHeight(), table.getFontMetrics(table.getFont()).getHeight() + 8));
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                Row match = model.rows.get(table.convertRowIndexToModel(row));
                int modelColumn = table.convertColumnIndexToModel(column);
                if (modelColumn == 3 || modelColumn == 4) setText(match.context(modelColumn == 4, true));
                setToolTipText(modelColumn == 1 ? match.snapshot.title() : value == null ? null : value.toString());
                return this;
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) navigate(); }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "navigate");
        table.getActionMap().put("navigate", new AbstractAction() { public void actionPerformed(ActionEvent e) { navigate(); } });
        find.addActionListener(e -> {
            List<SearchWorkspace.Snapshot> open = SearchWorkspace.open(editor.editors());
            String findText = query.getText(), replaceText = replacement.getText(), includes = include.getText(), excludes = exclude.getText(), directory = folder.getText();
            if (findText.isEmpty()) { status.setText("Enter text to find."); query.requestFocusInWindow(); return; }
            boolean useFolder = scope.getSelectedIndex() == 1, useRegex = regex.isSelected(), useCase = matchCase.isSelected(), useWord = word.isSelected(), usePreserve = preserve.isSelected();
            apply.setEnabled(false); find.setEnabled(false); status.setText("Searching…");
            worker = new SwingWorker<>() {
                protected List<SearchWorkspace.Snapshot> doInBackground() throws Exception {
                    List<SearchWorkspace.Snapshot> snapshots = useFolder ? SearchWorkspace.folder(Path.of(directory), includes, excludes, open) : open;
                    for (SearchWorkspace.Snapshot snapshot : snapshots) snapshot.matches.addAll(EditorSearch.find(snapshot.text, findText, replaceText, useRegex, useCase, useWord, usePreserve, 0, snapshot.text.length()));
                    return snapshots;
                }
                protected void done() {
                    find.setEnabled(true);
                    if (!isDisplayable()) return;
                    try {
                        model.rows.clear(); int files = 0;
                        for (SearchWorkspace.Snapshot snapshot : get()) {
                            if (!snapshot.matches.isEmpty()) files++;
                            for (EditorSearch.Match match : snapshot.matches) model.rows.add(new Row(snapshot, match));
                        }
                        model.fireTableDataChanged(); status.setText(model.rows.size() + " matches in " + files + " files"); apply.setEnabled(!model.rows.isEmpty());
                    } catch (Exception ex) { status.setText("Search failed"); showError(ex.getCause() == null ? ex : ex.getCause()); }
                }
            }; worker.execute();
        });
        apply.addActionListener(e -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            Map<SearchWorkspace.Snapshot, List<EditorSearch.Match>> reviewed = new LinkedHashMap<>();
            for (Row row : model.rows) if (row.checked) reviewed.computeIfAbsent(row.snapshot, key -> new ArrayList<>()).add(row.match);
            if (reviewed.isEmpty()) return;
            try { SearchWorkspace.apply(reviewed); status.setText("Replacements applied; open buffers need saving"); apply.setEnabled(false); }
            catch (IOException ex) { showError(ex); }
        });
        addWindowListener(new WindowAdapter() { public void windowClosed(WindowEvent e) { if (worker != null) worker.cancel(true); } });
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(1050, 620); setLocationRelativeTo(editor.getView());
    }
    private static JLabel field(JPanel panel, String text, JComponent input) {
        JLabel label = new JLabel(text);
        label.setLabelFor(input);
        input.getAccessibleContext().setAccessibleName(text);
        panel.add(label);
        panel.add(input, "growx, wrap");
        return label;
    }
    private void navigate() {
        int row = table.getSelectedRow(); if (row < 0) return;
        Row selected = model.rows.get(table.convertRowIndexToModel(row));
        if (selected.snapshot.path != null && !editor.openFile(selected.snapshot.path)) return;
        if (selected.snapshot.path == null) editor.activate(selected.snapshot.buffer);
        String source = selected.snapshot.text;
        String loaded = editor.activeEditor().textPane().getText();
        boolean normalized = !loaded.equals(source) && loaded.equals(source.replace("\r\n", "\n").replace('\r', '\n'));
        if (!loaded.equals(source) && !normalized) {
            showError(new IllegalArgumentException("Source changed since search. Search again.")); return;
        }
        int start = selected.match.start, end = selected.match.end;
        if (normalized) {
            start = source.substring(0, start).replace("\r\n", "\n").length();
            end = source.substring(0, end).replace("\r\n", "\n").length();
        }
        editor.activeEditor().textPane().select(start, end);
        editor.activeEditor().grabFocus();
    }
    private void showError(Throwable ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Search / replace", JOptionPane.ERROR_MESSAGE); }
}
