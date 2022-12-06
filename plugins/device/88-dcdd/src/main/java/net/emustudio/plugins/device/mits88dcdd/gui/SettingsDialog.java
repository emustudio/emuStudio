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

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.device.mits88dcdd.DiskSettings;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.emustudio.plugins.device.mits88dcdd.DiskSettings.*;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

public class SettingsDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);
    private final static Font DRIVE_BUTTON_FONT = new Font("Monospaced", Font.PLAIN, 14);
    private final static ImageIcon OFF_ICON = new ImageIcon(SettingsDialog.class.getResource("/net/emustudio/plugins/device/mits88dcdd/gui/off.png"));
    private final static ImageIcon ON_ICON = new ImageIcon(SettingsDialog.class.getResource("/net/emustudio/plugins/device/mits88dcdd/gui/on.png"));

    private final Dialogs dialogs;
    private final DiskSettings settings;
    private final DriveCollection drives;

    private final List<DriveSettingsUI> driveSettingsUI = new ArrayList<>();
    private final List<JToggleButton> driveButtons = new ArrayList<>();
    private int currentDriveIndex = 0;

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

    public SettingsDialog(JFrame parent, DiskSettings settings, DriveCollection drives, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.drives = Objects.requireNonNull(drives);
        this.dialogs = Objects.requireNonNull(dialogs);

        readSettings();

        initComponents();
        setLocationRelativeTo(parent);

        driveButtons.add(btnA);
        driveButtons.add(btnB);
        driveButtons.add(btnC);
        driveButtons.add(btnD);
        driveButtons.add(btnE);
        driveButtons.add(btnF);
        driveButtons.add(btnG);
        driveButtons.add(btnH);
        driveButtons.add(btnI);
        driveButtons.add(btnJ);
        driveButtons.add(btnK);
        driveButtons.add(btnL);
        driveButtons.add(btnM);
        driveButtons.add(btnN);
        driveButtons.add(btnO);
        driveButtons.add(btnP);

        btnA.setSelected(true);
        updateGUI(0);
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
            String nullablePath = dsui.image;
            if (nullablePath != null && nullablePath.equals("")) {
                nullablePath = null;
            }
            DiskSettings.DriveSettings driveSettings = new DiskSettings.DriveSettings(
                parsedSectorSizes.get(i), parsedSectorsPerTracks.get(i), nullablePath, dsui.mounted);
            settings.setDriveSettings(i, driveSettings);
            return null;
        });
    }

    private void updateGUI(int index) {
        this.currentDriveIndex = index;
        DriveSettingsUI dsui = driveSettingsUI.get(index);
        txtSectorSize.setText(dsui.sectorSize);
        txtSectorsPerTrack.setText(dsui.sectorsPerTrack);

        Optional
            .ofNullable(dsui.image)
            .filter(p -> !p.equals(""))
            .map(Path::of)
            .ifPresentOrElse(path -> {
                txtImageFile.setText(path.toAbsolutePath().toString());
                btnMountUnmount.setSelected(dsui.mounted);
                if (dsui.mounted) {
                    btnMountUnmount.setText("Unmount");
                } else {
                    btnMountUnmount.setText("Mount");
                }
                driveButtons.get(index).setIcon(ON_ICON);
            }, () -> {
                txtImageFile.setText("");
                btnMountUnmount.setSelected(false);
                btnMountUnmount.setText("Mount");
                driveButtons.get(index).setIcon(OFF_ICON);
            });
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

    private void initComponents() {
        ButtonGroup buttonGroup1 = new ButtonGroup();
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel panelDrive = new JPanel();
        JPanel panelImageParameters = new JPanel();
        JLabel lblDrive = new JLabel("Drive:");
        JLabel lblImage = new JLabel("Image:");
        JLabel lblSpt = new JLabel("Sectors per track:");
        JLabel lblSectorSize = new JLabel("Sector size:");
        JLabel lblBytes = new JLabel("bytes");
        JPanel panelCpu = new JPanel();
        JLabel lblPort1 = new JLabel("Port 1:");
        JLabel lblPort2 = new JLabel("Port 2:");
        JLabel lblPort3 = new JLabel("Port 3:");
        JLabel lblInterruptVector = new JLabel("Interrupt vector:");
        JLabel lblPort1In = new JLabel("(IN: Get flags");
        JLabel lblPort2In = new JLabel("(IN: Current sector");
        JLabel lblPort3In = new JLabel("(IN: Read data");
        JLabel lblNote = new JLabel("Set CPU ports and interrupt vector used by this device.");
        JLabel lblPort1Out = new JLabel("OUT: Select/unselect drive)");
        JLabel lblPort2Out = new JLabel("OUT: Set flags)");
        JLabel lblPort3Out = new JLabel("OUT: Write data)");
        JLabel lblRange = new JLabel("(range 0-7)");

        setTitle("88-DCDD Settings");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        rootPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setupDriveButton(buttonGroup1, btnA, 0);
        setupDriveButton(buttonGroup1, btnB, 1);
        setupDriveButton(buttonGroup1, btnC, 2);
        setupDriveButton(buttonGroup1, btnD, 3);
        setupDriveButton(buttonGroup1, btnE, 4);
        setupDriveButton(buttonGroup1, btnF, 5);
        setupDriveButton(buttonGroup1, btnG, 6);
        setupDriveButton(buttonGroup1, btnH, 7);
        setupDriveButton(buttonGroup1, btnI, 8);
        setupDriveButton(buttonGroup1, btnJ, 9);
        setupDriveButton(buttonGroup1, btnK, 10);
        setupDriveButton(buttonGroup1, btnL, 11);
        setupDriveButton(buttonGroup1, btnM, 12);
        setupDriveButton(buttonGroup1, btnN, 13);
        setupDriveButton(buttonGroup1, btnO, 14);
        setupDriveButton(buttonGroup1, btnP, 15);

        tabbedPane.addTab("Drive settings", panelDrive);
        tabbedPane.addTab("Connection with CPU", panelCpu);

        panelImageParameters.setBorder(BorderFactory.createTitledBorder("Parameters"));
        GroupLayout panelImageParametersLayout = new GroupLayout(panelImageParameters);
        panelImageParameters.setLayout(panelImageParametersLayout);
        panelImageParametersLayout.setHorizontalGroup(
            panelImageParametersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImageParametersLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelImageParametersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblSpt)
                        .addComponent(lblSectorSize))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelImageParametersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelImageParametersLayout.createSequentialGroup()
                            .addComponent(txtSectorsPerTrack, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(panelImageParametersLayout.createSequentialGroup()
                            .addComponent(txtSectorSize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(lblBytes)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnDriveDefault)))
                    .addContainerGap())
        );
        panelImageParametersLayout.setVerticalGroup(
            panelImageParametersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImageParametersLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelImageParametersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblSpt)
                        .addComponent(txtSectorsPerTrack, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelImageParametersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblSectorSize)
                        .addComponent(txtSectorSize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblBytes)
                        .addComponent(btnDriveDefault))
                    .addContainerGap(9, Short.MAX_VALUE))
        );

        GroupLayout panelDriveLayout = new GroupLayout(panelDrive);
        panelDrive.setLayout(panelDriveLayout);
        panelDriveLayout.setHorizontalGroup(
            panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelDriveLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, panelDriveLayout.createSequentialGroup()
                            .addComponent(lblImage)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(panelDriveLayout.createSequentialGroup()
                                    .addComponent(btnBrowse)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnMountUnmount)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnUnmountAll))
                                .addComponent(txtImageFile)))
                        .addGroup(panelDriveLayout.createSequentialGroup()
                            .addComponent(lblDrive)
                            .addGap(22, 22, 22)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnI, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnA, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnJ, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnB, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnK, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnC, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnL, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnD, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnM, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnF, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnN, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnO, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnG, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnP, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnH, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(panelImageParameters, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        panelDriveLayout.setVerticalGroup(
            panelDriveLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelDriveLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblDrive)
                        .addComponent(btnA)
                        .addComponent(btnB)
                        .addComponent(btnC)
                        .addComponent(btnD)
                        .addComponent(btnE)
                        .addComponent(btnF)
                        .addComponent(btnG)
                        .addComponent(btnH))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnI)
                        .addComponent(btnJ)
                        .addComponent(btnK)
                        .addComponent(btnL)
                        .addComponent(btnM)
                        .addComponent(btnN)
                        .addComponent(btnO)
                        .addComponent(btnP))
                    .addGap(18, 18, 18)
                    .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblImage)
                        .addComponent(txtImageFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelDriveLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnMountUnmount)
                        .addComponent(btnBrowse)
                        .addComponent(btnUnmountAll))
                    .addGap(18, 18, 18)
                    .addComponent(panelImageParameters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout panelCpuLayout = new GroupLayout(panelCpu);
        panelCpu.setLayout(panelCpuLayout);
        panelCpuLayout.setHorizontalGroup(
            panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelCpuLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(btnCpuDefault, GroupLayout.Alignment.TRAILING)
                        .addGroup(panelCpuLayout.createSequentialGroup()
                            .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblNote)
                                .addGroup(panelCpuLayout.createSequentialGroup()
                                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(lblPort3)
                                        .addComponent(lblPort2))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(panelCpuLayout.createSequentialGroup()
                                            .addComponent(txtPort2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(lblPort2In))
                                        .addGroup(panelCpuLayout.createSequentialGroup()
                                            .addComponent(txtPort3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(lblPort3In)))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblPort3Out)
                                        .addComponent(lblPort1Out)
                                        .addComponent(lblPort2Out)))
                                .addGroup(panelCpuLayout.createSequentialGroup()
                                    .addComponent(lblPort1)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtPort1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(lblPort1In))
                                .addGroup(panelCpuLayout.createSequentialGroup()
                                    .addComponent(lblInterruptVector)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(spnInterruptVector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(lblRange))
                                .addComponent(chkInterruptsSupported))
                            .addGap(0, 164, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        panelCpuLayout.setVerticalGroup(
            panelCpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, panelCpuLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblNote)
                    .addGap(18, 18, 18)
                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPort1)
                        .addComponent(txtPort1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblPort1In)
                        .addComponent(lblPort1Out))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPort2)
                        .addComponent(txtPort2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblPort2In)
                        .addComponent(lblPort2Out))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPort3)
                        .addComponent(txtPort3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblPort3In)
                        .addComponent(lblPort3Out))
                    .addGap(32, 32, 32)
                    .addComponent(chkInterruptsSupported)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelCpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lblInterruptVector)
                        .addComponent(spnInterruptVector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblRange))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                    .addComponent(btnCpuDefault)
                    .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(tabbedPane)
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnSave)
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnSave)
                    .addContainerGap())
        );

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
                .of(driveSettingsUI.get(currentDriveIndex).image)
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

        pack();
    }

    private void setupDriveButton(ButtonGroup group, JToggleButton button, int index) {
        group.add(button);
        button.setFont(DRIVE_BUTTON_FONT);
        button.setIcon(OFF_ICON);
        button.addActionListener(e -> updateGUI(index));
    }

    private void setupTextField(JTextField textField, BiConsumer<DriveSettingsUI, String> property) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                property.accept(driveSettingsUI.get(currentDriveIndex), textField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                property.accept(driveSettingsUI.get(currentDriveIndex), textField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                property.accept(driveSettingsUI.get(currentDriveIndex), textField.getText());
            }
        });
    }


    private final JToggleButton btnA = new JToggleButton("A");
    private final JToggleButton btnB = new JToggleButton("B");
    private final JToggleButton btnC = new JToggleButton("C");
    private final JToggleButton btnD = new JToggleButton("D");
    private final JToggleButton btnE = new JToggleButton("E");
    private final JToggleButton btnF = new JToggleButton("F");
    private final JToggleButton btnG = new JToggleButton("G");
    private final JToggleButton btnH = new JToggleButton("H");
    private final JToggleButton btnI = new JToggleButton("I");
    private final JToggleButton btnJ = new JToggleButton("J");
    private final JToggleButton btnK = new JToggleButton("K");
    private final JToggleButton btnL = new JToggleButton("L");
    private final JToggleButton btnM = new JToggleButton("M");
    private final JToggleButton btnN = new JToggleButton("N");
    private final JToggleButton btnO = new JToggleButton("O");
    private final JToggleButton btnP = new JToggleButton("P");
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

}
