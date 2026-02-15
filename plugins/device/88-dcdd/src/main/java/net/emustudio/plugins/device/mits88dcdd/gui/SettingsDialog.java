/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.gui;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.device.mits88dcdd.DiskSettings;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.emustudio.plugins.device.mits88dcdd.DiskSettings.*;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.*;

public class SettingsDialog extends DialogBase {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final Dialogs dialogs;
    private final DiskSettings settings;
    private final DriveCollection drives;

    private final List<DriveSettingsUI> driveSettingsUI = new ArrayList<>();
    private final List<JToggleButton> driveButtons = new ArrayList<>();
    private final JToggleButton btnMountUnmount = new JToggleButton("Mount");
    private final JButton btnUnmountAll = new JButton("Unmount all");
    private final JButton btnSave = new JButton("Save");
    private final JButton btnBrowse = new JButton("Browse...");
    private final JCheckBox chkInterruptsSupported = new JCheckBox("Interrupts supported");
    private final JSpinner spnInterruptVector = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
    private final JButton btnCpuDefault = new JButton("Set default");
    private final JButton btnDriveDefault = new JButton("Set default");
    private final JTextField txtImageFile = new JTextField();
    private final JTextField txtPort1 = new JTextField(String.format("0x%02X", DEFAULT_CPU_PORT1));
    private final JTextField txtPort2 = new JTextField(String.format("0x%02X", DEFAULT_CPU_PORT2));
    private final JTextField txtPort3 = new JTextField(String.format("0x%02X", DEFAULT_CPU_PORT3));
    private final JTextField txtSectorSize = new JTextField(String.valueOf(DiskSettings.DEFAULT_SECTOR_SIZE));
    private final JTextField txtSectorsPerTrack = new JTextField(String.valueOf(DiskSettings.DEFAULT_SECTORS_PER_TRACK));
    private int currentDriveIndex = 0;

    public SettingsDialog(JFrame parent, DiskSettings settings, DriveCollection drives, Dialogs dialogs) {
        super(parent, "88-DCDD Settings", true);

        this.settings = Objects.requireNonNull(settings);
        this.drives = Objects.requireNonNull(drives);
        this.dialogs = Objects.requireNonNull(dialogs);

        readSettings();

        for (char c = 'A'; c <= 'P'; c++) {
            driveButtons.add(new JToggleButton(String.valueOf(c)));
        }
        driveButtons.get(0).setSelected(true);
        updateGUI(0);
        buildContent();
    }

    private void readSettings() {
        txtPort1.setText(String.format("0x%02X", settings.getPort1CPU()));
        txtPort2.setText(String.format("0x%02X", settings.getPort2CPU()));
        txtPort3.setText(String.format("0x%02X", settings.getPort3CPU()));
        spnInterruptVector.setValue(settings.getInterruptVector());
        chkInterruptsSupported.setSelected(settings.getInterruptsSupported());

        driveSettingsUI.clear();
        drives.foreach((i, drive) -> {
            DiskSettings.DriveSettings driveSettings = settings.getDriveSettings(i);
            DriveSettingsUI dsui = DriveSettingsUI.fromDriveSettings(driveSettings);
            dsui.mounted = drive.isMounted();
            driveSettingsUI.add(dsui);
            return null;
        });
    }

    private void saveSettings() throws PluginInitializationException, RuntimeException {
        int parsedPort1 = parseInt(txtPort1, "Port1", txtPort1::getText);
        int parsedPort2 = parseInt(txtPort2, "Port2", txtPort2::getText);
        int parsedPort3 = parseInt(txtPort3, "Port3", txtPort3::getText);
        int interruptVector = ((Number) spnInterruptVector.getValue()).intValue();

        List<Integer> parsedSectorsPerTracks = new ArrayList<>();
        List<Integer> parsedSectorSizes = new ArrayList<>();

        drives.foreach((i, drive) -> {
            DriveSettingsUI dsui = driveSettingsUI.get(i);
            JToggleButton driveButton = driveButtons.get(i);
            parsedSectorsPerTracks.add(parseInt(driveButton, txtSectorsPerTrack, "Sectors per track", () -> dsui.sectorsPerTrack));
            parsedSectorSizes.add(parseInt(driveButton, txtSectorSize, "Sector size", () -> dsui.sectorSize));
            return null;
        });

        settings.setPort1CPU(parsedPort1);
        settings.setPort2CPU(parsedPort2);
        settings.setPort3CPU(parsedPort3);
        settings.setInterruptVector(interruptVector);
        settings.setInterruptsSupported(chkInterruptsSupported.isSelected());

        drives.foreach((i, drive) -> {
            DriveSettingsUI dsui = driveSettingsUI.get(i);
            DiskSettings.DriveSettings driveSettings = new DiskSettings.DriveSettings(
                    parsedSectorSizes.get(i), parsedSectorsPerTracks.get(i),
                    hasImage(dsui.image) ? dsui.image : null, dsui.mounted);
            settings.setDriveSettings(i, driveSettings);
            return null;
        });
    }

