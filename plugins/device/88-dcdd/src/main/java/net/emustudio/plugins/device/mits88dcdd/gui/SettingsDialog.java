/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

import net.emustudio.emulib.runtime.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.device.mits88dcdd.SettingsConstants;
import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.device.mits88dcdd.DeviceImpl.*;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_PLAIN;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

public class SettingsDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final Dialogs dialogs;
    private final PluginSettings settings;
    private final DriveCollection drives;

    public SettingsDialog(JFrame parent, PluginSettings settings, DriveCollection drives, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.drives = Objects.requireNonNull(drives);
        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        setLocationRelativeTo(parent);

        readSettings();
        cmbDrive.setSelectedIndex(0);
        updateGUI(drives.get(0));
    }

    private void readSettings() {
        if (settings.contains(SettingsConstants.PORT1_CPU)) {
            txtPort1.setText(String.valueOf(settings.getInt(SettingsConstants.PORT1_CPU, DEFAULT_CPU_PORT1)));
        }
        if (settings.contains(SettingsConstants.PORT2_CPU)) {
            txtPort2.setText(String.valueOf(settings.getInt(SettingsConstants.PORT2_CPU, DEFAULT_CPU_PORT2)));
        }
        if (settings.contains(SettingsConstants.PORT3_CPU)) {
            txtPort3.setText(String.valueOf(settings.getInt(SettingsConstants.PORT3_CPU, DEFAULT_CPU_PORT3)));
        }
    }

    private void writeSettings() {
        RadixUtils radixUtils = RadixUtils.getInstance();

        try {
            settings.setInt(SettingsConstants.PORT1_CPU, radixUtils.parseRadix(txtPort1.getText()));
            settings.setInt(SettingsConstants.PORT2_CPU, radixUtils.parseRadix(txtPort2.getText()));
            settings.setInt(SettingsConstants.PORT3_CPU, radixUtils.parseRadix(txtPort3.getText()));

            drives.foreach((i, drive) -> {
                try {
                    settings.setInt(SettingsConstants.SECTORS_COUNT + i, drive.getSectorsCount());
                    settings.setInt(SettingsConstants.SECTOR_LENGTH + i, drive.getSectorLength());

                    Path imagePath = drive.getImagePath();
                    if (imagePath != null) {
                        settings.setString(SettingsConstants.IMAGE + i, imagePath.toAbsolutePath().toString());
                    } else {
                        settings.remove(SettingsConstants.IMAGE + i);
                    }
                } catch (CannotUpdateSettingException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        } catch (CannotUpdateSettingException | RuntimeException e) {
            LOGGER.error("Could not write " + DIALOG_TITLE + " settings", e);
            dialogs.showError("Could not write settings. Please see log for more details.", DIALOG_TITLE);
        }
    }

    private void updateGUI(Drive drive) {
        txtSectorLength.setText(String.valueOf(drive.getSectorLength()));
        txtSectorsCount.setText(String.valueOf(drive.getSectorsCount()));

        Path imagePath = drive.getImagePath();
        Optional.ofNullable(imagePath).ifPresentOrElse(path -> {
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
        cmbDrive = new JComboBox<>();
        JLabel jLabel1 = new JLabel();
        txtImageFile = new JTextField();
        JButton btnBrowse = new JButton();
        JPanel jPanel1 = new JPanel();
        btnMount = new JButton();
        btnUnmount = new JButton();
        JLabel jLabel2 = new JLabel();
        JPanel jPanel3 = new JPanel();
        JLabel jLabel10 = new JLabel();
        JLabel jLabel11 = new JLabel();
        JButton btnDefaultParams = new JButton();
        txtSectorsCount = new JTextField();
        txtSectorLength = new JTextField();
        JPanel panelPorts = new JPanel();
        JPanel jPanel2 = new JPanel();
        JLabel jLabel3 = new JLabel();
        JLabel jLabel4 = new JLabel();
        JLabel jLabel5 = new JLabel();
        JLabel jLabel6 = new JLabel();
        txtPort1 = new JTextField();
        txtPort2 = new JTextField();
        txtPort3 = new JTextField();
        JLabel jLabel7 = new JLabel();
        JLabel jLabel8 = new JLabel();
        JLabel jLabel9 = new JLabel();
        JButton btnDefault = new JButton();
        JButton btnOK = new JButton();
        chkSaveSettings = new JCheckBox();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle(DIALOG_TITLE + " Settings");

        jTabbedPane1.setFont(jTabbedPane1.getFont());

        cmbDrive.setModel(new DefaultComboBoxModel<>(new String[]{"Drive 0 (A)", "Drive 1 (B)", "Drive 2 (C)", "Drive 3 (D)", "Drive 4 (E)", "Drive 5 (F)", "Drive 6 (G)", "Drive 7 (H)", "Drive 8 (I)", "Drive 9 (J)", "Drive 10 (K)", "Drive 11 (L)", "Drive 12 (M)", "Drive 13 (N)", "Drive 14 (O)", "Drive 15 (P)"}));
        cmbDrive.addItemListener(this::cmbDriveItemStateChanged);

        jLabel1.setText("Image file name:");

        txtImageFile.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                txtImageFileInputMethodTextChanged();
            }

            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
        });

        btnBrowse.setText("Browse...");
        btnBrowse.addActionListener(this::btnBrowseActionPerformed);

        jPanel1.setBorder(BorderFactory.createTitledBorder(
            null, "Image operations", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            DIALOG_PLAIN
        ));

        btnMount.setText("Mount");
        btnMount.addActionListener(this::btnMountActionPerformed);

        btnUnmount.setText("Umount");
        btnUnmount.addActionListener(this::btnUnmountActionPerformed);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(btnMount)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnUnmount)
                    .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnMount)
                        .addComponent(btnUnmount))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel2.setText("Disk drive:");

        jPanel3.setBorder(BorderFactory.createTitledBorder(
            null, "Drive parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
           DIALOG_PLAIN
        ));

        jLabel10.setText("Sectors count:");
        jLabel11.setText("Sector length:");

        btnDefaultParams.setText("Change to Default");
        btnDefaultParams.addActionListener(this::btnDefaultParamsActionPerformed);

        txtSectorsCount.setText("32");
        txtSectorLength.setText("137");

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
                                .addComponent(txtSectorsCount)
                                .addComponent(txtSectorLength, GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE))))
                    .addContainerGap(58, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(txtSectorsCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel11)
                        .addComponent(txtSectorLength, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
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
                            .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );

        jTabbedPane1.addTab("Disk Images", panelImages);

        jPanel2.setBorder(BorderFactory.createTitledBorder("Note"));
        jLabel3.setText("Settings in this tab will be reflected after the restart of emuStudio.");

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

        jLabel4.setText("Port 1:");
        jLabel5.setText("Port 2:");
        jLabel5.setToolTipText("");
        jLabel6.setText("Port 3:");

        txtPort1.setText("0x08");
        txtPort2.setText("0x09");
        txtPort2.setToolTipText("");

        txtPort3.setText("0x0A");

        jLabel7.setText("(IN: flags, OUT: select/unselect drive)");
        jLabel8.setText("(IN: current sector, OUT: set flags)");
        jLabel9.setText("(IN: read data, OUT: write data)");

        btnDefault.setText("Change to Default");
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

        btnOK.setText("OK");
        btnOK.addActionListener(this::btnOKActionPerformed);

        chkSaveSettings.setSelected(true);
        chkSaveSettings.setText("Save settings to the file");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jTabbedPane1)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(chkSaveSettings)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnOK, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jTabbedPane1)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnOK)
                        .addComponent(chkSaveSettings))
                    .addContainerGap())
        );

        pack();
    }

    private void cmbDriveItemStateChanged(java.awt.event.ItemEvent evt) {
        updateGUI(drives.get(cmbDrive.getSelectedIndex()));
    }

    private void txtImageFileInputMethodTextChanged() {
        btnMount.setEnabled(!txtImageFile.getText().equals(""));
    }

    private void btnUnmountActionPerformed(java.awt.event.ActionEvent evt) {
        Drive drive = drives.get(cmbDrive.getSelectedIndex());
        drive.umount();
        updateGUI(drive);
    }

    private void btnMountActionPerformed(java.awt.event.ActionEvent evt) {
        Drive drive = drives.get(cmbDrive.getSelectedIndex());
        try {
            drive.mount(Path.of(txtImageFile.getText()));
        } catch (FileNotFoundException e) {
            dialogs.showError("Could not mount file. File is either not found, is directory, or is not readable.", "Mount image");
            txtImageFile.grabFocus();
        } catch (Exception ex) {
            LOGGER.error("Could not mount file: " + txtImageFile.getText(), ex);
            dialogs.showError("Could not mount file. Please see log file for details", "Mount image");
            txtImageFile.grabFocus();
        }
        updateGUI(drive);
    }

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        int driveIndex = cmbDrive.getSelectedIndex();
        Path imagePath = drives.get(driveIndex).getImagePath();

        Path currentDirectory = Optional
            .ofNullable(imagePath)
            .orElse(Path.of(System.getProperty("user.dir")));

        dialogs.chooseFile(
            "Open disk image", "Open", currentDirectory, false,
            new FileExtensionsFilter("Disk images", "dsk", "bin")
        ).ifPresent(path -> txtImageFile.setText(path.toString()));
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        RadixUtils radixUtils = RadixUtils.getInstance();
        JTextField textField = null;
        String name = "";

        Drive drive = drives.get(cmbDrive.getSelectedIndex());
        try {
            textField = txtPort1;
            name = "Port1";
            radixUtils.parseRadix(txtPort1.getText());

            textField = txtPort2;
            name = "Port2";
            radixUtils.parseRadix(txtPort2.getText());

            textField = txtPort3;
            name = "Port3";
            radixUtils.parseRadix(txtPort3.getText());

            textField = txtSectorsCount;
            name = "Sectors count";
            drive.setSectorsCount(radixUtils.parseRadix(txtSectorsCount.getText()));

            textField = txtSectorLength;
            name = "Sector length";
            drive.setSectorLength(radixUtils.parseRadix(txtSectorLength.getText()));
        } catch (NumberFormatException e) {
            dialogs.showError(name + ": Invalid number format", "Save settings");
            textField.grabFocus();
            return;
        } catch (IllegalArgumentException e) {
            dialogs.showError(name + " must be greater than 0", "Save settings");
            textField.grabFocus();
            return;
        }

        if (chkSaveSettings.isSelected()) {
            writeSettings();
        }
        dispose();
    }

    private void btnDefaultActionPerformed(java.awt.event.ActionEvent evt) {
        txtPort1.setText(String.format("0x%02X", DEFAULT_CPU_PORT1));
        txtPort2.setText(String.format("0x%02X", DEFAULT_CPU_PORT2));
        txtPort3.setText(String.format("0x%02X", DEFAULT_CPU_PORT3));
    }

    private void btnDefaultParamsActionPerformed(java.awt.event.ActionEvent evt) {
        txtSectorsCount.setText(String.valueOf(Drive.DEFAULT_SECTORS_COUNT));
        txtSectorLength.setText(String.valueOf(Drive.DEFAULT_SECTOR_LENGTH));
    }

    private JButton btnMount;
    private JButton btnUnmount;
    private JCheckBox chkSaveSettings;
    private JComboBox<String> cmbDrive;
    private JTextField txtImageFile;
    private JTextField txtPort1;
    private JTextField txtPort2;
    private JTextField txtPort3;
    private JTextField txtSectorLength;
    private JTextField txtSectorsCount;
}
