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
                driveButtons[driveIndex].turnOn();
            } else {
                driveButtons[driveIndex].turnOff();
            }
        }
    }

    private void initComponents() {
        ButtonGroup buttonGroup1 = new ButtonGroup();
        JPanel panelDiskSelection = new JPanel();
        JPanel panelFlags = new JPanel();
        JLabel jLabel1 = new JLabel("Port 1:");
        JLabel jLabel2 = new JLabel("Port 2:");
        JPanel jPanel3 = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        JPanel jPanel4 = new JPanel();
        JLabel jLabel3 = new JLabel("Track:");
        JLabel jLabel4 = new JLabel("Sector:");
        JLabel jLabel5 = new JLabel("Offset:");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle(DIALOG_TITLE);
        setResizable(false);

        panelDiskSelection.setBorder(BorderFactory.createTitledBorder(null, "Disk selection",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | Font.BOLD)
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
            jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | Font.BOLD)
        ));

        GroupLayout jPanel2Layout = new GroupLayout(panelFlags);
        panelFlags.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblPort2Status))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblPort1Status)))
                    .addContainerGap(119, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(lblPort1Status))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(lblPort2Status))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(BorderFactory.createTitledBorder(
            null, "Mounted image", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | Font.BOLD)
        ));

        txtMountedImage.setEditable(false);
        txtMountedImage.setBackground(UIManager.getDefaults().getColor("TextField.disabledBackground"));
        txtMountedImage.setColumns(20);
        txtMountedImage.setFont(MONOSPACED_PLAIN);
        txtMountedImage.setLineWrap(true);
        txtMountedImage.setRows(5);
        jScrollPane1.setViewportView(txtMountedImage);

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1)
                    .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1)
                    .addContainerGap())
        );

        jPanel4.setBorder(BorderFactory.createTitledBorder(
            null, "Position", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | Font.BOLD)
        ));

        GroupLayout jPanel4Layout = new GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel4)
                        .addComponent(jLabel3)
                        .addComponent(jLabel5))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblOffset)
                        .addComponent(lblTrack)
                        .addComponent(lblSector))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(lblTrack))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(lblSector))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel5)
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
                        .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(panelFlags, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(panelDiskSelection, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panelFlags, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
