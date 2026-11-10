/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.gui.debugger.DebuggerWidgets.Operation;
import net.emustudio.application.gui.debugger.DebuggerWidgets.Selection;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import static net.emustudio.application.gui.debugger.DebuggerWidgets.*;

/**
 * Annotation search field with a popup result list, plus the annotation toolbar actions.
 * Activating a result publishes it to the shared annotation selection; previewing with arrow keys does not.
 */
final class AnnotationSearchPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationSearchPanel.class);

    private final DebuggerSession session;
    private final AnnotationEditor editor;
    private final Selection<MemoryAnnotations.Entry> selection;
    private final IntSupplier inspectedAddress;
    private final Consumer<MemoryAnnotations.Entry> showAnnotation;
    private final JTextField filter;
    private final JPopupMenu results = new JPopupMenu();
    private final JPanel resultCards = new JPanel(new CardLayout());
    private final JLabel empty = new JLabel("", SwingConstants.CENTER);
    private final DefaultListModel<MemoryAnnotations.Entry> rows = new DefaultListModel<>();
    private final JList<MemoryAnnotations.Entry> list = new JList<>(rows);
    private final JButton add, edit, delete, importAnnotations, exportAnnotations;
    private boolean closed;

    AnnotationSearchPanel(EmuStudioGui gui, DebuggerSession session, AnnotationEditor editor,
                          Selection<MemoryAnnotations.Entry> selection, JToolBar tools,
                          IntSupplier inspectedAddress, Consumer<MemoryAnnotations.Entry> showAnnotation) {
        this.session = Objects.requireNonNull(session);
        this.editor = Objects.requireNonNull(editor);
        this.selection = Objects.requireNonNull(selection);
        this.inspectedAddress = Objects.requireNonNull(inspectedAddress);
        this.showAnnotation = Objects.requireNonNull(showAnnotation);
        filter = gui.searchField("Search annotations", 14,
                "/net/emustudio/application/gui/icons/inspection/search-annotations.png");
        list.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        list.setCellRenderer(new AnnotationRenderer());
        list.getAccessibleContext().setAccessibleName("Annotations");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(event -> updateState());
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                int row = list.locationToIndex(event.getPoint());
                if (SwingUtilities.isLeftMouseButton(event) && row >= 0
                        && list.getCellBounds(row, row).contains(event.getPoint()))
                    run(AnnotationSearchPanel.this::activate);
            }
        });
        resultCards.add(gui.scrollPane(list), "list");
        resultCards.add(empty, "empty");
        results.setLayout(new BorderLayout());
        results.add(resultCards, BorderLayout.CENTER);
        results.setPreferredSize(new Dimension(320, 200));
        results.setFocusable(false);
        filter.setComponentPopupMenu(results);
        EmuStudioGui.onTextChange(() -> {
            if (closed) return;
            refresh();
            if (filter.isShowing() && filter.hasFocus()) showResults();
        }, filter);
        filter.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (!closed && SwingUtilities.isLeftMouseButton(event) && filter.isShowing()) showResults();
            }
        });
        filter.addActionListener(event -> run(this::activate));
        bindSearchKey(filter, "DOWN", () -> move(1));
        bindSearchKey(filter, "UP", () -> move(-1));
        bindSearchKey(filter, "ESCAPE", () -> results.setVisible(false));
        bindSearchKey(list, "ENTER", this::activate);
        bindSearchKey(list, "ESCAPE", () -> {
            results.setVisible(false);
            filter.requestFocusInWindow();
        });
        selection.listen(this::select);

        add = button(gui, "add-annotation", "Annotate inspected address", () -> {
            editor.showDialog(inspectedAddress.getAsInt(), "New annotation", null);
            refresh();
        });
        edit = button(gui, "edit-annotation", "Edit selected annotation", () -> {
            MemoryAnnotations.Entry entry = list.getSelectedValue();
            if (entry != null) editor.showDialog(entry.start, "Edit annotation", AnnotationEditor.editable(entry));
            refresh();
        });
        delete = button(gui, "remove-annotation", "Delete selected annotation", () -> {
            MemoryAnnotations.Entry entry = list.getSelectedValue();
            if (entry == null) return;
            if (!confirm(String.format("Delete annotation at %04X?", entry.start), "Delete annotation")) return;
            session.config.annotations().remove(AnnotationEditor.editable(entry));
            session.config.save();
            refresh();
            editor.load(entry.start);
        });
        importAnnotations = button(gui, "import-annotations", "Import annotations", () -> {
            Path path = choose(false);
            if (path == null) return;
            int skipped = session.config.loadAnnotations(path);
            session.config.save();
            refresh();
            editor.reload();
            if (skipped > 0) LOGGER.warn("Skipped {} invalid annotation(s).", skipped);
        });
        exportAnnotations = button(gui, "export-annotations", "Export annotations", () -> {
            Path path = choose(true);
            if (path != null && (!Files.exists(path) || confirm("Replace existing file?", "Export annotations")))
                session.config.saveAnnotations(path);
        });
        tools.add(filter, "growx, pushx, wmin 160");
        tools.addSeparator();
        tools.add(add);
        tools.add(edit);
        tools.add(delete);
        tools.addSeparator();
        tools.add(importAnnotations);
        tools.add(exportAnnotations);
        refresh();
    }

    private JButton button(EmuStudioGui gui, String icon, String tooltip, Operation operation) {
        return gui.toolbarButton(event -> run(operation),
                "/net/emustudio/application/gui/icons/inspection/" + icon + ".png", tooltip);
    }

    private void run(Operation operation) {
        if (closed) return;
        try { operation.run(); } catch (Exception failure) { logFailure(LOGGER, failure); }
    }

    void updateState() {
        boolean open = !closed;
        for (JComponent component : List.of(filter, add, importAnnotations, exportAnnotations))
            component.setEnabled(open);
        boolean selected = open && list.getSelectedValue() != null;
        edit.setEnabled(selected);
        delete.setEnabled(selected);
        if (!open) results.setVisible(false);
    }

    void hideResults() {
        results.setVisible(false);
    }

    void close() {
        closed = true;
        updateState();
        results.setVisible(false);
        filter.setComponentPopupMenu(null);
    }

    /** Re-reads user annotations, keeping the previewed row and refreshing the shared selection. */
    void refresh() {
        List<MemoryAnnotations.Entry> all = AnnotationEditor.userAnnotations(session.annotationEngine.all());
        MemoryAnnotations.Entry previewed = list.getSelectedValue();
        String needle = filter.getText().toLowerCase(Locale.ROOT);
        List<MemoryAnnotations.Entry> entries = all.stream()
                .filter(entry -> entry.text.toLowerCase(Locale.ROOT).contains(needle))
                .collect(Collectors.toList());
        EmuStudioGui.updateRows(rows, entries);
        if (previewed != null) list.setSelectedIndex(EmuStudioGui.rowOf(rows, entry -> entry.entryId == previewed.entryId));
        MemoryAnnotations.Entry selected = selection.get();
        if (selected != null)
            selection.set(all.stream().filter(entry -> entry.entryId == selected.entryId).findFirst().orElse(null));
        updateState();
        empty.setText(all.isEmpty()
                ? "<html>No annotations yet.<br>Select an address and create an annotation.</html>"
                : "<html>No matching annotations.<br>Change Search annotations.</html>");
        ((CardLayout) resultCards.getLayout()).show(resultCards, entries.isEmpty() ? "empty" : "list");
    }

    private void select(MemoryAnnotations.Entry annotation) {
        if (annotation == null) {
            list.clearSelection();
            return;
        }
        int row = EmuStudioGui.rowOf(rows, entry -> entry.entryId == annotation.entryId);
        if (row < 0 && !filter.getText().isEmpty()) {
            filter.setText("");
            row = EmuStudioGui.rowOf(rows, entry -> entry.entryId == annotation.entryId);
        }
        if (row >= 0) list.setSelectedIndex(row);
    }

    private void showResults() {
        results.show(filter, 0, filter.getHeight());
    }

    private void bindSearchKey(JComponent component, String key, Operation operation) {
        EmuStudioGui.bindKey(component, KeyStroke.getKeyStroke(key), "annotationSearch" + key, () -> run(operation));
    }

    private void move(int direction) {
        if (filter.isShowing() && !results.isVisible()) showResults();
        if (rows.isEmpty()) return;
        int selected = list.getSelectedIndex();
        int next = selected < 0 ? (direction > 0 ? 0 : rows.size() - 1)
                : Math.max(0, Math.min(rows.size() - 1, selected + direction));
        list.setSelectedIndex(next);
        list.ensureIndexIsVisible(next);
    }

    private void activate() {
        if (list.getSelectedIndex() < 0 && !rows.isEmpty()) list.setSelectedIndex(0);
        MemoryAnnotations.Entry entry = list.getSelectedValue();
        if (entry == null) return;
        editor.loadEntry(AnnotationEditor.editable(entry));
        selection.set(entry);
        results.setVisible(false);
        showAnnotation.accept(entry);
    }

    private boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(filter, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private Path choose(boolean save) {
        JFileChooser chooser = new JFileChooser();
        int result = save ? chooser.showSaveDialog(filter) : chooser.showOpenDialog(filter);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }
}
