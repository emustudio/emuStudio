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
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.RangeTree;
import net.emustudio.plugins.memory.bytemem.gui.model.FileImagesModel;
import net.emustudio.plugins.memory.bytemem.gui.model.ROMmodel;
import net.emustudio.plugins.memory.bytemem.gui.model.TableMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class SettingsDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final MemoryContextImpl context;
    private final MemoryImpl memory;
    private final TableMemory tblMem;
    private final FileImagesModel imagesModel;
    private final ROMmodel romModel;
    private final Dialogs dialogs;

    public SettingsDialog(JDialog parent, MemoryImpl memory, MemoryContextImpl context, TableMemory tblMem,
                          PluginSettings settings, Dialogs dialogs) {
        super(parent, true);

        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
        this.tblMem = Objects.requireNonNull(tblMem);
        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        super.setLocationRelativeTo(parent);

        loadSettings(settings);

        imagesModel = new FileImagesModel(settings, dialogs);
        tblImages.setModel(imagesModel);

        this.romModel = new ROMmodel(this.context);
        tblROM.setModel(romModel);
    }

    private void loadSettings(PluginSettings settings) {
        try {
            txtBanksCount.setText(String.valueOf(settings.getInt("banksCount", 0)));
            txtCommonBoundary.setText(String.format("0x%04X", settings.getInt("commonBoundary", 0)));
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format while loading settings: ", "Load settings");
        }
    }
    
    private void initComponents() {
        JPanel jPanel1 = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        tblROM = new JTable();
        JButton btnAddRange = new JButton();
        JButton btnRemoveRange = new JButton();
        chkApplyROMatStartup = new JCheckBox();
        JPanel jPanel3 = new JPanel();
        JLabel jLabel6 = new JLabel();
        txtBanksCount = new JTextField();
        JLabel jLabel7 = new JLabel();
        txtCommonBoundary = new JTextField();
        JSeparator jSeparator2 = new JSeparator();
        JLabel jLabel8 = new JLabel();
        JLabel jLabel9 = new JLabel();
        JLabel jLabel10 = new JLabel();
        JLabel jLabel1 = new JLabel();
        JPanel jPanel2 = new JPanel();
        JScrollPane jScrollPane2 = new JScrollPane();
        tblImages = new JTable();
        JButton btnAddImage = new JButton();
        JButton btnRemoveImage = new JButton();
        JButton btnLoadNow = new JButton();
        JButton btnOK = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Memory Settings");

        jPanel1.setBorder(BorderFactory.createTitledBorder("ROM areas"));

        tblROM.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(tblROM);

        btnAddRange.setText("Add");
        btnAddRange.addActionListener(this::btnAddRangeActionPerformed);

        btnRemoveRange.setText("Remove");
        btnRemoveRange.addActionListener(this::btnRemoveRangeActionPerformed);

        chkApplyROMatStartup.setText("Apply at startup");

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(chkApplyROMatStartup)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(btnRemoveRange)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnAddRange))
                        .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnAddRange)
                        .addComponent(btnRemoveRange))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(chkApplyROMatStartup)
                    .addContainerGap())
        );

        jPanel3.setBorder(BorderFactory.createTitledBorder("Bank-switching"));

        jLabel6.setText("Banks count:");
        txtBanksCount.setText("0");

        jLabel7.setText("Common boundary:");
        txtCommonBoundary.setText("0x0000");

        jLabel8.setHorizontalAlignment(SwingConstants.LEFT);
        jLabel8.setText("<html>Memory banks are different locations of memory wired in a way they share the addresses. Common area is shared across all banks. ");
        jLabel8.setVerticalAlignment(SwingConstants.TOP);

        jLabel9.setText("<html>Banks are accessible from <strong>[0..Common]</strong>.");
        jLabel10.setText("<html>Common area starts from <strong>[Common..memory end]</strong>.");
        jLabel1.setText("<html><strong>NOTE:</strong> Changes will be visible after restart.");

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                            .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel8, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(jSeparator2, GroupLayout.Alignment.LEADING)
                                .addGroup(GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel6)
                                        .addComponent(jLabel7))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(txtBanksCount)
                                        .addComponent(txtCommonBoundary))))
                            .addContainerGap())
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel9, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel10, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(txtBanksCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7)
                        .addComponent(txtCommonBoundary, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jSeparator2, GroupLayout.PREFERRED_SIZE, 2, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel8, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel9, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel10, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(BorderFactory.createTitledBorder("Files to load at startup"));

        tblImages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tblImages);

        btnAddImage.setText("Add");
        btnAddImage.addActionListener(this::btnAddImageActionPerformed);

        btnRemoveImage.setText("Remove");
        btnRemoveImage.addActionListener(this::btnRemoveImageActionPerformed);

        btnLoadNow.setText("Load now");
        btnLoadNow.addActionListener(this::btnLoadNowActionPerformed);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 498, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnLoadNow, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRemoveImage, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAddImage, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(btnAddImage)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnRemoveImage)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(btnLoadNow))
                        .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 117, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btnOK.setText("OK");
        btnOK.addActionListener(this::btnOKActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(btnOK, GroupLayout.PREFERRED_SIZE, 99, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnOK)
                    .addContainerGap())
        );

        pack();
    }

    private void btnAddRangeActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Optional<Integer> from = dialogs.readInteger("Enter FROM address:", "Add ROM range", 0);
            Optional<Integer> to = dialogs.readInteger("Enter TO address:", "Add ROM range", 0);

            if (from.isPresent() && to.isPresent()) {
                RangeTree.Range range = new RangeTree.Range(from.get(), to.get());
                context.setReadOnly(range);

                tblROM.revalidate();
                tblROM.repaint();
                tblMem.revalidate();
                tblMem.repaint();
            }
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format!", "Add ROM range");
        } catch (IllegalArgumentException e) {
            dialogs.showError(e.getMessage(), "Add ROM range");
        }
    }

    private void btnRemoveRangeActionPerformed(java.awt.event.ActionEvent evt) {
        RadixUtils radixUtils = RadixUtils.getInstance();
        int i = tblROM.getSelectedRow();

        Optional<RangeTree.Range> rangeOpt = Optional.empty();
        try {
            if (i >= 0) {
                int from = radixUtils.parseRadix((String) romModel.getValueAt(i, 0));
                int to = radixUtils.parseRadix((String) romModel.getValueAt(i, 1));
                rangeOpt = Optional.of(new RangeTree.Range(from, to));
            } else {
                Optional<Integer> from = dialogs.readInteger("Enter FROM address:", "Remove ROM range", 0);
                Optional<Integer> to = dialogs.readInteger("Enter TO address:", "Remove ROM range", 0);

                if (from.isPresent() && to.isPresent()) {
                    rangeOpt = Optional.of(new RangeTree.Range(from.get(), to.get()));
                }
            }

            rangeOpt.ifPresent(range -> {
                context.setReadWrite(range);
                tblROM.revalidate();
                tblROM.repaint();
                tblMem.revalidate();
                tblMem.repaint();
            });
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format", "Remove ROM range");
        }
    }

    private int getPositiveIntegerOrThrow(String name, JTextField textField) {
        try {
            int number = RadixUtils.getInstance().parseRadix(textField.getText());
            if (number < 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException e) {
            dialogs.showError(name + " has to be positive integer !");
            throw e;
        }
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            int bCount = getPositiveIntegerOrThrow("Banks count", txtBanksCount);
            int bCommon = getPositiveIntegerOrThrow("Common boundary", txtCommonBoundary);
            memory.saveCoreSettings(
                bCount, bCommon, imagesModel.getImageFullNames(), imagesModel.getImageAddresses(),
                imagesModel.getImageBanks()
            );
            if (chkApplyROMatStartup.isSelected()) {
                memory.saveROMRanges();
            }
            dispose();
        } catch (NumberFormatException ignored) {
            dialogs.showError("Could not save settings: invalid number format", "Save settings");
        } catch (CannotUpdateSettingException ex) {
            LOGGER.error("Could not save memory settings", ex);
            dialogs.showError("Could not save settings. Please see log file for more details");
        }
    }

    private void btnAddImageActionPerformed(java.awt.event.ActionEvent evt) {
        dialogs.chooseFile(
            "Add memory image", "Add", Path.of(System.getProperty("user.dir")), false,
            new FileExtensionsFilter("Memory images", "hex", "bin")
        ).ifPresent(path -> {
            try {

                boolean isHex = path.toString().toLowerCase().endsWith(".hex");
                Optional<Integer> imageAddress = Optional.empty();
                if (!isHex) {
                    imageAddress = dialogs.readInteger("Enter image address:", "Add image", 0);
                }

                final int bank = (context.getBanksCount() > 1)
                    ? dialogs.readInteger("Enter memory bank index:", "Add memory image", 0).orElse(0)
                    : 0;

                if (!isHex) {
                    imageAddress.ifPresent(address -> imagesModel.addImage(path, address, bank));
                } else {
                    imagesModel.addImage(path, 0, bank);
                }
            } catch (NumberFormatException e) {
                dialogs.showError("Invalid number format", "Add image");
            }
        });
    }

    private void btnRemoveImageActionPerformed(java.awt.event.ActionEvent evt) {
        int index = tblImages.getSelectedRow();
        if (index == -1) {
            dialogs.showError("Image has to be selected", "Remove image");
        } else {
            Dialogs.DialogAnswer answer = dialogs.ask(
                "Are you sure to remove selected image from the list?", "Remove image"
            );
            if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                imagesModel.removeImageAt(index);
            }
        }
    }

    private void btnLoadNowActionPerformed(java.awt.event.ActionEvent evt) {
        int index = tblImages.getSelectedRow();
        if (index == -1) {
            dialogs.showError("Image has to be selected", "Load image now");
        } else {
            memory.loadImage(
                Path.of(imagesModel.getFileNameAtRow(index)), imagesModel.getImageAddressAtRow(index),
                imagesModel.getImageBankAtRow(index)
            );
            tblMem.getTableModel().fireTableDataChanged();
        }
    }

    private JCheckBox chkApplyROMatStartup;
    private JTable tblImages;
    private JTable tblROM;
    private JTextField txtBanksCount;
    private JTextField txtCommonBoundary;
}
