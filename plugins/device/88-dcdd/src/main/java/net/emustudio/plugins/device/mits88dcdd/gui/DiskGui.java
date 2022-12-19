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
package net.emustudio.plugins.device.mits88dcdd.gui;

import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveListener;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveParameters;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.MONOSPACED_PLAIN;

public class DiskGui extends JDialog {
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
    private final DriveCollection drives;

    private final JLabel lblOffset = createMonospacedLabel("0");
    private final JLabel lblSector = createMonospacedLabel("0");
    private final JLabel lblTrack = createMonospacedLabel("0");
    private final JLabel lblPort1Status = createMonospacedLabel(DriveParameters.port1StatusString(Drive.DEAD_DRIVE));
    private final JLabel lblPort2Status = createMonospacedLabel(DriveParameters.port2StatusString(Drive.SECTOR0));
    private final JTextArea txtMountedImage = new JTextArea();

    public DiskGui(JFrame parent, DriveCollection drives) {
        super(parent);
        this.drives = Objects.requireNonNull(drives);

        initComponents();
        setLocationRelativeTo(parent);

        drives.foreach((i, drive) -> {
            drive.addDriveListener(new GUIDriveListener(i));
            return null;
        });
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

    private void initComponents() {
        ButtonGroup buttonGroup1 = new ButtonGroup();
        JPanel panelDiskSelection = new JPanel();
        JPanel panelFlags = new JPanel();
        JLabel lblPort1Label = new JLabel("Port 1:");
        JLabel lblPort2Label = new JLabel("Port 2:");
        JPanel panelImage = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        JPanel panelPosition = new JPanel();
        JLabel lblTrackLabel = new JLabel("Track:");
        JLabel lblSectorLabel = new JLabel("Sector:");
        JLabel lblOffsetLabel = new JLabel("Offset:");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        rootPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle(DIALOG_TITLE);
        setResizable(false);

        panelDiskSelection.setBorder(BorderFactory.createTitledBorder(null, "Disk selection",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            lblPort1Label.getFont().deriveFont(lblPort1Label.getFont().getStyle() | Font.BOLD)
        ));

        for (DriveButton button : driveButtons) {
            buttonGroup1.add(button);
        }

        GroupLayout diskSelectionLayout = new GroupLayout(panelDiskSelection);
        panelDiskSelection.setLayout(diskSelectionLayout);

        GroupLayout.SequentialGroup upperSequentialGroup = diskSelectionLayout.createSequentialGroup();
        GroupLayout.ParallelGroup upperParallelGroup = diskSelectionLayout.createParallelGroup(GroupLayout.Alignment.BASELINE);
        for (int i = 0; i < 8; i++) {
            upperSequentialGroup
                .addGroup(diskSelectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(driveButtons[i])
                    .addComponent(driveButtons[i + 8])
                ).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
            upperParallelGroup.addComponent(driveButtons[i]);
        }

        GroupLayout.SequentialGroup lowerSequentialGroup = diskSelectionLayout.createSequentialGroup();
        GroupLayout.ParallelGroup lowerParallelGroup = diskSelectionLayout.createParallelGroup(GroupLayout.Alignment.BASELINE);
        for (int i = 8; i < 16; i++) {
            lowerSequentialGroup.addComponent(driveButtons[i]).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
            lowerParallelGroup.addComponent(driveButtons[i]);
        }

        diskSelectionLayout.setHorizontalGroup(
            diskSelectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(diskSelectionLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(upperSequentialGroup)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        diskSelectionLayout.setVerticalGroup(
            diskSelectionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(upperParallelGroup)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lowerParallelGroup)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        panelFlags.setBorder(BorderFactory.createTitledBorder(
            null, "Flags and settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            lblPort1Label.getFont().deriveFont(lblPort1Label.getFont().getStyle() | Font.BOLD)
        ));

        GroupLayout panelFlagsLayout = new GroupLayout(panelFlags);
        panelFlags.setLayout(panelFlagsLayout);
        panelFlagsLayout.setHorizontalGroup(
            panelFlagsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelFlagsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelFlagsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelFlagsLayout.createSequentialGroup()
                            .addComponent(lblPort2Label)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblPort2Status))
                        .addGroup(panelFlagsLayout.createSequentialGroup()
                            .addComponent(lblPort1Label)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblPort1Status)))
                    .addContainerGap(119, Short.MAX_VALUE))
        );
        panelFlagsLayout.setVerticalGroup(
            panelFlagsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelFlagsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelFlagsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPort1Label)
                        .addComponent(lblPort1Status))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelFlagsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPort2Label)
                        .addComponent(lblPort2Status))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelImage.setBorder(BorderFactory.createTitledBorder(
            null, "Mounted image", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            lblPort1Label.getFont().deriveFont(lblPort1Label.getFont().getStyle() | Font.BOLD)
        ));

        txtMountedImage.setEditable(false);
        txtMountedImage.setBackground(UIManager.getDefaults().getColor("TextField.disabledBackground"));
        txtMountedImage.setColumns(20);
        txtMountedImage.setFont(MONOSPACED_PLAIN);
        txtMountedImage.setLineWrap(true);
        txtMountedImage.setRows(5);
        jScrollPane1.setViewportView(txtMountedImage);

        GroupLayout panelImageLayout = new GroupLayout(panelImage);
        panelImage.setLayout(panelImageLayout);
        panelImageLayout.setHorizontalGroup(
            panelImageLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImageLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1)
                    .addContainerGap())
        );
        panelImageLayout.setVerticalGroup(
            panelImageLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImageLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1)
                    .addContainerGap())
        );

        panelPosition.setBorder(BorderFactory.createTitledBorder(
            null, "Position", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            lblPort1Label.getFont().deriveFont(lblPort1Label.getFont().getStyle() | Font.BOLD)
        ));

        GroupLayout panelPositionLayout = new GroupLayout(panelPosition);
        panelPosition.setLayout(panelPositionLayout);
        panelPositionLayout.setHorizontalGroup(
            panelPositionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelPositionLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelPositionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblSectorLabel)
                        .addComponent(lblTrackLabel)
                        .addComponent(lblOffsetLabel))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelPositionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblOffset)
                        .addComponent(this.lblTrack)
                        .addComponent(this.lblSector))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelPositionLayout.setVerticalGroup(
            panelPositionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelPositionLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelPositionLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblTrackLabel)
                        .addComponent(this.lblTrack))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelPositionLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblSectorLabel)
                        .addComponent(this.lblSector))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelPositionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblOffsetLabel)
                        .addComponent(lblOffset))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(panelDiskSelection, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panelImage, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(panelFlags, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(panelPosition, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(panelDiskSelection, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(panelPosition, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panelFlags, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(panelImage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private static JLabel createMonospacedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MONOSPACED_PLAIN);
        return label;
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
