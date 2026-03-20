/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;


import net.emustudio.application.gui.framework.EmuStudioUI;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.application.gui.GUIProvider;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.application.Resources.getCopyright;
import static net.emustudio.application.Resources.getVersion;

public class AboutDialog extends DialogBase {

    public AboutDialog(JFrame parent) {
        super(parent, "About emuStudio", true);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        // Main panel with horizontal layout
        JPanel mainPanel = GUIProvider.getGUI().panel("insets 10", "[][grow]", "[]");
        JLabel lblLogo = EmuStudioUI.createLogoJLabel();

        // Info panel
        JPanel infoPanel = GUIProvider.getGUI().panel("insets 10", "[grow]", "[]");
        infoPanel.add(GUIProvider.getGUI().labelTitle("emuStudio"), "wrap, gapbottom 10");
        infoPanel.add(GUIProvider.getGUI().label(getCopyright()), "wrap, gapbottom 10");

        infoPanel.add(GUIProvider.getGUI().label("Version: "), "split 2");
        infoPanel.add(GUIProvider.getGUI().labelBold(getVersion()), "wrap, gapbottom 10");

        JLabel licenseInfo = GUIProvider.getGUI().label(
                "<html><p>This program comes with ABSOLUTELY NO WARRANTY. " +
                        "This is free software, and you are welcome to redistribute it " +
                        "under certain conditions; for details see " +
                        "https://www.gnu.org/licenses/gpl-3.0.html.<br/>" +
                        "For more information about emuStudio, see https://www.emustudio.net/.</p></html>"
        );
        licenseInfo.setPreferredSize(new Dimension(400, 80));
        infoPanel.add(licenseInfo, "wrap, grow");

        // Combine panels
        mainPanel.add(lblLogo, "");
        mainPanel.add(infoPanel, "grow");

        return mainPanel;
    }
}
