/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * Simple input dialog extending DialogBase so that ESC properly closes it.
 */
public class InputDialog extends DialogBase {
    private final String message;
    private final JTextField txtInput;
    private String result;

    private InputDialog(Component parent, String message, String title, Object initialValue) {
        super(parent instanceof Frame ? (Frame) parent : (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent),
                title, true);
        this.message = message;
        this.txtInput = new JTextField(String.valueOf(initialValue), 20);
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panel = GUI.panel("insets dialog", "[grow]", "[]6[]6[]");
        panel.add(GUI.label(message), "wrap, gapbottom 5");
        panel.add(txtInput, "growx, wrap, gapbottom 10");

        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        btnOk.addActionListener(e -> {
            result = txtInput.getText();
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(btnOk);

        JPanel buttonPanel = GUI.panel("insets 0", "push[][]", "[]");
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnOk);
        panel.add(buttonPanel, "growx, align right");

        SwingUtilities.invokeLater(txtInput::selectAll);
        return panel;
    }

    /**
     * Shows an input dialog and returns the user's input, or empty if cancelled/ESC.
     */
    public static Optional<String> showInputDialog(Component parent, String message, String title, Object initialValue) {
        InputDialog dialog = new InputDialog(parent, message, title, initialValue);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }
}
