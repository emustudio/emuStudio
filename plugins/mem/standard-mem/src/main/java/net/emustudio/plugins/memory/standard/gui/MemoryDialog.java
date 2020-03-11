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
package net.emustudio.plugins.memory.standard.gui;

import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.memory.standard.gui.model.MemoryTableModel;
import net.emustudio.plugins.memory.standard.gui.model.TableMemory;
import net.emustudio.plugins.memory.standard.impl.MemoryContextImpl;
import net.emustudio.plugins.memory.standard.impl.MemoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatBinaryString;

public class MemoryDialog extends javax.swing.JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryDialog.class);

    private final MemoryContextImpl context;
    private final MemoryImpl memory;
    private final PluginSettings settings;
    private final Dialogs dialogs;

    private TableMemory table;
    private MemoryTableModel tableModel;

    public MemoryDialog(MemoryImpl memory, MemoryContextImpl context, PluginSettings settings, Dialogs dialogs) {
        this.context = Objects.requireNonNull(context);
        this.memory = Objects.requireNonNull(memory);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.tableModel = new MemoryTableModel(context);

        initComponents();
        super.setLocationRelativeTo(null);

        table = new TableMemory(tableModel, paneMemory);
        paneMemory.setViewportView(table);

        tableModel.addTableModelListener(e -> spnPage.getModel().setValue(tableModel.getPage()));
        lblPageCount.setText(String.valueOf(tableModel.getPageCount()));
        lblBanksCount.setText(String.valueOf(context.getBanksCount()));
        spnPage.addChangeListener(e -> {
            int i = (Integer) spnPage.getModel().getValue();
            try {
                tableModel.setPage(i);
            } catch (IndexOutOfBoundsException ex) {
                spnPage.getModel().setValue(tableModel.getPage());
            }
        });
        spnBank.addChangeListener(e -> {
            int i = (Integer) spnBank.getModel().getValue();
            try {
                tableModel.setCurrentBank(i);
            } catch (IndexOutOfBoundsException ex) {
                int currentBank = tableModel.getCurrentBank();
                spnBank.getModel().setValue(currentBank);
            }
        });

        super.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                destroyME();
            }
        });

        tableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            updateMemVal(row, column);
        });
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mousePressed(e);
                int row = table.getSelectedRow();
                int col = table.getSelectedColumn();
                updateMemVal(row, col);
            }
        });
        table.addKeyListener(new KeyboardHandler(table, spnPage.getModel(), this));
    }

    public void updateMemVal(int row, int column) {
        if (!table.isCellSelected(row, column)) {
            return;
        }
        int address = tableModel.getRowCount() * tableModel.getColumnCount()
            * tableModel.getPage() + row * tableModel.getColumnCount() + column;

        int data = Integer.parseInt(tableModel.getValueAt(row, column).toString(), 16);
        txtAddress.setText(String.format("%04X", address));
        txtChar.setText(String.format("%c", (char) (data & 0xFF)));
        txtValueDec.setText(String.format("%02d", data));
        txtValueHex.setText(String.format("%02X", data));
        txtValueOct.setText(String.format("%02o", data));
        txtValueBin.setText(formatBinaryString(data, 8));
    }

    private void destroyME() {
        dispose();
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JToolBar jToolBar1 = new javax.swing.JToolBar();
        JButton btnLoadImage = new JButton();
        JButton btnDump = new JButton();
        javax.swing.JToolBar.Separator jSeparator1 = new javax.swing.JToolBar.Separator();
        JButton btnGotoAddress = new JButton();
        JButton btnFind = new JButton();
        javax.swing.JToolBar.Separator jSeparator2 = new javax.swing.JToolBar.Separator();
        // Variables declaration - do not modify//GEN-BEGIN:variables
        JButton btnClean = new JButton();
        javax.swing.JToolBar.Separator jSeparator3 = new javax.swing.JToolBar.Separator();
        JButton btnSettings = new JButton();
        JSplitPane splitPane = new JSplitPane();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        spnPage = new javax.swing.JSpinner();
        lblPageCount = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        spnBank = new javax.swing.JSpinner();
        lblBanksCount = new javax.swing.JLabel();
        javax.swing.JPanel jPanel4 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        txtAddress = new javax.swing.JTextField();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        txtChar = new javax.swing.JTextField();
        javax.swing.JSeparator jSeparator4 = new javax.swing.JSeparator();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        txtValueDec = new javax.swing.JTextField();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        txtValueHex = new javax.swing.JTextField();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        txtValueOct = new javax.swing.JTextField();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        txtValueBin = new javax.swing.JTextField();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        paneMemory = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Standard Operating Memory");
        setSize(new java.awt.Dimension(794, 629));

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnLoadImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/standard/gui/document-open.png"))); // NOI18N
        btnLoadImage.setToolTipText("Load image...");
        btnLoadImage.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnLoadImage.setFocusable(false);
        btnLoadImage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnLoadImage.setOpaque(false);
        btnLoadImage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnLoadImage.addActionListener(this::btnLoadImageActionPerformed);
        jToolBar1.add(btnLoadImage);

        btnDump.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/standard/gui/document-save.png"))); // NOI18N
        btnDump.setToolTipText("Dump (save) memory...");
        btnDump.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnDump.setFocusable(false);
        btnDump.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDump.setOpaque(false);
        btnDump.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDump.addActionListener(this::btnDumpActionPerformed);
        jToolBar1.add(btnDump);
        jToolBar1.add(jSeparator1);

        btnGotoAddress.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/standard/gui/format-indent-more.png"))); // NOI18N
        btnGotoAddress.setToolTipText("Go to address...");
        btnGotoAddress.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnGotoAddress.setFocusable(false);
        btnGotoAddress.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnGotoAddress.setOpaque(false);
        btnGotoAddress.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGotoAddress.addActionListener(this::btnGotoAddressActionPerformed);
        jToolBar1.add(btnGotoAddress);

        btnFind.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/standard/gui/edit-find.png"))); // NOI18N
        btnFind.setToolTipText("Find sequence...");
        btnFind.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnFind.setFocusable(false);
        btnFind.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnFind.setOpaque(false);
        btnFind.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnFind.addActionListener(this::btnFindActionPerformed);
        jToolBar1.add(btnFind);
        jToolBar1.add(jSeparator2);

        btnClean.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/standard/gui/edit-clear.png"))); // NOI18N
        btnClean.setToolTipText("Erase memory");
        btnClean.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnClean.setFocusable(false);
        btnClean.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnClean.setOpaque(false);
        btnClean.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnClean.addActionListener(this::btnCleanActionPerformed);
        jToolBar1.add(btnClean);
        jToolBar1.add(jSeparator3);

        btnSettings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/standard/gui/preferences-system.png"))); // NOI18N
        btnSettings.setToolTipText("Settings...");
        btnSettings.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnSettings.setFocusable(false);
        btnSettings.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSettings.setOpaque(false);
        btnSettings.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSettings.addActionListener(this::btnSettingsActionPerformed);
        jToolBar1.add(btnSettings);

        splitPane.setDividerLocation(390);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Memory control"));

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel1.setText("Page number:");

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel2.setText("/");

        lblPageCount.setFont(lblPageCount.getFont().deriveFont(lblPageCount.getFont().getStyle() | java.awt.Font.BOLD));
        lblPageCount.setText("0");

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel3.setText("Memory bank:");

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel4.setText("/");

        lblBanksCount.setText("0");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel1)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(spnPage, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel2)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(lblPageCount)
                    .addGap(54, 54, 54)
                    .addComponent(jLabel3)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(spnBank, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel4)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(lblBanksCount)
                    .addContainerGap(283, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(spnPage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2)
                        .addComponent(lblPageCount)
                        .addComponent(jLabel3)
                        .addComponent(spnBank, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4)
                        .addComponent(lblBanksCount))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected value"));

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel5.setText("Address:");

        txtAddress.setEditable(false);
        txtAddress.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtAddress.setText("0000");

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel6.setText("Symbol:");

        txtChar.setEditable(false);
        txtChar.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel7.setText("Value:");

        txtValueDec.setEditable(false);
        txtValueDec.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtValueDec.setText("00");
        txtValueDec.setToolTipText("");

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel8.setText("(dec)");

        txtValueHex.setEditable(false);
        txtValueHex.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtValueHex.setText("00");

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel9.setText("(hex)");

        txtValueOct.setEditable(false);
        txtValueOct.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtValueOct.setText("000");

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel10.setText("(oct)");

        txtValueBin.setEditable(false);
        txtValueBin.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtValueBin.setText("0000 0000");
        txtValueBin.setToolTipText("");

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel11.setText("(bin)");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel5)
                        .addComponent(jLabel6))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtChar, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                        .addComponent(txtAddress, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(2, 2, 2)
                    .addComponent(jLabel7)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtValueHex)
                        .addComponent(txtValueDec, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel8)
                        .addComponent(jLabel9))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtValueBin)
                        .addComponent(txtValueOct, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel10))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                            .addGap(7, 7, 7)
                            .addComponent(jLabel11)))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel7)
                                .addComponent(txtValueDec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel8)
                                .addComponent(txtValueOct, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel10))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtValueHex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel9)
                                .addComponent(txtValueBin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel11)))
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel5)
                                    .addComponent(txtAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(txtChar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jSeparator4)))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        splitPane.setBottomComponent(jPanel2);

        paneMemory.setMinimumSize(new java.awt.Dimension(768, 300));
        splitPane.setLeftComponent(paneMemory);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(splitPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoadImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadImageActionPerformed
        File fileSource = FileChooser.selectFile(this, "Load image");

        if (fileSource != null) {
            if (fileSource.canRead()) {
                if (fileSource.getName().toLowerCase().endsWith(".hex")) {
                    context.loadHex(fileSource.getAbsolutePath(), 0);
                } else {
                    // ask for address where to load image
                    try {
                        dialogs
                            .readInteger("Enter image location address:", "Load image")
                            .ifPresent(address -> context.loadBin(fileSource.getAbsolutePath(), address, 0));
                    } catch (NumberFormatException e) {
                        dialogs.showError("Invalid number format", "Load image");
                    }
                }
                table.revalidate();
                table.repaint();
            } else {
                dialogs.showError("File " + fileSource.getPath() + " can't be read (unknown reason)).", "Load image");
            }
        }
    }//GEN-LAST:event_btnLoadImageActionPerformed

    private void btnCleanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCleanActionPerformed
        context.clear();
        tableModel.fireTableDataChanged();
    }//GEN-LAST:event_btnCleanActionPerformed

    private void btnGotoAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGotoAddressActionPerformed
        try {
            dialogs
                .readInteger("Enter memory address:", "Go to address")
                .ifPresent(address -> {
                    if (address < 0 || address >= context.getSize()) {
                        dialogs.showError(
                            "Address out of bounds (min=0, max=" + (context.getSize() - 1) + ")", "Go to address"
                        );
                    } else {
                        setPageFromAddress(address);
                    }
                });
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid number format", "Go to address");
        }
    }//GEN-LAST:event_btnGotoAddressActionPerformed

    private void btnFindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFindActionPerformed
        FindTextDialog dialog = new FindTextDialog(this, tableModel, getCurrentAddress());

        dialog.setVisible(true);

        int address = dialog.getFoundAddress();
        if (address != -1) {
            setPageFromAddress(address);
        }
    }//GEN-LAST:event_btnFindActionPerformed

    private void btnSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSettingsActionPerformed
        new SettingsDialog(this, memory, context, table, settings, dialogs).setVisible(true);
    }//GEN-LAST:event_btnSettingsActionPerformed

    private void btnDumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDumpActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter f1 = new FileNameExtensionFilter("Human-readable dump (*.txt)", "txt");
        FileNameExtensionFilter f2 = new FileNameExtensionFilter("Binary dump (*.bin)", "bin");

        fileChooser.setDialogTitle("Dump memory content into a file");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(f1);
        fileChooser.addChoosableFileFilter(f2);
        fileChooser.setFileFilter(f1);
        fileChooser.setApproveButtonText("Dump");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int returnVal = fileChooser.showOpenDialog(this);
        fileChooser.setVisible(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fileSource = fileChooser.getSelectedFile();
            try {
                if (fileSource.exists()) {
                    fileSource.delete();
                }
                fileSource.createNewFile();
                if (fileChooser.getFileFilter().equals(f1)) {
                    try (BufferedWriter out = new BufferedWriter(new FileWriter(fileSource))) {
                        for (int i = 0; i < context.getSize(); i++) {
                            out.write(String.format("%X:\t%02X\n", i, context.read(i)));
                        }
                    }
                } else {
                    // binary format
                    FileOutputStream fos = new FileOutputStream(fileSource);
                    try (DataOutputStream ds = new DataOutputStream(fos)) {
                        for (int i = 0; i < context.getSize(); i++) {
                            ds.writeByte(context.read(i) & 0xff);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Memory dump could not be created", e);
                dialogs.showError("Memory dump could not be created. Please see log file for more details.");
            }
        }
    }//GEN-LAST:event_btnDumpActionPerformed

    private int getCurrentAddress() {
        return tableModel.getPage() * (tableModel.getRowCount() * tableModel.getColumnCount());
    }

    private void setPageFromAddress(int address) {
        tableModel.setPage(address / (tableModel.getRowCount() * tableModel.getColumnCount()));
        int c = (address & 0xF);
        int r = (address & 0xF0) >> 4;
        try {
            table.setColumnSelectionInterval(c, c);
            table.setRowSelectionInterval(r, r);
            table.scrollRectToVisible(table.getCellRect(r, c, false));
            updateMemVal(r, c);
        } catch (RuntimeException ignored) {
        }
    }


    private javax.swing.JLabel lblBanksCount;
    private javax.swing.JLabel lblPageCount;
    private javax.swing.JScrollPane paneMemory;
    private javax.swing.JSpinner spnBank;
    private javax.swing.JSpinner spnPage;
    private javax.swing.JTextField txtAddress;
    private javax.swing.JTextField txtChar;
    private javax.swing.JTextField txtValueBin;
    private javax.swing.JTextField txtValueDec;
    private javax.swing.JTextField txtValueHex;
    private javax.swing.JTextField txtValueOct;
    // End of variables declaration//GEN-END:variables
}
