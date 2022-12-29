/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.adm3a.TerminalSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.device.adm3a.TerminalSettings.DEFAULT_INPUT_FILE_NAME;
import static net.emustudio.plugins.device.adm3a.TerminalSettings.DEFAULT_OUTPUT_FILE_NAME;

public class SettingsDialog extends JDialog {
    private final TerminalSettings settings;
    private final TerminalWindow window;
    private final Dialogs dialogs;
    private final JCheckBox chkAlwaysOnTop = new JCheckBox("Display always on top");
    private final JCheckBox chkHalfDuplex = new JCheckBox("Half duplex mode");
    private final JSpinner spnInputDelay = new JSpinner();
    private final JTextField txtInputFileName = new JTextField(DEFAULT_INPUT_FILE_NAME);
    private final JTextField txtOutputFileName = new JTextField(DEFAULT_OUTPUT_FILE_NAME);
    private final JComboBox<Integer> cmbFont = new JComboBox<>(new Integer[]{0, 1});

    public SettingsDialog(JFrame parent, TerminalSettings settings, TerminalWindow window, Dialogs dialogs) {
        super(parent, true);

        this.dialogs = Objects.requireNonNull(dialogs);
        this.settings = Objects.requireNonNull(settings);
        this.window = window;

        initComponents();

        readSettings();
        setLocationRelativeTo(parent);
    }

    private void readSettings() {
        chkHalfDuplex.setSelected(settings.isHalfDuplex());
        chkAlwaysOnTop.setSelected(settings.isAlwaysOnTop());
        txtInputFileName.setText(settings.getInputPath().toString());
        txtOutputFileName.setText(settings.getOutputPath().toString());
        spnInputDelay.setValue(settings.getInputReadDelay());
    }

    private void updateSettings() throws IOException {
        settings.setHalfDuplex(chkHalfDuplex.isSelected());
        settings.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        settings.setInputPath(Path.of(txtInputFileName.getText()));
        settings.setOutputPath(Path.of(txtOutputFileName.getText()));
        settings.setInputReadDelay((Integer) spnInputDelay.getValue());
        getSelectedFont().ifPresent(settings::setFont);

        settings.write();
    }

    private void initComponents() {
        JPanel panelRedirectIO = new JPanel();
        JLabel lblInputFileName = new JLabel("Input file name:");
        JButton btnInputBrowse = new JButton("Browse...");
        JLabel lblOutputFileName = new JLabel("Output file name:");
        JButton btnOutputBrowse = new JButton("Browse...");
        JLabel lblNote = new JLabel("Note: I/O redirection will be used only in case of No GUI mode.");
        JLabel lblInputDelay = new JLabel("Input delay:");
        JLabel lblMs = new JLabel("ms");
        JPanel panelTerminal = new JPanel();
        JButton btnSave = new JButton("Save");
        JLabel lblFont = new JLabel("Font");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("LSI ADM-3A Settings");

        panelRedirectIO.setBorder(BorderFactory.createTitledBorder(
                null, "Redirect I/O", 0, 0,
                lblInputFileName.getFont().deriveFont(lblInputFileName.getFont().getStyle() | Font.BOLD)
        ));

        cmbFont.setRenderer(new DisplayFontJComboRenderer());
        cmbFont.setSelectedIndex(settings.getFont().ordinal());

        spnInputDelay.setModel(new SpinnerNumberModel(0, 0, null, 100));

        btnSave.addActionListener(this::btnSaveActionPerformed);
        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.setDefaultCapable(true);

        GroupLayout layoutRedirectIO = new GroupLayout(panelRedirectIO);
        panelRedirectIO.setLayout(layoutRedirectIO);
        layoutRedirectIO.setHorizontalGroup(
                layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layoutRedirectIO.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblNote)
                                        .addGroup(layoutRedirectIO.createSequentialGroup()
                                                .addGroup(layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addGroup(layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                .addComponent(lblInputFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(lblOutputFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(lblInputDelay))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addGroup(layoutRedirectIO.createSequentialGroup()
                                                                .addComponent(txtOutputFileName, GroupLayout.PREFERRED_SIZE, 241, Short.MAX_VALUE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnOutputBrowse))
                                                        .addGroup(GroupLayout.Alignment.TRAILING, layoutRedirectIO.createSequentialGroup()
                                                                .addComponent(txtInputFileName, GroupLayout.PREFERRED_SIZE, 241, Short.MAX_VALUE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnInputBrowse))
                                                        .addGroup(layoutRedirectIO.createSequentialGroup()
                                                                .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblMs)))))
                                .addContainerGap())
        );
        layoutRedirectIO.setVerticalGroup(
                layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layoutRedirectIO.createSequentialGroup()
                                .addGroup(layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblInputFileName)
                                        .addComponent(txtInputFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnInputBrowse))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblOutputFileName)
                                        .addComponent(txtOutputFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnOutputBrowse))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layoutRedirectIO.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblInputDelay)
                                        .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblMs))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                                .addComponent(lblNote))
        );

        panelTerminal.setBorder(BorderFactory.createTitledBorder(
                null, "Terminal", 0, 0,
                lblInputFileName.getFont().deriveFont(lblInputFileName.getFont().getStyle() | Font.BOLD)
        ));

        GroupLayout layoutTerminal = new GroupLayout(panelTerminal);
        panelTerminal.setLayout(layoutTerminal);
        layoutTerminal.setHorizontalGroup(
                layoutTerminal.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layoutTerminal.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layoutTerminal.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layoutTerminal.createSequentialGroup()
                                                .addComponent(lblFont)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cmbFont))
                                        .addComponent(chkHalfDuplex)
                                        .addComponent(chkAlwaysOnTop))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layoutTerminal.setVerticalGroup(
                layoutTerminal.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layoutTerminal.createSequentialGroup()
                                .addGroup(layoutTerminal.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblFont)
                                        .addComponent(cmbFont))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chkHalfDuplex)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chkAlwaysOnTop)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(panelRedirectIO, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addContainerGap())
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(panelTerminal, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addContainerGap())))
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnSave, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(panelRedirectIO, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelTerminal, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnSave)
                                .addContainerGap())
        );

        pack();
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        if (txtInputFileName.getText().trim().equals(txtOutputFileName.getText().trim())) {
            dialogs.showError("Input and output file names cannot point to the same file");
            txtInputFileName.grabFocus();
            return;
        }
        if (window != null) {
            window.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
            getSelectedFont().ifPresent(f -> window.setDisplayFont(DisplayFont.fromTerminalFont(f)));
        }
        try {
            updateSettings();
            dispose();
        } catch (IOException e) {
            dialogs.showError("Input or output file names (or both) do not exist. Please make sure they do.", "ADM-3A Terminal");
        }
    }

    private Optional<TerminalSettings.TerminalFont> getSelectedFont() {
        return Optional.ofNullable(cmbFont.getSelectedItem())
                .map(p -> (Integer) p)
                .map(TerminalSettings.TerminalFont::valueOf);
    }
}