    private static boolean hasImage(String image) {
        return image != null && !image.isEmpty();
    }

    private void updateGUI(int index) {
        this.currentDriveIndex = index;
        DriveSettingsUI dsui = driveSettingsUI.get(index);
        txtSectorSize.setText(dsui.sectorSize);
        txtSectorsPerTrack.setText(dsui.sectorsPerTrack);

        if (hasImage(dsui.image)) {
            txtImageFile.setText(Path.of(dsui.image).toAbsolutePath().toString());
            btnMountUnmount.setSelected(dsui.mounted);
            btnMountUnmount.setText(dsui.mounted ? "Unmount" : "Mount");
            driveButtons.get(index).setIcon(ICON_ON);
        } else {
            txtImageFile.setText("");
            btnMountUnmount.setSelected(false);
            btnMountUnmount.setText("Mount");
            driveButtons.get(index).setIcon(ICON_OFF);
        }
    }

    private int parseInt(JComponent component, String name, Supplier<String> text) {
        RadixUtils radixUtils = RadixUtils.getInstance();
        try {
            return radixUtils.parseRadix(text.get());
        } catch (NumberFormatException e) {
            dialogs.showError(name + ": Invalid number format", "Save settings");
            component.grabFocus();
            throw e;
        } catch (IllegalArgumentException e) {
            dialogs.showError(name + " must be greater than 0", "Save settings");
            component.grabFocus();
            throw e;
        }
    }

    private int parseInt(JToggleButton driveButton, JComponent component, String name, Supplier<String> text) {
        try {
            return parseInt(component, name, text);
        } catch (RuntimeException e) {
            driveButton.setSelected(true);
            throw e;
        }
    }

