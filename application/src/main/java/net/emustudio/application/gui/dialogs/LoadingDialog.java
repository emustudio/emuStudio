/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.application.gui.GUIProvider;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_LOADING;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class LoadingDialog extends DialogBase {

    public LoadingDialog() {
        super((JFrame) null, "emuStudio", false);
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panel = GUIProvider.getGUI().panelHorizontal();

        JLabel lblLoading = GUIProvider.getGUI().labelBold("Loading computer, please wait...");
        lblLoading.setIcon(loadIcon(ICON_LOADING));

        panel.add(lblLoading, "wrap, gapbottom 10");
        panel.add(GUIProvider.getGUI().label("If you see some errors, please see the log file."), "wrap");

        return panel;
    }
}
