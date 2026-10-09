/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

final class SettingsDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);
    private static final ImageIcon ICON_ON = GUI.loadIcon("/net/emustudio/plugins/device/altairhdsk/gui/on.png", SettingsDialog.class);
    private static final ImageIcon ICON_OFF = GUI.loadIcon("/net/emustudio/plugins/device/altairhdsk/gui/off.png", SettingsDialog.class);
    private final PluginSettings settings;
    private final Dialogs dialogs;
    private final GUI gui;
    private final Runnable onSave;
    private final boolean runningMits;
    private final DriveSettingsUI[] drives = new DriveSettingsUI[HdskController.DRIVE_COUNT];
    private final JToggleButton[] driveButtons = new JToggleButton[HdskController.DRIVE_COUNT];
    private final JComboBox<String> controllerType = new JComboBox<>(new String[]{"SIMH", "MITS"});
    private final JTextField image = new JTextField(32);
    private final JTextField sectorSize = new JTextField(8);
    private final JTextField sectorsPerTrack = new JTextField(8);
    private final JCheckBox readOnly = new JCheckBox("Read-only");
    private final JLabel selectedDrive = new JLabel();
    private final JLabel geometry = new JLabel();
    private final JLabel connection = new JLabel();
    private int currentDrive;
    private boolean displayedMits;

    SettingsDialog(JFrame parent, PluginSettings settings, boolean runningMits,
                   HdskController controller, MhdskController mitsController,
                   Dialogs dialogs, GUI gui, Runnable onSave) {
        super(parent, "88-HDSK Settings", true);
        this.settings = settings;
        this.dialogs = dialogs;
        this.gui = gui;
        this.onSave = onSave;
        this.runningMits = runningMits;
        controllerType.setSelectedItem(settings.getString("controllerType", "SIMH"));
        displayedMits = isMits();
        for (int i = 0; i < drives.length; i++) {
            DriveSettingsUI drive = new DriveSettingsUI();
            drive.image = settings.getString("image" + i).orElse("");
            drive.sectorSize = String.valueOf(settings.getInt("sectorSize" + i, HardDisk.DEFAULT_SECTOR_SIZE));
            drive.sectorsPerTrack = String.valueOf(settings.getInt("sectorsPerTrack" + i, HardDisk.DEFAULT_SECTORS_PER_TRACK));
            drive.readOnly = settings.getBoolean("readOnly" + i, false);
            if (displayedMits == runningMits) {
                if (controller != null) {
                    drive.image = controller.imagePath(i).map(Path::toString).orElse("");
                    drive.sectorSize = String.valueOf(controller.sectorSize(i));
                    drive.sectorsPerTrack = String.valueOf(controller.sectorsPerTrack(i));
                } else if (mitsController != null && i < MhdskController.PLATTER_COUNT) {
                    Path path = mitsController.getImage(i);
                    drive.image = path == null ? "" : path.toString();
                    if (path != null) { drive.readOnly = mitsController.isReadOnly(i); }
                }
            }
            drives[i] = drive;
            driveButtons[i] = new JToggleButton();
        }
        buildContent();
    }

    private boolean isMits() {
        return "MITS".equals(controllerType.getSelectedItem());
    }

    @Override
    protected JComponent initializeComponents() {
        ButtonGroup group = new ButtonGroup();
        JPanel selection = gui.panel("insets dialog", "[][grow]", "[][][]");
        selection.add(gui.label("Drive:"));
        JPanel buttons = gui.panel("insets 0, wrap 8", "[][][][][][][][]", "[][]");
        for (int i = 0; i < driveButtons.length; i++) {
            int index = i;
            JToggleButton button = driveButtons[i];
            group.add(button);
            button.setFont(FONT_MONOSPACED.deriveFont(14.0f));
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                storeDrive();
                currentDrive = index;
                loadDrive();
            });
            buttons.add(button, "hidemode 3");
        }
        selection.add(buttons, "growx, wrap");
        selection.add(selectedDrive, "skip 1, wrap");
        image.getDocument().addDocumentListener(new DocumentListener() {
            private void updateIcon() {
                driveButtons[currentDrive].setIcon(image.getText().isBlank() ? ICON_OFF : ICON_ON);
            }
            @Override public void insertUpdate(DocumentEvent e) { updateIcon(); }
            @Override public void removeUpdate(DocumentEvent e) { updateIcon(); }
            @Override public void changedUpdate(DocumentEvent e) { updateIcon(); }
        });

        JButton browse = gui.button("Browse...");
        browse.addActionListener(e -> browse());
        JButton create = gui.button("Create image");
        create.setToolTipText("Create a new image; Save mounts it on the selected drive.");
        create.addActionListener(e -> createImage());
        JButton unmount = gui.button("Unmount");
        unmount.setToolTipText("Remove this image when saving settings.");
        unmount.addActionListener(e -> image.setText(""));
        JButton unmountAll = gui.button("Unmount all");
        unmountAll.setToolTipText("Remove all images for this controller when saving settings.");
        unmountAll.addActionListener(e -> {
            for (int i = 0; i < driveCount(); i++) {
                drives[i].image = "";
                driveButtons[i].setIcon(ICON_OFF);
            }
            image.setText("");
        });
        JPanel media = gui.panel("insets dialog", "[][grow]", "[][][]");
        media.add(gui.label("Image:"));
        media.add(image, "growx, wrap");
        media.add(browse, "skip 1, split 4");
        media.add(create);
        media.add(unmount);
        media.add(unmountAll, "push, align right, wrap");
        media.add(readOnly, "skip 1");

        JButton defaults = gui.button("Set default");
        defaults.addActionListener(e -> {
            sectorSize.setText(String.valueOf(HardDisk.DEFAULT_SECTOR_SIZE));
            sectorsPerTrack.setText(String.valueOf(HardDisk.DEFAULT_SECTORS_PER_TRACK));
        });
        JPanel parameters = gui.section("Parameters", "insets dialog", "[][grow][][]", "[][][]");
        parameters.add(gui.label("Sectors per track:"));
        parameters.add(sectorsPerTrack, "growx, wrap");
        parameters.add(gui.label("Sector size:"));
        parameters.add(sectorSize, "growx");
        parameters.add(gui.label("bytes"));
        parameters.add(defaults, "wrap");
        parameters.add(geometry, "span");

        JPanel drive = gui.panel("insets dialog", "[grow]", "[][][]");
        drive.add(selection, "growx, wrap");
        drive.add(media, "growx, wrap");
        drive.add(parameters, "growx");

        JPanel interfacePanel = gui.panel("insets dialog", "[][grow]", "[][][]");
        interfacePanel.add(gui.label("Controller:"));
        interfacePanel.add(controllerType, "wrap");
        interfacePanel.add(connection, "span, wrap");
        interfacePanel.add(gui.label("<html>Controller changes take effect after reopening the computer.<br>"
                + "Update schema connections to match the selected controller.</html>"), "span");
        controllerType.addActionListener(e -> {
            storeDrive();
            displayedMits = isMits();
            if (currentDrive >= driveCount()) { currentDrive = 0; }
            updateMode();
            defaults.setEnabled(!displayedMits);
            pack();
        });
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Drive settings", drive);
        tabs.addTab("Controller", interfacePanel);

        JButton save = gui.button("Save");
        save.addActionListener(e -> saveSettings());
        rootPane.setDefaultButton(save);
        gui.buttonMakePrimary(save);
        JPanel content = gui.panel("insets 0, fill", "[grow]", "[grow][]");
        content.add(tabs, "grow, wrap");
        content.add(save, "align right, gapright 6, gapbottom 6");
        updateMode();
        defaults.setEnabled(!displayedMits);
        return content;
    }

    private int driveCount() {
        return displayedMits ? MhdskController.PLATTER_COUNT : HdskController.DRIVE_COUNT;
    }

    private void updateMode() {
        for (int i = 0; i < driveButtons.length; i++) {
            driveButtons[i].setIcon(drives[i].image.isBlank() ? ICON_OFF : ICON_ON);
            driveButtons[i].setVisible(i < driveCount());
            driveButtons[i].setText(displayedMits ? (i / 2) + ((i & 1) == 0 ? "R" : "F")
                    : String.valueOf((char) ('A' + i)));
        }
        sectorSize.setEnabled(!displayedMits);
        sectorsPerTrack.setEnabled(!displayedMits);
        readOnly.setEnabled(displayedMits);
        geometry.setText(displayedMits ? "406 cylinders, 2 surfaces per platter; fixed geometry."
                : "Sector size: 128, 256, 512 or 1024; sectors per track: 1–255.");
        connection.setText(displayedMits
                ? "<html>MITS 88-HDSK / MHDSK: connect to 88-PIO in 88-4PIO mode.<br>"
                    + "At least two PIAs; bundled configuration uses A0h–A7h.</html>"
                : "<html>SIMH HDSK: connect to CPU port FDh and byte memory for DMA.</html>");
        loadDrive();
    }

    private void storeDrive() {
        DriveSettingsUI drive = drives[currentDrive];
        drive.image = image.getText().trim();
        if (!displayedMits) {
            drive.sectorSize = sectorSize.getText().trim();
            drive.sectorsPerTrack = sectorsPerTrack.getText().trim();
        }
        drive.readOnly = readOnly.isSelected();
    }

    private void loadDrive() {
        DriveSettingsUI drive = drives[currentDrive];
        driveButtons[currentDrive].setSelected(true);
        selectedDrive.setText(displayedMits ? "Unit " + (currentDrive / 2)
                + ((currentDrive & 1) == 0 ? " removable platter" : " fixed platter")
                : "Drive " + (char) ('A' + currentDrive) + " (" + currentDrive + ")");
        image.setText(drive.image);
        sectorSize.setText(displayedMits ? String.valueOf(Platter.SECTOR_SIZE) : drive.sectorSize);
        sectorsPerTrack.setText(displayedMits ? String.valueOf(Platter.SECTORS) : drive.sectorsPerTrack);
        readOnly.setSelected(drive.readOnly);
    }

    private void browse() {
        try {
            Path directory = image.getText().isBlank() ? Path.of(System.getProperty("user.dir"))
                    : Path.of(image.getText().trim()).toAbsolutePath().getParent();
            dialogs.chooseFile("Open disk image", "Open", directory, false,
                    new FileExtensionsFilter("Disk images", "dsk", "bin", "img"))
                    .ifPresent(path -> image.setText(path.toString()));
        } catch (IllegalArgumentException e) {
            dialogs.showError("Invalid image path.", "Open disk image");
            image.requestFocusInWindow();
        }
    }

    private void createImage() {
        try {
            Path directory = image.getText().isBlank() ? Path.of(System.getProperty("user.dir"))
                    : Path.of(image.getText().trim()).toAbsolutePath().getParent();
            dialogs.chooseFile("Create disk image", "Create", directory, true,
                    new FileExtensionsFilter("Disk images", "dsk", "bin", "img"))
                    .ifPresent(path -> {
                        try {
                            if (isMits()) { MhdskController.createImage(path); }
                            else { Files.createFile(path); }
                            image.setText(path.toString());
                        } catch (IOException | IllegalArgumentException e) {
                            dialogs.showError("Could not create image: " + e.getMessage(), "Create disk image");
                        }
                    });
        } catch (IllegalArgumentException e) {
            dialogs.showError("Invalid image path.", "Create disk image");
            image.requestFocusInWindow();
        }
    }

    private void saveSettings() {
        storeDrive();
        int[] sizes = new int[driveCount()];
        int[] sectors = new int[driveCount()];
        for (int i = 0; i < driveCount(); i++) {
            try {
                DriveSettingsUI drive = drives[i];
                if (!displayedMits) {
                    sizes[i] = RadixUtils.getInstance().parseRadix(drive.sectorSize);
                    sectors[i] = RadixUtils.getInstance().parseRadix(drive.sectorsPerTrack);
                    new HardDisk().configure(sizes[i], sectors[i]);
                }
                if (!drive.image.isBlank()) {
                    Path path = Path.of(drive.image);
                    if (displayedMits) {
                        if (!Files.isRegularFile(path) || Files.size(path) != Platter.CAPACITY) {
                            throw new IOException("MHDSK platter must contain exactly " + Platter.CAPACITY + " bytes");
                        }
                    } else if (!Files.isRegularFile(path)) {
                        throw new IOException("Choose an existing disk image or use Create image.");
                    }
                    if (!Files.isReadable(path) || ((!displayedMits || !drive.readOnly) && !Files.isWritable(path))) {
                        throw new IOException("Disk image is not accessible with the selected write protection");
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                currentDrive = i;
                loadDrive();
                dialogs.showError(selectedDrive.getText() + ": " + e.getMessage(), "Save settings");
                return;
            }
        }
        try {
            for (int i = 0; i < driveCount(); i++) {
                DriveSettingsUI drive = drives[i];
                if (drive.image.isBlank()) { settings.remove("image" + i); }
                else { settings.setString("image" + i, drive.image); }
                if (displayedMits) { settings.setBoolean("readOnly" + i, drive.readOnly); }
                else {
                    settings.setInt("sectorSize" + i, sizes[i]);
                    settings.setInt("sectorsPerTrack" + i, sectors[i]);
                }
            }
            settings.setString("controllerType", displayedMits ? "MITS" : "SIMH");
            onSave.run();
            if (displayedMits != runningMits) {
                dialogs.showInfo("Reopen the computer to use the selected controller. "
                        + "Update its schema connections as shown on the Controller tab.", "88-HDSK Settings");
            }
            dispose();
        } catch (RuntimeException e) {
            LOGGER.error("Could not save HDSK settings", e);
            dialogs.showError("Could not save or apply settings: " + e.getMessage(), "Save settings");
        }
    }

    private static final class DriveSettingsUI {
        String image;
        String sectorSize;
        String sectorsPerTrack;
        boolean readOnly;
    }
}
