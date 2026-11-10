/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.UserAnnotations;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.ui.components.DialogBase;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.emustudio.application.gui.debugger.DebuggerWidgets.*;
import static net.emustudio.emulib.ui.StandardIcons.ICON_SAVE;

/** Edits and persists one user annotation independently of CPU execution. */
final class AnnotationEditor extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationEditor.class);

    private static final List<String> KINDS = List.of("CODE", "DATA");

    private final Component owner;
    private final DebuggerSession session;
    private final JTextField addressField, endField, labelField;
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"Code", "Data"});
    private final JButton save;
    private int address;
    private UserAnnotations.Entry original;
    private boolean loading;
    private Runnable onSaved = () -> {};

    AnnotationEditor(EmuStudioGui gui, Component owner, DebuggerSession session) {
        super(new MigLayout("insets dialog, fillx, hidemode 3", "[]12[grow,fill]", "[]8[]8[]8[]16[]"));
        this.owner = Objects.requireNonNull(owner);
        this.session = Objects.requireNonNull(session);
        addressField = gui.textField("", 12);
        endField = gui.textField("", 12);
        labelField = gui.textField("", 20);
        addressField.setToolTipText("Decimal or hexadecimal address, for example 42 or 0x2A");
        add(gui.label("From:", addressField));
        add(addressField, "growx, wrap");
        endField.setToolTipText("Last address included in the annotation");
        add(gui.label("To:", endField));
        add(endField, "growx, wrap");
        add(gui.label("Label", labelField));
        add(labelField, "growx, wrap");
        add(gui.label("Type", kind));
        add(kind, "wmin 100, alignx left, growx 0, wrap");
        save = gui.button("Save");
        save.getAccessibleContext().setAccessibleName("Save annotation");
        save.setIcon(EmuStudioGui.loadIcon(ICON_SAVE, AnnotationEditor.class));
        save.setToolTipText("Save annotation");
        save.addActionListener(event -> {
            try {
                saveDraft();
                onSaved.run();
            } catch (Exception failure) {
                logFailure(LOGGER, failure);
            }
        });
        save.setName("saveAnnotation");
        add(save, "span, alignx right, wmin 88");
        EmuStudioGui.onTextChange(this::updateState, labelField, addressField, endField);
        kind.addActionListener(event -> updateState());
        labelField.addActionListener(event -> save.doClick());
        updateState();
    }

    static UserAnnotations.Entry editable(MemoryAnnotations.Entry entry) {
        return new UserAnnotations.Entry(entry.entryId, entry.start, entry.end, entry.text, entry.kind);
    }

    static List<MemoryAnnotations.Entry> userAnnotations(List<MemoryAnnotations.Entry> entries) {
        return entries.stream()
                .filter(entry -> entry.ownerId == UserAnnotations.OWNER)
                .collect(Collectors.toList());
    }

    /** Shows a modal editor for the entry, or for a new annotation at the address when the entry is null. */
    void showDialog(int at, String title, UserAnnotations.Entry entry) {
        load(at, entry);
        Window window = SwingUtilities.getWindowAncestor(owner);
        DialogBase dialog = new DialogBase(window instanceof Frame ? (Frame) window : null, title, true) {
            { setResizable(false); buildContent(); }
            @Override protected JComponent initializeComponents() { return AnnotationEditor.this; }
        };
        onSaved = dialog::dispose;
        dialog.getRootPane().setDefaultButton(save);
        SwingUtilities.invokeLater(addressField::requestFocusInWindow);
        dialog.setVisible(true);
        onSaved = () -> {};
        load(at);
    }

    void reload() {
        load(address);
    }

    void load(int at) {
        UserAnnotations annotations = session.cpu.getAddressSpaceSize() > 0 ? session.config.annotations() : null;
        load(at, annotations == null ? null : annotations.get(at));
    }

    /** Loads the entry, or a new annotation at the address when the entry is null. */
    private void load(int at, UserAnnotations.Entry entry) {
        loadEntry(entry == null ? new UserAnnotations.Entry(at, "", "CODE") : entry);
    }

    void loadEntry(UserAnnotations.Entry entry) {
        loading = true;
        try {
            address = entry.address;
            addressField.setText(hex(address));
            endField.setText(hex(entry.end - 1));
            original = entry;
            labelField.setText(entry.label);
            kind.setSelectedIndex(KINDS.indexOf(entry.kind));
        } finally {
            loading = false;
        }
        updateState();
    }

    private static String hex(int value) {
        return String.format("0x%04X", value);
    }

    private boolean dirty() {
        return original != null && (!original.label.equals(labelField.getText())
                || !original.kind.equals(KINDS.get(kind.getSelectedIndex()))
                || !addressField.getText().equalsIgnoreCase(hex(original.address))
                || !endField.getText().equalsIgnoreCase(hex(original.end - 1)));
    }

    void updateState() {
        if (loading) return;
        save.setEnabled(original != null && (dirty() || original.id < 0));
    }

    private void saveDraft() throws Exception {
        if (original == null || (original.id >= 0 && !dirty())) return;
        int target, end;
        try {
            target = Numbers.parseRadix(addressField.getText());
            end = Numbers.parseRadix(endField.getText());
            if (target < 0 || end < target || end >= session.addressSpaceSize()) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid address. Enter a value from 0 to "
                    + (session.addressSpaceSize() - 1) + ".");
        }
        address = target;
        UserAnnotations.Entry draft = new UserAnnotations.Entry(original.id, address, end + 1, labelField.getText(),
                KINDS.get(kind.getSelectedIndex()));
        UserAnnotations annotations = session.config.annotations();
        if (draft.id < 0) draft = annotations.add(draft);
        else annotations.update(draft);
        loadEntry(draft);
        session.config.save();
    }

}
