/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.vt100.TerminalSettings;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;


public class SettingsDialog extends DialogBase {
    private final GUI gui;
    private final TerminalSettings settings;
    private final Dialogs dialogs;
    private final JTextField txtInputFile = new JTextField();
    private final JTextField txtOutputFile = new JTextField();
    private final JTextField txtColumns = new JTextField();
    private final JTextField txtRows = new JTextField();
    private final JSpinner spnInputDelay = new JSpinner();

    public SettingsDialog(JFrame parent, TerminalSettings settings, Dialogs dialogs, GUI gui) {
        super(parent, "VT100 Terminal Settings", true);
        this.gui = gui;

        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);

        setResizable(false);
        readSettings();
        buildContent();
    }

    private void readSettings() {
        txtColumns.setText(String.valueOf(settings.getColumns()));
        txtRows.setText(String.valueOf(settings.getRows()));
        txtInputFile.setText(settings.getInputPath().toString());
        txtOutputFile.setText(settings.getOutputPath().toString());
        spnInputDelay.setValue(settings.getInputReadDelayMillis());
    }

    @Override
    protected JComponent initializeComponents() {
        spnInputDelay.setModel(new SpinnerNumberModel(0, 0, null, 100));

        // Terminal size section
        JButton btnColumnsDefault = gui.button("Set default");
        JButton btnRowsDefault = gui.button("Set default");
        btnColumnsDefault.addActionListener(e -> txtColumns.setText(String.valueOf(TerminalSettings.DEFAULT_COLUMNS)));
        btnRowsDefault.addActionListener(e -> txtRows.setText(String.valueOf(TerminalSettings.DEFAULT_ROWS)));

        JPanel panelSize = gui.section("Terminal size", "insets dialog", "[][64!][]", "[][][]");
        panelSize.add(gui.label("Terminal size changes will clear current content."), "span, wrap");
        panelSize.add(gui.label("Columns:"));
        panelSize.add(txtColumns, "growx");
        panelSize.add(btnColumnsDefault, "wrap");
        panelSize.add(gui.label("Rows:"));
        panelSize.add(txtRows, "growx");
        panelSize.add(btnRowsDefault, "wrap");

        // Redirect I/O section
        JButton btnBrowseInputFile = gui.buttonBrowseFiles(dialogs, "Select input file", "Select", false, p -> txtInputFile.setText(p.toString()));
        JButton btnBrowseOutputFile = gui.buttonBrowseFiles(dialogs, "Select output file", "Select", false, p -> txtOutputFile.setText(p.toString()));

        JPanel panelRedirectIO = gui.section("Redirect I/O", "insets dialog", "[][grow][]", "[][][][]");
        panelRedirectIO.add(gui.label("In No GUI mode, input/output will be redirected to files."), "span, h 30!, wrap");
        panelRedirectIO.add(gui.label("Input file:"));
        panelRedirectIO.add(txtInputFile, "growx");
        panelRedirectIO.add(btnBrowseInputFile, "wrap");
        panelRedirectIO.add(gui.label("Output file:"));
        panelRedirectIO.add(txtOutputFile, "growx");
        panelRedirectIO.add(btnBrowseOutputFile, "wrap");
        panelRedirectIO.add(gui.label("Input delay:"));
        panelRedirectIO.add(spnInputDelay, "split 2, w 64!");
        panelRedirectIO.add(gui.label("ms"), "wrap");

        // Save button
        JButton btnSave = gui.button("Save");
        gui.buttonMakePrimary(btnSave);
        btnSave.addActionListener(this::btnSaveActionPerformed);

        JPanel content = gui.panel("insets dialog", "[grow]", "[][][]");
        content.add(panelSize, "growx, wrap");
        content.add(panelRedirectIO, "growx, wrap");
        content.add(btnSave, "align right");

        return content;
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
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
}
