/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.InstructionRecording;
import net.emustudio.emulib.application.ui.GUI;
import net.emustudio.emulib.ui.components.DialogBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Frame;
import java.util.concurrent.CompletionException;

/** Configures a new instruction recording without changing the current recording on cancellation. */
public final class RecordingDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingDialog.class);
    private final GUI gui;
    private final DebuggerSession session;
    private final Runnable started;
    private final JComboBox<String> mode = new JComboBox<>(new String[] {
            "Keep first N", "Keep last N", "Custom Range", "Unlimited"});
    private JButton start;
    private final JTextField count = new JTextField("1024", 10);
    private final JTextField from = new JTextField("0x0", 10);
    private final JTextField to = new JTextField(10);

    public RecordingDialog(Frame owner, GUI gui, DebuggerSession session, Runnable started) {
        super(owner, "Record instructions", true);
        this.gui = gui;
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
        start = gui.button("Start");
        start.addActionListener(e -> start());
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
            InstructionRecording.Mode selected = InstructionRecording.Mode.values()[mode.getSelectedIndex()];
            int limit = count.isEnabled() ? Integer.parseInt(count.getText().strip()) : 0;
            int first = from.isEnabled() ? address(from) : 0;
            int last = to.isEnabled() ? address(to) : 0;
            session.update(() -> session.startNewRecording(limit, selected, first, last)).join();
            started.run();
            dispose();
        } catch (RuntimeException failure) {
            Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                    ? failure.getCause() : failure;
            LOGGER.warn(cause instanceof NumberFormatException
                    ? "Enter a valid instruction count or address (decimal or 0x-prefixed hexadecimal)."
                    : cause.getMessage(), cause);
        }
    }

    private static int address(JTextField field) {
        String value = field.getText().strip();
        return value.startsWith("0x") || value.startsWith("0X")
                ? Integer.parseInt(value.substring(2), 16) : Integer.parseInt(value);
    }
}
