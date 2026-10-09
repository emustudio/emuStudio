/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.nio.file.Path;
import java.text.ParseException;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

final class HdskGui extends DialogBase {
    private static final ImageIcon ICON_ON = GUI.loadIcon("/net/emustudio/plugins/device/altairhdsk/gui/selected.png", HdskGui.class);
    private static final ImageIcon ICON_OFF = GUI.loadIcon("/net/emustudio/plugins/device/altairhdsk/gui/unselected.png", HdskGui.class);
    private final HdskController controller;
    private final MhdskController mitsController;
    private final Dialogs dialogs;
    private final GUI gui;
    private final boolean mits;
    private final JToggleButton[] driveButtons;
    private final JComboBox<Integer> sectorSize = new JComboBox<>(new Integer[]{128, 256, 512, 1024});
    private final JSpinner sectorsPerTrack = new JSpinner(new SpinnerNumberModel(32, 1, 255, 1));
    private final JLabel driveName = monospacedLabel();
    private final JLabel mounted = monospacedLabel();
    private final JLabel writeProtection = monospacedLabel();
    private final JLabel tracks = monospacedLabel();
    private final JTextArea activity;
    private final JTextArea image;
    private final Timer refreshTimer = new Timer(100, event -> refresh());
    private int selectedDrive;
    private int displayedSectorSize;
    private int displayedSectorsPerTrack;

    HdskGui(JFrame parent, HdskController controller, MhdskController mitsController, Dialogs dialogs, GUI gui) {
        super(parent, mitsController == null ? "SIMH Altair HDSK" : "MITS 88-HDSK (MHDSK)", false);
        if ((controller == null) == (mitsController == null)) {
            throw new IllegalArgumentException("Select exactly one HDSK controller");
        }
        this.controller = controller;
        this.mitsController = mitsController;
        this.dialogs = dialogs;
        this.gui = gui;
        mits = mitsController != null;
        driveButtons = new JToggleButton[mits ? MhdskController.PLATTER_COUNT : HdskController.DRIVE_COUNT];
        activity = new JTextArea(3, 24);
        activity.setEditable(false);
        activity.setLineWrap(true);
        activity.setWrapStyleWord(true);
        image = new JTextArea(3, 40);
        image.setEditable(false);
        image.setFont(FONT_MONOSPACED);
        image.setBackground(UIManager.getColor("TextField.disabledBackground"));
        setResizable(false);
        buildContent();
        loadDrive();
    }

    private static JLabel monospacedLabel() {
        JLabel label = new JLabel();
        label.setFont(FONT_MONOSPACED);
        return label;
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel selection = gui.section("Disk selection", "insets dialog, wrap 8", "[][][][][][][][]", "[][]");
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < driveButtons.length; i++) {
            int index = i;
            JToggleButton button = new JToggleButton(mits ? (i / 2) + ((i & 1) == 0 ? "R" : "F")
                    : String.valueOf((char) ('A' + i)), ICON_OFF);
            button.setFont(FONT_MONOSPACED);
            button.setFocusPainted(false);
            button.addActionListener(event -> {
                selectedDrive = index;
                loadDrive();
            });
            group.add(button);
            driveButtons[i] = button;
            selection.add(button);
        }

        JPanel flags = gui.section("Flags and settings", "insets dialog, fill", "[][grow]", "[][][][grow]");
        flags.add(gui.label("Drive:"));
        flags.add(driveName, "wrap");
        flags.add(gui.label("Image:"));
        flags.add(mounted, "wrap");
        flags.add(gui.label("Write protection:"));
        flags.add(writeProtection, "wrap");
        flags.add(gui.label("Activity:"), "aligny top");
        flags.add(gui.scrollPane(activity), "grow");

        JPanel geometry = gui.section("Geometry", "insets dialog", "[][grow][]", "[][][][]");
        geometry.add(gui.label(mits ? "Cylinders:" : "Tracks:"));
        geometry.add(tracks, "span 2, wrap");
        if (mits) {
            geometry.add(gui.label("Surfaces:"));
            geometry.add(gui.label("2 per platter"), "span 2, wrap");
        }
        geometry.add(gui.label("Sectors per track:"));
        geometry.add(mits ? gui.label(String.valueOf(Platter.SECTORS)) : sectorsPerTrack, "span 2, wrap");
        geometry.add(gui.label("Sector size:"));
        geometry.add(mits ? gui.label(String.valueOf(Platter.SECTOR_SIZE)) : sectorSize);
        geometry.add(gui.label("bytes"), "wrap");
        if (!mits) {
            JButton apply = gui.button("Apply geometry");
            apply.addActionListener(event -> configure());
            geometry.add(apply, "span, align right");
        }

