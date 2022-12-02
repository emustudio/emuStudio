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
import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.device.mits88dcdd.DiskSettings.*;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_PLAIN;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

public class SettingsDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final Dialogs dialogs;
    private final DiskSettings settings;
    private final DriveCollection drives;

    private final List<String> sectorsPerTrack = new ArrayList<>();
    private final List<String> sectorSizes = new ArrayList<>();
    private final List<String> images = new ArrayList<>();

    public SettingsDialog(JFrame parent, DiskSettings settings, DriveCollection drives, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.drives = Objects.requireNonNull(drives);
        this.dialogs = Objects.requireNonNull(dialogs);

        readSettings();

        initComponents();
        setLocationRelativeTo(parent);

        cmbDrive.setSelectedIndex(0);
        updateGUI(0);
    }

    private void readSettings() {
        txtPort1.setText(String.format("0x%02X", settings.getPort1CPU()));
        txtPort2.setText(String.format("0x%02X", settings.getPort2CPU()));
        txtPort3.setText(String.format("0x%02X", settings.getPort3CPU()));

        sectorsPerTrack.clear();
        sectorSizes.clear();
        images.clear();
        drives.foreach((i, drive) -> {
            DiskSettings.DriveSettings driveSettings = settings.getDriveSettings(i);
            sectorsPerTrack.add(String.valueOf(driveSettings.sectorsPerTrack));
            sectorSizes.add(String.valueOf(driveSettings.sectorSize));
            images.add(Optional.ofNullable(driveSettings.imagePath).orElse(""));
            return null;
        });
    }

    private void saveSettings(int parsedPort1, int parsedPort2, int parsedPort3) {
        RadixUtils radixUtils = RadixUtils.getInstance();

        try {
            List<Integer> parsedSectorsPerTracks = new ArrayList<>();
            List<Integer> parsedSectorSizes = new ArrayList<>();

            drives.foreach((i, drive) -> {
                parsedSectorsPerTracks.add(radixUtils.parseRadix(sectorsPerTrack.get(i)));
                parsedSectorSizes.add(radixUtils.parseRadix(sectorSizes.get(i)));
                return null;
            });

            settings.setPort1CPU(parsedPort1);
            settings.setPort2CPU(parsedPort2);
            settings.setPort3CPU(parsedPort3);

            drives.foreach((i, drive) -> {
                String nullablePath = images.get(i);
                if (nullablePath.equals("")) {
                    nullablePath = null;
                }
                DiskSettings.DriveSettings driveSettings = new DiskSettings.DriveSettings(
                    parsedSectorSizes.get(i), parsedSectorsPerTracks.get(i), nullablePath
                );
                settings.setDriveSettings(i, driveSettings);
                drive.setDriveSettings(driveSettings);
                return null;
            });
            drives.reattach();
        } catch (RuntimeException e) {
            LOGGER.error("Could not write " + DIALOG_TITLE + " settings", e);
            dialogs.showError("Could not save settings. Please see log for more details.", DIALOG_TITLE);
        } catch (PluginInitializationException e) {
            LOGGER.error(DIALOG_TITLE + ": Could not re-attach CPU ports", e);
            dialogs.showError("Could not re-attach CPU ports. Please see log for more details.", DIALOG_TITLE);
        }
    }

    private void updateGUI(int index) {
        txtSectorSize.setText(sectorSizes.get(index));
        txtSectorsPerTrack.setText(sectorsPerTrack.get(index));

        Optional
            .of(images.get(index))
            .filter(p -> !p.equals(""))
            .map(Path::of)
            .ifPresentOrElse(path -> {
                txtImageFile.setText(path.toAbsolutePath().toString());
                btnUnmount.setEnabled(true);
            }, () -> {
                txtImageFile.setText("");
                btnUnmount.setEnabled(false);
            });
    }

    private void initComponents() {
        JTabbedPane jTabbedPane1 = new JTabbedPane();
        JPanel panelImages = new JPanel();
        JLabel jLabel1 = new JLabel("Image file name:");
        JButton btnBrowse = new JButton("Browse...");
        JPanel panelImage = new JPanel();
        JLabel jLabel2 = new JLabel("Disk drive:");
        JPanel jPanel3 = new JPanel();
        JLabel jLabel10 = new JLabel("Sectors per track:");
        JLabel jLabel11 = new JLabel("Sector size:");
        JButton btnDefaultParams = new JButton("Change to Default");
        JPanel panelPorts = new JPanel();
        JPanel jPanel2 = new JPanel();
        JLabel jLabel3 = new JLabel("Settings in this tab will be reflected after the restart of emuStudio.");
        JLabel jLabel4 = new JLabel("Port 1:");
        JLabel jLabel5 = new JLabel("Port 2:");
        JLabel jLabel6 = new JLabel("Port 3:");
        JLabel jLabel7 = new JLabel("(IN: flags, OUT: select/unselect drive)");
        JLabel jLabel8 = new JLabel("(IN: current sector, OUT: set flags)");
        JLabel jLabel9 = new JLabel("(IN: read data, OUT: write data)");
        JButton btnDefault = new JButton("Change to Default");
        JButton btnSave = new JButton("Save");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle(DIALOG_TITLE + " Settings");

        jTabbedPane1.setFont(jTabbedPane1.getFont());

        cmbDrive.setModel(new DefaultComboBoxModel<>(new String[]{"Drive 0 (A)", "Drive 1 (B)", "Drive 2 (C)", "Drive 3 (D)", "Drive 4 (E)", "Drive 5 (F)", "Drive 6 (G)", "Drive 7 (H)", "Drive 8 (I)", "Drive 9 (J)", "Drive 10 (K)", "Drive 11 (L)", "Drive 12 (M)", "Drive 13 (N)", "Drive 14 (O)", "Drive 15 (P)"}));
        cmbDrive.addItemListener(this::cmbDriveItemStateChanged);

        txtImageFile.addInputMethodListener(new InputMethodListener() {
            public void inputMethodTextChanged(InputMethodEvent evt) {
                txtImageFileInputMethodTextChanged();
            }

            public void caretPositionChanged(InputMethodEvent evt) {
            }
        });

        btnBrowse.addActionListener(this::btnBrowseActionPerformed);

        panelImage.setBorder(BorderFactory.createTitledBorder(
            null, "Image operations", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            DIALOG_PLAIN
        ));

        btnMount.addActionListener(this::btnMountActionPerformed);
        btnUnmount.addActionListener(this::btnUnmountActionPerformed);

        GroupLayout panelImageLayout = new GroupLayout(panelImage);
        panelImage.setLayout(panelImageLayout);
        panelImageLayout.setHorizontalGroup(
            panelImageLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImageLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(btnMount)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnUnmount)
                    .addContainerGap(23, Short.MAX_VALUE))
        );
        panelImageLayout.setVerticalGroup(
            panelImageLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImageLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelImageLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnMount)
                        .addComponent(btnUnmount))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(BorderFactory.createTitledBorder(
            null, "Drive parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            DIALOG_PLAIN
        ));

        btnDefaultParams.addActionListener(this::btnDefaultParamsActionPerformed);

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(btnDefaultParams)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel10)
                                .addComponent(jLabel11))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(txtSectorsPerTrack)
                                .addComponent(txtSectorSize, GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE))))
                    .addContainerGap(58, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(txtSectorsPerTrack, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel11)
                        .addComponent(txtSectorSize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(18, 18, 18)
                    .addComponent(btnDefaultParams)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout panelImagesLayout = new GroupLayout(panelImages);
        panelImages.setLayout(panelImagesLayout);
        panelImagesLayout.setHorizontalGroup(
            panelImagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImagesLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(txtImageFile)
                        .addGroup(GroupLayout.Alignment.TRAILING, panelImagesLayout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(btnBrowse))
                        .addGroup(panelImagesLayout.createSequentialGroup()
                            .addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(cmbDrive, GroupLayout.PREFERRED_SIZE, 277, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2))
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(panelImagesLayout.createSequentialGroup()
                            .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(panelImage, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        panelImagesLayout.setVerticalGroup(
            panelImagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelImagesLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel2)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(cmbDrive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(jLabel1)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(txtImageFile, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnBrowse)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelImagesLayout.createSequentialGroup()
                            .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addComponent(panelImage, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );

        jTabbedPane1.addTab("Disk Images", panelImages);

        jPanel2.setBorder(BorderFactory.createTitledBorder("Note"));

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel3)
                    .addContainerGap(90, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel3)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btnDefault.addActionListener(this::btnDefaultActionPerformed);

        GroupLayout panelPortsLayout = new GroupLayout(panelPorts);
        panelPorts.setLayout(panelPortsLayout);
        panelPortsLayout.setHorizontalGroup(
            panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelPortsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelPortsLayout.createSequentialGroup()
                            .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(panelPortsLayout.createSequentialGroup()
                                    .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(panelPortsLayout.createSequentialGroup()
                                            .addComponent(jLabel4)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtPort1))
                                        .addGroup(panelPortsLayout.createSequentialGroup()
                                            .addComponent(jLabel5)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtPort2, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelPortsLayout.createSequentialGroup()
                                            .addComponent(jLabel6)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtPort3, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE)))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel9)
                                            .addComponent(jLabel8, GroupLayout.Alignment.TRAILING))
                                        .addComponent(jLabel7, GroupLayout.Alignment.TRAILING)))
                                .addComponent(btnDefault))
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        panelPortsLayout.setVerticalGroup(
            panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelPortsLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(txtPort1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(txtPort2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel8))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(txtPort3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 103, Short.MAX_VALUE)
                    .addComponent(btnDefault)
                    .addContainerGap())
        );

        jTabbedPane1.addTab("CPU Ports", panelPorts);
        btnSave.addActionListener(this::btnOKActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jTabbedPane1)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(btnSave, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jTabbedPane1)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnSave))
                    .addContainerGap())
        );

        pack();
    }

    private void cmbDriveItemStateChanged(ItemEvent evt) {
        updateGUI(cmbDrive.getSelectedIndex());
    }

    private void txtImageFileInputMethodTextChanged() {
        btnMount.setEnabled(!txtImageFile.getText().equals(""));
    }

    private void btnUnmountActionPerformed(ActionEvent evt) {
        int index = cmbDrive.getSelectedIndex();
        Drive drive = drives.get(index);
        drive.umount();
        updateGUI(index);
    }

    private void btnMountActionPerformed(ActionEvent evt) {
        int index = cmbDrive.getSelectedIndex();
        Drive drive = drives.get(index);
        try {
            images.set(index, txtImageFile.getText());
            drive.mount(Path.of(txtImageFile.getText()));
        } catch (FileNotFoundException e) {
            dialogs.showError("Could not mount file. File is either not found, is directory, or is not readable.", "Mount image");
            txtImageFile.grabFocus();
        } catch (Exception ex) {
            LOGGER.error("Could not mount file: " + txtImageFile.getText(), ex);
            dialogs.showError("Could not mount file. Please see log file for details", "Mount image");
            txtImageFile.grabFocus();
        }
        updateGUI(index);
    }

    private void btnBrowseActionPerformed(ActionEvent evt) {
        Path currentDirectory = Optional
            .of(images.get(cmbDrive.getSelectedIndex()))
            .filter(p -> !p.isEmpty())
            .map(Path::of)
            .orElse(Path.of(System.getProperty("user.dir")));

        dialogs.chooseFile(
            "Open disk image", "Open", currentDirectory, false,
            new FileExtensionsFilter("Disk images", "dsk", "bin")
        ).ifPresent(path -> txtImageFile.setText(path.toString()));
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        RadixUtils radixUtils = RadixUtils.getInstance();
        JTextField textField = null;
        String name = "";

        int index = cmbDrive.getSelectedIndex();
        int parsedPort1;
        int parsedPort2;
        int parsedPort3;

        try {
            textField = txtPort1;
            name = "Port1";
            parsedPort1 = radixUtils.parseRadix(txtPort1.getText());
            textField = txtPort2;
            name = "Port2";
            parsedPort2 = radixUtils.parseRadix(txtPort2.getText());
            textField = txtPort3;
            name = "Port3";
            parsedPort3 = radixUtils.parseRadix(txtPort3.getText());

            textField = txtSectorsPerTrack;
            name = "Sectors per track";
            radixUtils.parseRadix(txtSectorsPerTrack.getText());

            textField = txtSectorSize;
            name = "Sector size";
            radixUtils.parseRadix(txtSectorSize.getText());

            sectorsPerTrack.set(index, txtSectorsPerTrack.getText());
            sectorSizes.set(index, txtSectorSize.getText());
            images.set(index, txtImageFile.getText());
        } catch (NumberFormatException e) {
            dialogs.showError(name + ": Invalid number format", "Save settings");
            textField.grabFocus();
            return;
        } catch (IllegalArgumentException e) {
            dialogs.showError(name + " must be greater than 0", "Save settings");
            textField.grabFocus();
            return;
        }

        saveSettings(parsedPort1, parsedPort2, parsedPort3);
        dispose();
    }

    private void btnDefaultActionPerformed(ActionEvent evt) {
        txtPort1.setText(String.format("0x%02X", DEFAULT_CPU_PORT1));
        txtPort2.setText(String.format("0x%02X", DEFAULT_CPU_PORT2));
        txtPort3.setText(String.format("0x%02X", DEFAULT_CPU_PORT3));
    }

    private void btnDefaultParamsActionPerformed(ActionEvent evt) {
        txtSectorsPerTrack.setText(String.valueOf(DiskSettings.DEFAULT_SECTORS_PER_TRACK));
        txtSectorSize.setText(String.valueOf(DiskSettings.DEFAULT_SECTOR_SIZE));
    }

    private final JButton btnMount = new JButton("Mount");
    private final JButton btnUnmount = new JButton("Umount");
    private final JComboBox<String> cmbDrive = new JComboBox<>();
    private final JTextField txtImageFile = new JTextField();
    private final JTextField txtPort1 = new JTextField(String.format("0x%02X", DEFAULT_CPU_PORT1));
    private final JTextField txtPort2 = new JTextField(String.format("0x%02X", DEFAULT_CPU_PORT2));
    private final JTextField txtPort3 = new JTextField(String.format("0x%02X", DEFAULT_CPU_PORT3));
    private final JTextField txtSectorSize = new JTextField(String.valueOf(DiskSettings.DEFAULT_SECTOR_SIZE));
    private final JTextField txtSectorsPerTrack = new JTextField(String.valueOf(DiskSettings.DEFAULT_SECTORS_PER_TRACK));
}
