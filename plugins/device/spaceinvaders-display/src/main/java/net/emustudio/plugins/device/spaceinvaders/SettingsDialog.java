/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.ParseException;

final class SettingsDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final PluginSettings settings;
    private final Dialogs dialogs;
    private final GUI gui;
    private final Runnable onSave;
    private final JSpinner spnScale = new JSpinner(new SpinnerNumberModel(
            2, 1, Integer.MAX_VALUE / DisplayPanel.HEIGHT, 1));
    private final JCheckBox chkColorOverlay = new JCheckBox("Color overlay");
    private final JCheckBox chkSoundEnabled = new JCheckBox("Sound enabled");
    private final JTextField txtSamplesDirectory = new JTextField(30);

    SettingsDialog(JFrame parent, PluginSettings settings, Dialogs dialogs, GUI gui, Runnable onSave) {
        super(parent, "Space Invaders Settings", true);
        this.settings = settings;
        this.dialogs = dialogs;
        this.gui = gui;
        this.onSave = onSave;

        spnScale.setValue(Math.max(1, settings.getInt("scale", 2)));
        chkColorOverlay.setSelected(settings.getBoolean("colorOverlay", true));
        chkColorOverlay.setToolTipText("<html>Simulates the arcade screen overlay: red at the top, green at the bottom,"
                + "<br>and white in the middle. Disable for a black-and-white display.</html>");
        boolean guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        chkSoundEnabled.setSelected(settings.getBoolean("soundEnabled", guiSupported));
        txtSamplesDirectory.setText(settings.getString("soundSamplesDirectory", "examples/space-invaders/sounds"));
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel display = gui.section("Display", "insets dialog", "[][grow]", "[][]");
        display.add(gui.label("Scale:"));
        display.add(spnScale, "split 2");
        display.add(gui.label("×"), "wrap");
        display.add(chkColorOverlay, "span");

        JButton btnBrowse = gui.button("Browse...");
        btnBrowse.addActionListener(e -> browseSamplesDirectory());
        JPanel sound = gui.section("Sound", "insets dialog", "[][grow][]", "[][][]");
        sound.add(chkSoundEnabled, "span, wrap");
        sound.add(gui.label("Samples directory:"));
        sound.add(txtSamplesDirectory, "growx");
        sound.add(btnBrowse, "wrap");
        sound.add(gui.label("Uses 0.wav through 9.wav."), "skip 1, span");

        JButton btnSave = gui.button("Save");
        btnSave.addActionListener(e -> saveSettings());
        rootPane.setDefaultButton(btnSave);
        gui.buttonMakePrimary(btnSave);

        JPanel content = gui.panel("insets dialog, fillx", "[grow]", "[][][]");
        content.add(display, "growx, wrap");
        content.add(sound, "growx, wrap");
        content.add(btnSave, "align right");
        return content;
    }

    private void browseSamplesDirectory() {
        try {
            Path directory = txtSamplesDirectory.getText().isBlank()
                    ? Path.of(System.getProperty("user.dir"))
                    : Path.of(txtSamplesDirectory.getText().trim());
            dialogs.chooseDirectory("Sound samples directory", "Select", directory)
                    .ifPresent(path -> txtSamplesDirectory.setText(path.toString()));
        } catch (InvalidPathException e) {
            dialogs.showError("Invalid sound samples directory.", "Sound samples directory");
            txtSamplesDirectory.requestFocusInWindow();
        }
    }

    private void saveSettings() {
        try {
            spnScale.commitEdit();
        } catch (ParseException e) {
            dialogs.showError("Scale must be a positive integer.", "Save settings");
            spnScale.requestFocusInWindow();
            return;
        }
        String directory = txtSamplesDirectory.getText().trim();
        try {
            if (directory.isEmpty()) {
                throw new InvalidPathException(directory, "Empty directory");
            }
            Path.of(directory);
        } catch (InvalidPathException e) {
            dialogs.showError("Invalid sound samples directory.", "Save settings");
            txtSamplesDirectory.requestFocusInWindow();
            return;
        }
        try {
            settings.setInt("scale", ((Number) spnScale.getValue()).intValue());
            settings.setBoolean("colorOverlay", chkColorOverlay.isSelected());
            settings.setBoolean("soundEnabled", chkSoundEnabled.isSelected());
            settings.setString("soundSamplesDirectory", directory);
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not save Space Invaders settings", e);
            dialogs.showError("Could not save settings. Please see log file for details.", "Save settings");
            return;
        }
        onSave.run();
        dispose();
    }
}
