/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Simple base class for emuStudio dialogs with modern UI.
 * Provides common functionality and styling.
 * Subclasses must implement buildContent() and call showContent() at the end of their constructor.
 */
public abstract class EDialog extends JDialog {

    protected EDialog(Frame parent, String title, boolean modal) {
        super(parent, title, modal);
        initDialog();
    }

    protected EDialog(Dialog parent, String title, boolean modal) {
        super(parent, title, modal);
        initDialog();
    }

    private void initDialog() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ESC key closes dialog
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    /**
     * Call this method at the end of your constructor to build and display the content.
     * This calls buildContent(), adds it to the dialog, packs it, and centers it.
     */
    protected void buildContent() {
        JComponent content = initializeComponents();
        if (content != null) {
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(content, BorderLayout.CENTER);
            pack();
        }
        setLocationRelativeTo(getParent());
    }

    /**
     * Build the dialog content. Called by showContent().
     * This replaces the traditional initComponents() method.
     */
    protected abstract JComponent initializeComponents();
}