    @Override
    protected JComponent initializeComponents() {
        ButtonGroup buttonGroup = new ButtonGroup();
        for (int i = 0; i < driveButtons.size(); i++) {
            setupDriveButton(buttonGroup, driveButtons.get(i), i);
        }

        // === Drive settings tab ===

        // Drive selection: 2 rows of 8 buttons
        JPanel panelDriveSelection = GUI.panel("insets dialog", "[][][][][][][][]", "[][]");
        panelDriveSelection.add(GUI.label("Drive:"), "span 1 2");
        for (int i = 0; i < 8; i++) {
            panelDriveSelection.add(driveButtons.get(i), i == 7 ? "wrap" : "");
        }
        panelDriveSelection.add(new JPanel(), "skip 1"); // skip "Drive:" label column
        for (int i = 8; i < 16; i++) {
            panelDriveSelection.add(driveButtons.get(i));
        }

        // Image file
        JPanel panelImage = GUI.panel("insets dialog", "[][grow]", "[][]");
        panelImage.add(GUI.label("Image:"));
        panelImage.add(txtImageFile, "growx, wrap");
        panelImage.add(btnBrowse, "skip 1, split 3");
        panelImage.add(btnMountUnmount);
        panelImage.add(btnUnmountAll, "push, align right");

        // Parameters section
        JPanel panelParameters = GUI.section("Parameters", "insets dialog", "[][grow][][]", "[][]");
        panelParameters.add(GUI.label("Sectors per track:"));
        panelParameters.add(txtSectorsPerTrack, "growx, wrap");
        panelParameters.add(GUI.label("Sector size:"));
        panelParameters.add(txtSectorSize, "growx");
        panelParameters.add(GUI.label("bytes"));
        panelParameters.add(btnDriveDefault);

        JPanel panelDrive = GUI.panel("insets dialog", "[grow]", "[][][grow]");
        panelDrive.add(panelDriveSelection, "growx, wrap");
        panelDrive.add(panelImage, "growx, wrap");
        panelDrive.add(panelParameters, "growx");

        // === CPU tab ===
        JPanel panelCpu = GUI.panel("insets dialog", "[][fill][][]", "[][][][]20[][][]");
        panelCpu.add(GUI.label("Set CPU ports and interrupt vector used by this device."), "span, wrap");

        panelCpu.add(GUI.label("Port 1:"));
        panelCpu.add(txtPort1);
        panelCpu.add(GUI.label("(IN: Get flags"));
        panelCpu.add(GUI.label("OUT: Select/unselect drive)"), "wrap");

        panelCpu.add(GUI.label("Port 2:"));
        panelCpu.add(txtPort2);
        panelCpu.add(GUI.label("(IN: Current sector"));
        panelCpu.add(GUI.label("OUT: Set flags)"), "wrap");

        panelCpu.add(GUI.label("Port 3:"));
        panelCpu.add(txtPort3);
        panelCpu.add(GUI.label("(IN: Read data"));
        panelCpu.add(GUI.label("OUT: Write data)"), "wrap");

        panelCpu.add(chkInterruptsSupported, "span, wrap");

        panelCpu.add(GUI.label("Interrupt vector:"));
        panelCpu.add(spnInterruptVector);
        panelCpu.add(GUI.label("(range 0-7)"), "wrap");

        panelCpu.add(btnCpuDefault, "span, align right");

        // === Tabbed pane ===
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Drive settings", panelDrive);
        tabbedPane.addTab("Connection with CPU", panelCpu);

        // === Listeners ===
        btnMountUnmount.addItemListener(e -> {
            DriveSettingsUI dsui = driveSettingsUI.get(currentDriveIndex);
            if (e.getStateChange() == ItemEvent.SELECTED) {
                try {
                    drives.get(currentDriveIndex).mount(Path.of(txtImageFile.getText()));
                    dsui.image = txtImageFile.getText();
                    dsui.mounted = true;
                } catch (IOException ex) {
                    LOGGER.error("88-DCDD: Could not mount image: " + txtImageFile.getText(), ex);
                    dialogs.showError("Could not mount image: " + ex.getMessage(), "Mount image");
                    txtImageFile.grabFocus();
                }
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                dsui.image = null;
                dsui.mounted = false;
                drives.get(currentDriveIndex).umount();
            }
            updateGUI(currentDriveIndex);
        });

        btnUnmountAll.addActionListener(e -> drives.foreach((i, drive) -> {
            DriveSettingsUI dsui = driveSettingsUI.get(i);
            dsui.image = null;
            dsui.mounted = false;
            drive.umount();
            updateGUI(i);
            return null;
        }));

        btnBrowse.addActionListener(e -> {
            Path currentDirectory = Optional
                    .ofNullable(driveSettingsUI.get(currentDriveIndex).image)
                    .filter(p -> !p.isEmpty())
                    .map(Path::of)
                    .orElse(Path.of(System.getProperty("user.dir")));

            dialogs.chooseFile(
                    "Open disk image", "Open", currentDirectory, false,
                    new FileExtensionsFilter("Disk images", "dsk", "bin")
            ).ifPresent(path -> txtImageFile.setText(path.toString()));
        });

        rootPane.setDefaultButton(btnSave);
        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.addActionListener(e -> {
            try {
                saveSettings();
                dispose();
            } catch (PluginInitializationException ex) {
                LOGGER.error(DIALOG_TITLE + ": Could not re-attach CPU ports", ex);
                dialogs.showError("Could not re-attach CPU ports. Please see log for more details.", DIALOG_TITLE);
            } catch (RuntimeException ignored) {
                // already handled
            }
        });

        btnDriveDefault.addActionListener(e -> {
            txtSectorsPerTrack.setText(String.valueOf(DiskSettings.DEFAULT_SECTORS_PER_TRACK));
            txtSectorSize.setText(String.valueOf(DiskSettings.DEFAULT_SECTOR_SIZE));
        });

        btnCpuDefault.addActionListener(e -> {
            txtPort1.setText(String.format("0x%02X", DEFAULT_CPU_PORT1));
            txtPort2.setText(String.format("0x%02X", DEFAULT_CPU_PORT2));
            txtPort3.setText(String.format("0x%02X", DEFAULT_CPU_PORT3));
        });

        setupTextField(txtImageFile, (dsui, value) -> dsui.image = value);
        setupTextField(txtSectorsPerTrack, (dsui, value) -> dsui.sectorsPerTrack = value);
        setupTextField(txtSectorSize, (dsui, value) -> dsui.sectorSize = value);

        // Main content: tabbed pane + save button
        JPanel content = GUI.panel("insets 0, fill", "[grow]", "[grow][]");
        content.add(tabbedPane, "grow, wrap");
        content.add(btnSave, "align right, gapright 6, gapbottom 6");
        return content;
    }

    private void setupDriveButton(ButtonGroup group, JToggleButton button, int index) {
        group.add(button);
        button.setFont(DRIVE_BUTTON_FONT);
        button.setIcon(hasImage(driveSettingsUI.get(index).image) ? ICON_ON : ICON_OFF);
        button.setFocusPainted(false);
        button.addActionListener(e -> updateGUI(index));
    }

    private void setupTextField(JTextField textField, BiConsumer<DriveSettingsUI, String> property) {
        Consumer<DocumentEvent> handler = e -> property.accept(driveSettingsUI.get(currentDriveIndex), textField.getText());
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { handler.accept(e); }
            @Override public void removeUpdate(DocumentEvent e) { handler.accept(e); }
            @Override public void changedUpdate(DocumentEvent e) { handler.accept(e); }
        });
    }

    static class DriveSettingsUI {
        String sectorsPerTrack;
        String sectorSize;
        String image;
        boolean mounted;

        static DriveSettingsUI fromDriveSettings(DiskSettings.DriveSettings driveSettings) {
            DriveSettingsUI dsui = new DriveSettingsUI();
            dsui.sectorsPerTrack = String.valueOf(driveSettings.sectorsPerTrack);
            dsui.sectorSize = String.valueOf(driveSettings.sectorSize);
            dsui.image = Optional.ofNullable(driveSettings.imagePath).orElse("");
            dsui.mounted = driveSettings.mounted;
            return dsui;
        }
    }
}
