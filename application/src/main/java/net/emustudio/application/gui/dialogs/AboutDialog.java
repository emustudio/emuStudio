/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;


import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.Objects;

import static net.emustudio.application.Resources.getCopyright;
import static net.emustudio.application.Resources.getVersion;

public class AboutDialog extends DialogBase {
    private final EmuStudioGui gui;

    public AboutDialog(JFrame parent, EmuStudioGui gui) {
        super(parent, "About emuStudio", true);
        this.gui = Objects.requireNonNull(gui);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        // Main panel with horizontal layout
        JPanel mainPanel = gui.panel("insets 10", "[][grow]", "[]");
        JLabel lblLogo = gui.createLogoJLabel();

        // Info panel
        JPanel infoPanel = gui.panel("insets 10", "[grow]", "[]");
        infoPanel.add(gui.labelTitle("emuStudio"), "wrap, gapbottom 10");
        infoPanel.add(gui.label(getCopyright()), "wrap, gapbottom 10");

        infoPanel.add(gui.label("Version: "), "split 2");
        infoPanel.add(gui.labelBold(getVersion()), "wrap, gapbottom 10");

        JTextArea licenseInfo = gui.textAreaReadOnly(4, 42);
        licenseInfo.setText("This program comes with ABSOLUTELY NO WARRANTY. "
                + "This is free software, and you are welcome to redistribute it under certain conditions.");
        licenseInfo.setFont(UIManager.getFont("Label.font"));
        licenseInfo.setLineWrap(true);
        licenseInfo.setWrapStyleWord(true);
        licenseInfo.setOpaque(false);
        licenseInfo.getAccessibleContext().setAccessibleName("License notice");
        infoPanel.add(licenseInfo, "wrap, growx");
        JPanel links = gui.panel("insets 0", "[]12[]", "[]");
        links.add(link("emuStudio website", "https://www.emustudio.net/"));
        links.add(link("GNU General Public License", "https://www.gnu.org/licenses/gpl-3.0.html"));
        infoPanel.add(links, "growx");

        // Combine panels
        mainPanel.add(lblLogo, "");
        mainPanel.add(infoPanel, "grow");

        return mainPanel;
    }

    private JButton link(String text, String address) {
        JButton button = new JButton(text);
        button.setToolTipText(address);
        button.addActionListener(event -> {
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                    throw new UnsupportedOperationException();
                Desktop.getDesktop().browse(URI.create(address));
            } catch (Exception e) {
                JTextField location = new JTextField(address);
                location.setEditable(false);
                location.selectAll();
                JOptionPane.showMessageDialog(this,
                        new Object[]{"Open this address in your browser:", location}, text, JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return button;
    }
}
