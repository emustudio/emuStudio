/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.RangeTree;
import net.emustudio.plugins.memory.bytemem.gui.model.FileImagesModel;
import net.emustudio.plugins.memory.bytemem.gui.model.ROMmodel;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.memory.bytemem.gui.Constants.IMAGE_EXTENSION_FILTER;

public class SettingsDialog extends DialogBase {
    private final GUI gui;
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    private final MemoryContextImpl context;
    private final MemoryImpl memory;
    private final MemoryTable tblMem;
    private final FileImagesModel imagesModel;
    private final ROMmodel romModel;
    private final Dialogs dialogs;
    private final JCheckBox chkApplyROMatStartup = new JCheckBox("Apply at startup");
    private final JTable tblImages = new JTable();
    private final JTable tblROM = new JTable();
    private final JTextField txtBanksCount = new JTextField("0");
    private final JTextField txtCommonBoundary = new JTextField("0x0000");

    public SettingsDialog(JDialog parent, MemoryImpl memory, MemoryContextImpl context, MemoryTable tblMem,
                          PluginSettings settings, Dialogs dialogs, GUI gui) {
        super(parent, "Memory Settings", true);
        this.gui = gui;

        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
        this.tblMem = Objects.requireNonNull(tblMem);
        this.dialogs = Objects.requireNonNull(dialogs);

        loadSettings(settings);

        imagesModel = new FileImagesModel(settings, dialogs);
        tblImages.setModel(imagesModel);
        tblImages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.romModel = new ROMmodel(this.context);
        tblROM.setModel(romModel);
        tblROM.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        buildContent();
    }

    private void loadSettings(PluginSettings settings) {
        try {
            txtBanksCount.setText(String.valueOf(settings.getInt("banksCount", 0)));
            txtCommonBoundary.setText(String.format("0x%04X", settings.getInt("commonBoundary", 0)));
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format while loading settings: ", "Load settings");
        }
    }

    @Override
    protected JComponent initializeComponents() {
        // Bank-switching section
        JLabel lblDescription = new JLabel(
                "<html>Memory banks are different locations of memory wired in a way they share the addresses. Common area is shared across all banks. ");
        lblDescription.setHorizontalAlignment(SwingConstants.LEFT);
        lblDescription.setVerticalAlignment(SwingConstants.TOP);

        JPanel panelBanks = gui.section("Bank-switching", "insets dialog", "[][grow]", "");
        panelBanks.add(gui.label("Banks count:"));
        panelBanks.add(txtBanksCount, "growx, wrap");
        panelBanks.add(gui.label("Common boundary:"));
        panelBanks.add(txtCommonBoundary, "growx, wrap");
        panelBanks.add(new JSeparator(), "span, growx, h 2!, wrap");
        panelBanks.add(lblDescription, "span, h 64!, growx, wrap");
        panelBanks.add(new JLabel("<html>Banks are accessible from <strong>[0..Common]</strong>."), "span, wrap");
        panelBanks.add(new JLabel("<html>Common area starts from <strong>[Common..memory end]</strong>."), "span, wrap");
        panelBanks.add(new JLabel("<html><strong>NOTE:</strong> Changes will be visible after restart."), "span, gaptop 12");

        // ROM areas section
        JButton btnAddRange = new JButton("Add");
        btnAddRange.addActionListener(this::btnAddRangeActionPerformed);
        JButton btnRemoveRange = new JButton("Remove");
        btnRemoveRange.addActionListener(this::btnRemoveRangeActionPerformed);

        JPanel panelROM = gui.section("ROM areas", "insets dialog", "[grow]", "[116!][][grow][]");
        panelROM.add(new JScrollPane(tblROM), "grow, wrap");
        panelROM.add(btnRemoveRange, "split 2, align right");
        panelROM.add(btnAddRange, "wrap");
        panelROM.add(new JPanel(), "grow, wrap");
        panelROM.add(chkApplyROMatStartup);

        // Files to load at startup section
        JScrollPane scrollImages = new JScrollPane(tblImages);
        scrollImages.setPreferredSize(new Dimension(498, 117));

        JButton btnAddImage = new JButton("Add");
        btnAddImage.addActionListener(this::btnAddImageActionPerformed);
        JButton btnRemoveImage = new JButton("Remove");
        btnRemoveImage.addActionListener(this::btnRemoveImageActionPerformed);
        JButton btnLoadNow = new JButton("Load now");
        btnLoadNow.addActionListener(this::btnLoadNowActionPerformed);

        JPanel btnPanel = gui.panel("insets 0", "[grow]", "[][][unrel][]");
        btnPanel.add(btnAddImage, "growx, wrap");
        btnPanel.add(btnRemoveImage, "growx, wrap");
        btnPanel.add(btnLoadNow, "growx");

        JPanel panelImages = gui.section("Files to load at startup", "insets dialog", "[grow]unrel[]", "[]");
        panelImages.add(scrollImages, "grow");
        panelImages.add(btnPanel, "top");

        // OK button
        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(this::btnOKActionPerformed);

        JPanel content = gui.panel("insets dialog", "[340!]6[grow]", "[]6[]6[]");
        content.add(panelBanks, "grow");
        content.add(panelROM, "grow, wrap");
        content.add(panelImages, "span, growx, wrap");
        content.add(btnOK, "span, w 99!, align right");
        return content;
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
                "Add memory image", "Add", Path.of(System.getProperty("user.dir")), false, IMAGE_EXTENSION_FILTER
        ).ifPresent(path -> {
            boolean isHex = path.toString().toLowerCase().endsWith(".hex");
            boolean hasBanks = context.getBanksCount() > 1;

            int bank = 0;
            int address = 0;
            boolean ok = true;

            if (!isHex || hasBanks) {
                SelectBankAddressDialog dialog = new SelectBankAddressDialog(
                        this, hasBanks, !isHex, dialogs, gui
                );
                dialog.setVisible(true);
                ok = dialog.isOk();
                bank = dialog.getBank();
                address = dialog.getAddress();
            }

            if (ok) {
                imagesModel.addImage(path, address, bank);
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
            Path imagePath = Path.of(imagesModel.getFileNameAtRow(index));
            try {

                memory.loadImage(
                        imagePath, imagesModel.getImageAddressAtRow(index),
                        imagesModel.getImageBankAtRow(index)
                );
                tblMem.getTableModel().fireTableDataChanged();
            } catch (Exception e) {
                dialogs.showError("Could not load image: " + e.getMessage() + ". Please see log file for details.");
                LOGGER.error("Could not load memory image {}", imagePath, e);
            }
        }
    }
}
