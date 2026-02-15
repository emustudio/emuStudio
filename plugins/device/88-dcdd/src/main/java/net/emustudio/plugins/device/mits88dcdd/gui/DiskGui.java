/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.gui;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveListener;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveParameters;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

public class DiskGui extends DialogBase {
    private final DriveCollection drives;
    private final JLabel lblOffset = createMonospacedLabel("0");
    private final JLabel lblSector = createMonospacedLabel("0");
    private final JLabel lblTrack = createMonospacedLabel("0");
    private final JLabel lblPort1Status = createMonospacedLabel(DriveParameters.port1StatusString(Drive.DEAD_DRIVE));
    private final JLabel lblPort2Status = createMonospacedLabel(DriveParameters.port2StatusString(Drive.SECTOR0));
    private final JTextArea txtMountedImage = GUI.textAreaReadOnly(5, 20);
    private final DriveButton[] driveButtons = new DriveButton[]{
            new DriveButton("A", () -> updateDriveInfo(0)),
            new DriveButton("B", () -> updateDriveInfo(1)),
            new DriveButton("C", () -> updateDriveInfo(2)),
            new DriveButton("D", () -> updateDriveInfo(3)),
            new DriveButton("E", () -> updateDriveInfo(4)),
            new DriveButton("F", () -> updateDriveInfo(5)),
            new DriveButton("G", () -> updateDriveInfo(6)),
            new DriveButton("H", () -> updateDriveInfo(7)),
            new DriveButton("I", () -> updateDriveInfo(8)),
            new DriveButton("J", () -> updateDriveInfo(9)),
            new DriveButton("K", () -> updateDriveInfo(10)),
            new DriveButton("L", () -> updateDriveInfo(11)),
            new DriveButton("M", () -> updateDriveInfo(12)),
            new DriveButton("N", () -> updateDriveInfo(13)),
            new DriveButton("O", () -> updateDriveInfo(14)),
            new DriveButton("P", () -> updateDriveInfo(15)),
    };

    public DiskGui(JFrame parent, DriveCollection drives) {
        super(parent, DIALOG_TITLE, false);
        this.drives = Objects.requireNonNull(drives);

        setResizable(false);

        drives.foreach((i, drive) -> {
            drive.addDriveListener(new GUIDriveListener(i));
            return null;
        });

        buildContent();
    }

    private static JLabel createMonospacedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_MONOSPACED);
        return label;
    }

    private void updateDriveInfo(int index) {
        updateDriveInfo(drives.get(index).getDriveParameters());
    }

    private void updateDriveInfo(DriveParameters parameters) {
        lblPort1Status.setText(parameters.port1statusString);
        lblPort2Status.setText(parameters.port2statusString);

        lblSector.setText(String.valueOf(parameters.sector));
        lblTrack.setText(String.valueOf(parameters.track));
        lblOffset.setText(String.valueOf(parameters.sectorOffset));

        if (parameters.mountedFloppy != null) {
            txtMountedImage.setText(parameters.mountedFloppy.toAbsolutePath().toString());
        } else {
            txtMountedImage.setText("none");
        }
    }

    public void select(int driveIndex, boolean selected) {
        if (driveIndex >= 0 && driveIndex < driveButtons.length) {
            if (selected) {
                driveButtons[driveIndex].setSelected();
            } else {
                driveButtons[driveIndex].setUnselected();
            }
        }
    }

    @Override
    protected JComponent initializeComponents() {
        ButtonGroup buttonGroup = new ButtonGroup();
        for (DriveButton button : driveButtons) {
            buttonGroup.add(button);
        }

        // Disk selection - 2 rows of 8 drive buttons
        JPanel panelDiskSelection = GUI.section("Disk selection", "insets dialog", "[][][][][][][][]", "[][]");
        for (int i = 0; i < 8; i++) {
            panelDiskSelection.add(driveButtons[i], i == 7 ? "wrap" : "");
        }
        for (int i = 8; i < 16; i++) {
            panelDiskSelection.add(driveButtons[i]);
        }

        // Flags and settings
        JPanel panelFlags = GUI.section("Flags and settings", "insets dialog", "[][grow]", "[][]");
        panelFlags.add(GUI.label("Port 1:"));
        panelFlags.add(lblPort1Status, "wrap");
        panelFlags.add(GUI.label("Port 2:"));
        panelFlags.add(lblPort2Status);

        // Position
        JPanel panelPosition = GUI.section("Position", "insets dialog", "[][grow]", "[][][]");
        panelPosition.add(GUI.label("Track:"));
        panelPosition.add(lblTrack, "wrap");
        panelPosition.add(GUI.label("Sector:"));
        panelPosition.add(lblSector, "wrap");
        panelPosition.add(GUI.label("Offset:"));
        panelPosition.add(lblOffset);

        // Mounted image
        txtMountedImage.setFont(FONT_MONOSPACED);
        txtMountedImage.setBackground(UIManager.getDefaults().getColor("TextField.disabledBackground"));

        JPanel panelImage = GUI.section("Mounted image", "insets dialog, fill", "[grow]", "[grow]");
        panelImage.add(GUI.scrollable(txtMountedImage), "grow");

        // Main layout
        JPanel content = GUI.panel("insets dialog, fill", "[grow]", "[][][grow]");
        content.add(panelDiskSelection, "growx, wrap");
        content.add(panelFlags, "split 2, grow");
        content.add(panelPosition, "grow, wrap");
        content.add(panelImage, "grow");

        return content;
    }

    private class GUIDriveListener implements DriveListener {
        private final int index;

        private GUIDriveListener(int index) {
            this.index = index;
        }

        @Override
        public void driveSelect(final boolean selected) {
            SwingUtilities.invokeLater(() -> select(index, selected));
        }

        @Override
        public void driveParamsChanged(final DriveParameters parameters) {
            SwingUtilities.invokeLater(() -> updateDriveInfo(parameters));
        }
    }
}
