/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;


import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.application.Resources.getCopyright;
import static net.emustudio.application.Resources.getVersion;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class AboutDialog extends DialogBase {
    public final static String LOGO_FILE = "/net/emustudio/application/gui/dialogs/logo.png";

    public AboutDialog(JFrame parent) {
        super(parent, "About emuStudio", true);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        // Main panel with horizontal layout
        JPanel mainPanel = new JPanel(new MigLayout("insets 10", "[][grow]", "[]"));

        // Logo panel
        JLabel lblLogo = new JLabel(loadIcon(LOGO_FILE));
        lblLogo.setBackground(Color.WHITE);
        lblLogo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        lblLogo.setOpaque(true);

        // Info panel
        JPanel infoPanel = new JPanel(new MigLayout("insets 10", "[grow]", "[]"));
        infoPanel.add(GUI.titleLabel("emuStudio"), "wrap, gapbottom 10");
        infoPanel.add(GUI.label(getCopyright()), "wrap, gapbottom 10");

        infoPanel.add(GUI.label("Version: "), "split 2");
        infoPanel.add(GUI.boldLabel(getVersion()), "wrap, gapbottom 10");

        JLabel licenseInfo = new JLabel(
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