        JPanel media = gui.section("Mounted image", "insets dialog, fill", "[grow]", "[grow]");
        media.add(gui.scrollPane(image), "grow");

        JPanel content = gui.panel("insets dialog, fill", "[grow]", "[][grow][grow]");
        content.add(selection, "growx, wrap");
        content.add(flags, "split 2, grow");
        content.add(geometry, "grow, wrap");
        content.add(media, "grow");
        return content;
    }

    private Path imagePath(int drive) {
        return mits ? mitsController.getImage(drive) : controller.imagePath(drive).orElse(null);
    }

    private void loadDrive() {
        driveButtons[selectedDrive].setSelected(true);
        if (!mits) {
            displayedSectorSize = controller.sectorSize(selectedDrive);
            displayedSectorsPerTrack = controller.sectorsPerTrack(selectedDrive);
            sectorSize.setSelectedItem(displayedSectorSize);
            sectorsPerTrack.setValue(displayedSectorsPerTrack);
        }
        refresh();
    }

    void refresh() {
        if (!mits) {
            int size = controller.sectorSize(selectedDrive);
            int sectors = controller.sectorsPerTrack(selectedDrive);
            if (size != displayedSectorSize || sectors != displayedSectorsPerTrack) {
                displayedSectorSize = size;
                displayedSectorsPerTrack = sectors;
                sectorSize.setSelectedItem(size);
                sectorsPerTrack.setValue(sectors);
            }
        }
        for (int i = 0; i < driveButtons.length; i++) {
            boolean attached = imagePath(i) != null;
            driveButtons[i].setIcon(attached ? ICON_ON : ICON_OFF);
            driveButtons[i].setToolTipText((mits ? "Unit " + (i / 2) + ((i & 1) == 0 ? " removable" : " fixed")
                    : "Drive " + (char) ('A' + i)) + (attached ? ": image mounted" : ": no image mounted"));
        }
        Path path = imagePath(selectedDrive);
        String pathText = path == null ? "none" : path.toAbsolutePath().toString();
        if (!pathText.equals(image.getText())) {
            image.setText(pathText);
            image.setCaretPosition(0);
        }
        driveName.setText(mits ? "Unit " + (selectedDrive / 2) + ((selectedDrive & 1) == 0 ? " removable" : " fixed")
                : String.valueOf((char) ('A' + selectedDrive)) + " (" + selectedDrive + ")");
        mounted.setText(path == null ? "Unmounted" : "Mounted");
        writeProtection.setText(path == null ? "—" : mits && mitsController.isReadOnly(selectedDrive) ? "Read-only" : "Writable");
        try {
            tracks.setText(String.valueOf(mits ? Platter.CYLINDERS : controller.tracks(selectedDrive)));
            activity.setText(mits ? mitsController.getActivity() : controller.activity(selectedDrive));
        } catch (IllegalStateException e) {
            tracks.setText("Unavailable");
            activity.setText(e.getMessage());
        }
    }

    private int readSectorsPerTrack() throws ParseException {
        sectorsPerTrack.commitEdit();
        int sectors = ((Number) sectorsPerTrack.getValue()).intValue();
        if (sectors < 1 || sectors > 255) {
            throw new IllegalArgumentException("Sectors per track must be between 1 and 255");
        }
        return sectors;
    }

    private void configure() {
        try {
            controller.configure(selectedDrive, (Integer) sectorSize.getSelectedItem(), readSectorsPerTrack());
            refresh();
        } catch (ParseException | IllegalArgumentException e) {
            showError(e);
        }
    }

    private void showError(Exception error) {
        dialogs.showError(error.getMessage(), getTitle());
        refresh();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            loadDrive();
            refreshTimer.start();
        } else { refreshTimer.stop(); }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }
}
