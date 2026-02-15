/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.adm3a.TerminalSettings;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.device.adm3a.TerminalSettings.DEFAULT_INPUT_FILE_NAME;
import static net.emustudio.plugins.device.adm3a.TerminalSettings.DEFAULT_OUTPUT_FILE_NAME;

public class SettingsDialog extends DialogBase {
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
        super(parent, "LSI ADM-3A Settings", true);

        this.dialogs = Objects.requireNonNull(dialogs);
        this.settings = Objects.requireNonNull(settings);
        this.window = window;

        readSettings();
        buildContent();
    }

    private void readSettings() {
        chkHalfDuplex.setSelected(settings.isHalfDuplex());
        chkAlwaysOnTop.setSelected(settings.isAlwaysOnTop());
        txtInputFileName.setText(settings.getInputPath().toString());
        txtOutputFileName.setText(settings.getOutputPath().toString());
        spnInputDelay.setValue(settings.getInputReadDelayMillis());
    }

    private void updateSettings() throws IOException {
        settings.setHalfDuplex(chkHalfDuplex.isSelected());
        settings.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        settings.setInputPath(Path.of(txtInputFileName.getText()));
        settings.setOutputPath(Path.of(txtOutputFileName.getText()));
        settings.setInputReadDelayMillis((Integer) spnInputDelay.getValue());
        getSelectedFont().ifPresent(settings::setFont);

        settings.write();
    }

    @Override
    protected JComponent initializeComponents() {
        cmbFont.setRenderer(new DisplayFontJComboRenderer());
        cmbFont.setSelectedIndex(settings.getFont().ordinal());
        spnInputDelay.setModel(new SpinnerNumberModel(0, 0, null, 100));

        JButton btnInputBrowse = GUI.browseFiles(dialogs, "Select input file", "Select", false, p -> txtInputFileName.setText(p.toString()));
        JButton btnOutputBrowse = GUI.browseFiles(dialogs, "Select output file", "Select", false, p -> txtOutputFileName.setText(p.toString()));

        // Redirect I/O section
        JPanel panelRedirectIO = GUI.section("Redirect I/O", "insets dialog", "[][grow][]", "[][][][]");
        panelRedirectIO.add(GUI.label("Input file name:"));
        panelRedirectIO.add(txtInputFileName, "growx");
        panelRedirectIO.add(btnInputBrowse, "wrap");
        panelRedirectIO.add(GUI.label("Output file name:"));
        panelRedirectIO.add(txtOutputFileName, "growx");
        panelRedirectIO.add(btnOutputBrowse, "wrap");
        panelRedirectIO.add(GUI.label("Input delay:"));
        panelRedirectIO.add(spnInputDelay, "split 2, w 73!");
        panelRedirectIO.add(GUI.label("ms"), "wrap");
        panelRedirectIO.add(GUI.label("Note: I/O redirection will be used only in case of No GUI mode."), "span, wrap");

        // Terminal section
        JPanel panelTerminal = GUI.section("Terminal", "insets dialog", "[][grow]", "[][][]");
        panelTerminal.add(GUI.label("Font"));
        panelTerminal.add(cmbFont, "growx, wrap");
        panelTerminal.add(chkHalfDuplex, "span, wrap");
        panelTerminal.add(chkAlwaysOnTop, "span, wrap");

        // Save button
        JButton btnSave = new JButton("Save");
        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.addActionListener(this::btnSaveActionPerformed);

        JPanel content = GUI.panel("insets dialog", "[grow]", "[][][][]");
        content.add(panelRedirectIO, "growx, wrap");
        content.add(panelTerminal, "growx, wrap");
        content.add(btnSave, "align right");

        return content;
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
