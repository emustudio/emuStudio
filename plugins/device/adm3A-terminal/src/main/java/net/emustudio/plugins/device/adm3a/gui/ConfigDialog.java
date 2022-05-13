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
import net.emustudio.plugins.device.adm3a.interaction.Display;
import net.emustudio.plugins.device.adm3a.TerminalSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigDialog extends JDialog {
    private final TerminalSettings settings;
    private final Display display;
    private final TerminalWindow window;
    private final Dialogs dialogs;

    public ConfigDialog(JFrame parent, TerminalSettings settings, TerminalWindow window, Display lblTerminal, Dialogs dialogs) {
        super(parent, true);

        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();

        this.settings = settings;
        this.display = lblTerminal;
        this.window = window;

        readSettings();
        setLocationRelativeTo(parent);
    }

    private void readSettings() {
        chkHalfDuplex.setSelected(settings.isHalfDuplex());
        chkAlwaysOnTop.setSelected(settings.isAlwaysOnTop());
        chkAntiAliasing.setSelected(settings.isAntiAliasing());
        txtInputFileName.setText(settings.getInputPath().toString());
        txtOutputFileName.setText(settings.getOutputPath().toString());
        spnInputDelay.setValue(settings.getInputReadDelay());
    }

    private void updateSettings(boolean save) throws IOException {
        settings.setHalfDuplex(chkHalfDuplex.isSelected());
        settings.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        settings.setAntiAliasing(chkAntiAliasing.isSelected());
        settings.setInputPath(Path.of(txtInputFileName.getText()));
        settings.setOutputPath(Path.of(txtOutputFileName.getText()));
        settings.setInputReadDelay((Integer) spnInputDelay.getValue());
        if (save) {
            settings.write();
        }
    }

    private void initComponents() {
        JPanel panelSimulation = new JPanel();
        JLabel jLabel1 = new JLabel();
        txtInputFileName = new JTextField();
        JButton btnInputBrowse = new JButton();
        JLabel jLabel2 = new JLabel();
        txtOutputFileName = new JTextField();
        JButton btnOutputBrowse = new JButton();
        JLabel jLabel3 = new JLabel();
        JLabel jLabel4 = new JLabel();
        spnInputDelay = new JSpinner();
        JLabel jLabel5 = new JLabel();
        JPanel jPanel1 = new JPanel();
        chkHalfDuplex = new JCheckBox();
        chkAlwaysOnTop = new JCheckBox();
        chkAntiAliasing = new JCheckBox();
        JPanel jPanel2 = new JPanel();
        chkSaveSettings = new JCheckBox();
        JButton btnClearScreen = new JButton();
        JButton btnRollLine = new JButton();
        JButton btnOK = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Configuration of the terminal");

        panelSimulation.setBorder(BorderFactory.createTitledBorder(
            null, "Redirect I/O", 0, 0,
            jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | Font.BOLD)
        ));

        jLabel1.setText("Input file name:");
        txtInputFileName.setText("terminal-ADM3A.in");
        btnInputBrowse.setText("Browse...");

        jLabel2.setText("Output file name:");
        txtOutputFileName.setText("terminal-ADM3A.out");
        btnOutputBrowse.setText("Browse...");

        jLabel3.setText("Note: I/O redirection will be used only if No GUI mode is enabled.");
        jLabel4.setText("Input delay:");

        spnInputDelay.setModel(new SpinnerNumberModel(0, 0, null, 100));

        jLabel5.setText("ms");

        GroupLayout panelSimulationLayout = new GroupLayout(panelSimulation);
        panelSimulation.setLayout(panelSimulationLayout);
        panelSimulationLayout.setHorizontalGroup(
            panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelSimulationLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel3)
                        .addGroup(panelSimulationLayout.createSequentialGroup()
                            .addGroup(panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addComponent(jLabel4))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(panelSimulationLayout.createSequentialGroup()
                                    .addComponent(txtOutputFileName, GroupLayout.PREFERRED_SIZE, 241, Short.MAX_VALUE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnOutputBrowse))
                                .addGroup(GroupLayout.Alignment.TRAILING, panelSimulationLayout.createSequentialGroup()
                                    .addComponent(txtInputFileName, GroupLayout.PREFERRED_SIZE, 241, Short.MAX_VALUE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnInputBrowse))
                                .addGroup(panelSimulationLayout.createSequentialGroup()
                                    .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel5)))))
                    .addContainerGap())
        );
        panelSimulationLayout.setVerticalGroup(
            panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, panelSimulationLayout.createSequentialGroup()
                    .addGroup(panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(txtInputFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnInputBrowse))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(txtOutputFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnOutputBrowse))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelSimulationLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                    .addComponent(jLabel3))
        );

        jPanel1.setBorder(BorderFactory.createTitledBorder(
            null, "Terminal", 0, 0,
            jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | Font.BOLD)
        ));

        chkHalfDuplex.setText("Half duplex mode");
        chkAlwaysOnTop.setText("Display always on top");
        chkAntiAliasing.setText("Use anti-aliasing");

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkHalfDuplex)
                        .addComponent(chkAlwaysOnTop)
                        .addComponent(chkAntiAliasing))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(chkHalfDuplex)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(chkAlwaysOnTop)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(chkAntiAliasing)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chkSaveSettings.setSelected(true);
        chkSaveSettings.setText("Save settings");

        btnClearScreen.setText("Clear screen");
        btnClearScreen.addActionListener(this::btnClearScreenActionPerformed);

        btnRollLine.setText("Roll down");
        btnRollLine.addActionListener(this::btnRollLineActionPerformed);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(chkSaveSettings))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnClearScreen, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnRollLine, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(btnClearScreen)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnRollLine)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(chkSaveSettings)
                    .addContainerGap())
        );

        btnOK.setText("OK");
        btnOK.addActionListener(this::btnOKActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(panelSimulation, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addContainerGap())
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addContainerGap())))
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnOK, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(18, 18, 18)
                    .addComponent(panelSimulation, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnOK)
                    .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnClearScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearScreenActionPerformed
        display.clearScreen();
    }//GEN-LAST:event_btnClearScreenActionPerformed

    private void btnRollLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRollLineActionPerformed
        display.rollLine();
    }//GEN-LAST:event_btnRollLineActionPerformed

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOKActionPerformed
        window.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        try {
            updateSettings(chkSaveSettings.isSelected());
            dispose();
        } catch (IOException e) {
            dialogs.showError("Input or output file names (or both) do not exist. Please make sure they do.", "ADM 3A");
        }
    }//GEN-LAST:event_btnOKActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JCheckBox chkAlwaysOnTop;
    private JCheckBox chkAntiAliasing;
    private JCheckBox chkHalfDuplex;
    private JCheckBox chkSaveSettings;
    private JSpinner spnInputDelay;
    private JTextField txtInputFileName;
    private JTextField txtOutputFileName;
    // End of variables declaration//GEN-END:variables
}
