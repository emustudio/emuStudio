/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.framework.EDialog;
import net.emustudio.application.gui.framework.EPanel;

import javax.swing.*;

import static net.emustudio.application.gui.framework.EmuStudioUI.*;
import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class LoadingDialog extends EDialog {
    private final static String ICON_FILE = "/net/emustudio/application/gui/dialogs/loading.gif";

    public LoadingDialog() {
        super((JFrame) null, "emuStudio", false);
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        EPanel panel = EPanel.vertical();

        JLabel lblLoading = new JLabel(loadIcon(ICON_FILE));
        lblLoading.setFont(lblLoading.getFont().deriveFont(lblLoading.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoading.setText("Loading computer, please wait...");

        panel.add(lblLoading, "wrap, gapbottom 10");
        panel.add(label("If you see some errors, please see the log file."), "wrap");

        return panel;
    }
}
