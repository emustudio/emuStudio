/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.vt100.TerminalSettings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Objects;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.border.*;


public class SettingsDialog extends JDialog {
    private final TerminalSettings settings;
    private final Dialogs dialogs;

    public SettingsDialog(JFrame parent, TerminalSettings settings, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        initComponents();

        readSettings();
    }

    private void readSettings() {
        txtColumns.setText(String.valueOf(settings.getColumns()));
        txtRows.setText(String.valueOf(settings.getRows()));
        txtInputFile.setText(settings.getInputPath().toString());
        txtOutputFile.setText(settings.getOutputPath().toString());
        spnInputDelay.setValue(settings.getInputReadDelayMillis());
    }

    private void initComponents() {
        JPanel panelRedirectIO = new JPanel();
        JLabel lblInputFile = new JLabel("Input file:");
        JLabel lblOutputFile = new JLabel("Output file:");
        JLabel lblRedirectIoNote = new JLabel("In No GUI mode, input/output will be redirected to files.");
        JLabel lblInputDelay = new JLabel("Input delay:");
        JLabel lblMs = new JLabel("ms");
        JPanel panelSize = new JPanel();
        JLabel lblColumns = new JLabel("Columns:");
        JLabel lblRows = new JLabel("Rows:");
        JLabel lblSizeNote = new JLabel("Terminal size changes will clear current content.");

        setTitle("VT100 Terminal Settings");
        setModal(true);
        Container contentPane = getContentPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setResizable(false);

        btnSave.addActionListener(this::btnSaveActionPerformed);
        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.setDefaultCapable(true);

        btnRowsDefault.addActionListener(e -> txtRows.setText(String.valueOf(TerminalSettings.DEFAULT_ROWS)));
        btnColumnsDefault.addActionListener(e -> txtColumns.setText(String.valueOf(TerminalSettings.DEFAULT_COLUMNS)));

        panelRedirectIO.setBorder(new TitledBorder(null, "Redirect I/O", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                new Font("sansserif", Font.BOLD, 13)));

        panelSize.setBorder(new TitledBorder(null, "Terminal size", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                new Font("sansserif", Font.BOLD, 13)));

        GroupLayout panelSizeLayout = new GroupLayout(panelSize);
        panelSize.setLayout(panelSizeLayout);
        panelSizeLayout.setHorizontalGroup(
                panelSizeLayout.createParallelGroup()
                        .addGroup(panelSizeLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelSizeLayout.createParallelGroup()
                                        .addGroup(panelSizeLayout.createSequentialGroup()
                                                .addGroup(panelSizeLayout.createParallelGroup()
                                                        .addComponent(lblColumns)
                                                        .addComponent(lblRows))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSizeLayout.createParallelGroup()
                                                        .addGroup(panelSizeLayout.createSequentialGroup()
                                                                .addComponent(txtRows, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnRowsDefault))
                                                        .addGroup(panelSizeLayout.createSequentialGroup()
                                                                .addComponent(txtColumns, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnColumnsDefault))))
                                        .addComponent(lblSizeNote))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelSizeLayout.setVerticalGroup(
                panelSizeLayout.createParallelGroup()
                        .addGroup(panelSizeLayout.createSequentialGroup()
                                .addComponent(lblSizeNote)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSizeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblColumns)
                                        .addComponent(txtColumns, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnColumnsDefault))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSizeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblRows)
                                        .addComponent(txtRows, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnRowsDefault))
                                .addContainerGap(19, Short.MAX_VALUE))
        );

        GroupLayout panelIOLayout = new GroupLayout(panelRedirectIO);
        panelRedirectIO.setLayout(panelIOLayout);
        panelIOLayout.setHorizontalGroup(
                panelIOLayout.createParallelGroup()
                        .addGroup(panelIOLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelIOLayout.createParallelGroup()
                                        .addComponent(lblRedirectIoNote)
                                        .addGroup(panelIOLayout.createSequentialGroup()
                                                .addGroup(panelIOLayout.createParallelGroup()
                                                        .addComponent(lblInputFile)
                                                        .addComponent(lblOutputFile)
                                                        .addComponent(lblInputDelay))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelIOLayout.createParallelGroup()
                                                        .addGroup(panelIOLayout.createSequentialGroup()
                                                                .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblMs))
                                                        .addGroup(panelIOLayout.createSequentialGroup()
                                                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(txtInputFile, GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                                                                        .addComponent(txtOutputFile))
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelIOLayout.createParallelGroup()
                                                                        .addComponent(btnBrowseInputFile)
                                                                        .addComponent(btnBrowseOutputFile))))))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelIOLayout.setVerticalGroup(
                panelIOLayout.createParallelGroup()
                        .addGroup(GroupLayout.Alignment.TRAILING, panelIOLayout.createSequentialGroup()
                                .addComponent(lblRedirectIoNote, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblInputFile)
                                        .addComponent(txtInputFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnBrowseInputFile))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblOutputFile)
                                        .addComponent(txtOutputFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnBrowseOutputFile))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblInputDelay)
                                        .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblMs))
                                .addContainerGap())
        );

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(panelSize, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(panelRedirectIO, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(btnSave, GroupLayout.PREFERRED_SIZE, 82, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addComponent(panelSize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelRedirectIO, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSave)
                                .addContainerGap())
        );
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void btnSaveActionPerformed(ActionEvent evt) {
        if (txtInputFile.getText().trim().equals(txtOutputFile.getText().trim())) {
            dialogs.showError("Input and output file names cannot point to the same file");
            txtInputFile.grabFocus();
            return;
        }

        int rows;
        try {
            rows = RadixUtils.getInstance().parseRadix(txtRows.getText());
        } catch (NumberFormatException e) {
            dialogs.showError("Could not parse rows (expected integer number)", "VT100-terminal settings");
            txtRows.grabFocus();
            return;
        }

        int columns;
        try {
            columns = RadixUtils.getInstance().parseRadix(txtColumns.getText());
        } catch (NumberFormatException e) {
            dialogs.showError("Could not parse columns (expected integer number)", "VT100-terminal settings");
            txtColumns.grabFocus();
            return;
        }

        settings.setInputPath(Path.of(txtInputFile.getText()));
        settings.setOutputPath(Path.of(txtOutputFile.getText()));
        settings.setInputReadDelayMillis((Integer) spnInputDelay.getValue());
        settings.setSize(columns, rows);
        settings.write();
        dispose();
    }

    private final JButton btnSave = new JButton("Save");
    private final JTextField txtInputFile = new JTextField();
    private final JTextField txtOutputFile = new JTextField();
    private final JTextField txtColumns = new JTextField();
    private final JTextField txtRows = new JTextField();
    private final JButton btnBrowseInputFile = new JButton("Browse...");
    private final JButton btnBrowseOutputFile = new JButton("Browse...");
    private final JButton btnRowsDefault = new JButton("Set default");
    private final JButton btnColumnsDefault = new JButton("Set default");
    private final JSpinner spnInputDelay = new JSpinner();
}
