/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.awt.Frame;

/** Configures a new instruction recording without changing the current recording on cancellation. */
final class RecordingDialog extends DialogBase {
    private final GUI gui;
    private final Dialogs dialogs;
    private final DebugSession session;
    private final Runnable started;
    private final JComboBox<String> mode = new JComboBox<>(new String[] {
            "Keep first N instructions", "Keep last N instructions", "Custom Range", "Unlimited"});
    private final JTextField count = new JTextField("1024", 10);
    private final JTextField from = new JTextField("0x0", 10);
    private final JTextField to = new JTextField(10);

    RecordingDialog(Frame owner, GUI gui, Dialogs dialogs, DebugSession session, Runnable started) {
        super(owner, "Record instructions", true);
        this.gui = gui;
        this.dialogs = dialogs;
        this.session = session;
        this.started = started;
        mode.setSelectedIndex(1);
        to.setText(String.format("0x%X", Math.max(0, session.cpu.getAddressSpaceSize() - 1)));
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panel = gui.panel("insets dialog, fillx", "[][grow,fill][]", "");
        mode.getAccessibleContext().setAccessibleName("Capture");
        count.getAccessibleContext().setAccessibleName("Instructions");
        panel.add(mode);
        panel.add(count);
        panel.add(gui.label("instructions"), "wrap");
        addAddress(panel, "From:", from);
        addAddress(panel, "To:", to);
        mode.addActionListener(event -> updateFields());
        updateFields();
        JButton start = gui.button("Start", this::start);
        panel.add(start, "span, alignx right");
        getRootPane().setDefaultButton(start);
        return panel;
    }

    private void addAddress(JPanel panel, String text, JTextField field) {
        JLabel label = gui.label(text);
        label.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(text.substring(0, text.length() - 1));
        field.setToolTipText("Inclusive instruction address; decimal or 0x-prefixed hexadecimal");
        panel.add(label, "alignx right");
        panel.add(field, "span 2, growx, wrap");
    }

    private void updateFields() {
        count.setEnabled(mode.getSelectedIndex() < 2);
        from.setEnabled(mode.getSelectedIndex() == 2);
        to.setEnabled(mode.getSelectedIndex() == 2);
    }

    private void start() {
        try {
            DebugSession.RecordingMode selected = DebugSession.RecordingMode.values()[mode.getSelectedIndex()];
            int limit = count.isEnabled() ? Integer.parseInt(count.getText().strip()) : 0;
            int first = from.isEnabled() ? address(from) : 0;
            int last = to.isEnabled() ? address(to) : 0;
            session.startNewRecording(limit, selected, first, last);
            started.run();
            dispose();
        } catch (IllegalArgumentException | IllegalStateException failure) {
            dialogs.showError(failure instanceof NumberFormatException
                    ? "Enter a valid instruction count or address (decimal or 0x-prefixed hexadecimal)."
                    : failure.getMessage(), "Record instructions");
        }
    }

    private static int address(JTextField field) {
        String value = field.getText().strip();
        return value.startsWith("0x") || value.startsWith("0X")
                ? Integer.parseInt(value.substring(2), 16) : Integer.parseInt(value);
    }
}
