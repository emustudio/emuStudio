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

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.gui.model.MemoryTableModel;
import net.emustudio.plugins.memory.bytemem.gui.model.TableMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatBinaryString;
import static net.emustudio.plugins.memory.bytemem.gui.Constants.IMAGE_EXTENSION_FILTER;

public class MemoryGui extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryGui.class);

    private final MemoryContextImpl context;
    private final MemoryImpl memory;
    private final PluginSettings settings;
    private final Dialogs dialogs;

    private final TableMemory table;
    private final MemoryTableModel tableModel;
    private final JLabel lblBanksCount = new JLabel();
    private final JLabel lblPageCount = new JLabel();
    private final JScrollPane paneMemory = new JScrollPane();
    private final JSpinner spnBank = new JSpinner();
    private final JSpinner spnPage = new JSpinner();
    private final JTextField txtAddress = new JTextField();
    private final JTextField txtChar = new JTextField();
    private final JTextField txtValueBin = new JTextField();
    private final JTextField txtValueDec = new JTextField();
    private final JTextField txtValueHex = new JTextField();
    private final JTextField txtValueOct = new JTextField();
    private final JToggleButton btnAsciiMode = new JToggleButton();


    public MemoryGui(JFrame parent, MemoryImpl memory, MemoryContextImpl context, PluginSettings settings, Dialogs dialogs) {
        super(parent);

        this.context = Objects.requireNonNull(context);
        this.memory = Objects.requireNonNull(memory);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.tableModel = new MemoryTableModel(context);

        initComponents();
        super.setLocationRelativeTo(parent);

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
                dispose();
            }
        });

        tableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            updateMemVal(row, column);
        });
        MouseHandler mouseHandler = new MouseHandler(
                tableModel, () -> updateMemVal(table.getSelectedRow(), table.getSelectedColumn()));
        table.addMouseListener(mouseHandler);
        table.addMouseWheelListener(mouseHandler);
        table.addKeyListener(new KeyboardHandler(table, spnPage.getModel(), this));
    }

    public void updateMemVal(int row, int column) {
        if (!table.isCellSelected(row, column)) {
            return;
        }
        int address = tableModel.getRowCount() * tableModel.getColumnCount()
                * tableModel.getPage() + row * tableModel.getColumnCount() + column;

        int data = tableModel.getRawValueAt(row, column);
        txtAddress.setText(String.format("%04X", address));
        txtChar.setText(String.format("%c", (char) (data & 0xFF)));
        txtValueDec.setText(String.format("%02d", data));
        txtValueHex.setText(String.format("%02X", data));
        txtValueOct.setText(String.format("%02o", data));
        txtValueBin.setText(formatBinaryString(data, 8));
    }

    private void initComponents() {
        JToolBar toolBar = new JToolBar();
        JButton btnLoadImage = new JButton();
        JButton btnDump = new JButton();
        JButton btnGotoAddress = new JButton();
        JButton btnFind = new JButton();
        JButton btnClean = new JButton();
        JButton btnSettings = new JButton();
        JSplitPane splitPane = new JSplitPane();
        JPanel jPanel2 = new JPanel();
        JPanel jPanel3 = new JPanel();
        JLabel jLabel1 = new JLabel();
        JLabel jLabel2 = new JLabel();
        JLabel jLabel3 = new JLabel();
        JLabel jLabel4 = new JLabel();
        JPanel jPanel4 = new JPanel();
        JLabel jLabel5 = new JLabel();
        JLabel jLabel6 = new JLabel();
        JSeparator jSeparator4 = new JSeparator();
        JLabel jLabel7 = new JLabel();
        JLabel jLabel8 = new JLabel();
        JLabel jLabel9 = new JLabel();
        JLabel jLabel10 = new JLabel();
        JLabel jLabel11 = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Byte Operating Memory");
        setSize(new Dimension(794, 629));

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        btnLoadImage.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/bytemem/gui/document-open.png")));
        btnLoadImage.setToolTipText("Load image...");
        btnLoadImage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnLoadImage.setFocusable(false);
        btnLoadImage.setHorizontalTextPosition(SwingConstants.CENTER);
        btnLoadImage.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnLoadImage.addActionListener(this::btnLoadImageActionPerformed);
        toolBar.add(btnLoadImage);

        btnDump.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/bytemem/gui/document-save.png")));
        btnDump.setToolTipText("Dump (save) memory...");
        btnDump.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnDump.setFocusable(false);
        btnDump.setHorizontalTextPosition(SwingConstants.CENTER);
        btnDump.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnDump.addActionListener(this::btnDumpActionPerformed);
        toolBar.add(btnDump);
        toolBar.addSeparator();

        btnGotoAddress.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/bytemem/gui/format-indent-more.png")));
        btnGotoAddress.setToolTipText("Go to address...");
        btnGotoAddress.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnGotoAddress.setFocusable(false);
        btnGotoAddress.setHorizontalTextPosition(SwingConstants.CENTER);
        btnGotoAddress.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnGotoAddress.addActionListener(this::btnGotoAddressActionPerformed);
        toolBar.add(btnGotoAddress);

        btnFind.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/bytemem/gui/edit-find.png")));
        btnFind.setToolTipText("Find sequence...");
        btnFind.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnFind.setFocusable(false);
        btnFind.setHorizontalTextPosition(SwingConstants.CENTER);
        btnFind.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnFind.addActionListener(this::btnFindActionPerformed);
        toolBar.add(btnFind);
        toolBar.addSeparator();

        btnAsciiMode.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/bytemem/gui/ascii-mode.png")));
        btnAsciiMode.setToolTipText("Toggle ASCII mode...");
        btnAsciiMode.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnAsciiMode.setFocusable(false);
        btnAsciiMode.setHorizontalTextPosition(SwingConstants.CENTER);
        btnAsciiMode.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnAsciiMode.setSelected(false);
        btnAsciiMode.addActionListener(this::btnSymbolModeActionPerformed);
        toolBar.add(btnAsciiMode);
        toolBar.addSeparator();

        btnClean.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/bytemem/gui/edit-clear.png")));
        btnClean.setToolTipText("Erase memory");
        btnClean.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnClean.setFocusable(false);
        btnClean.setHorizontalTextPosition(SwingConstants.CENTER);
        btnClean.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnClean.addActionListener(this::btnCleanActionPerformed);
        toolBar.add(btnClean);
        toolBar.addSeparator();

        btnSettings.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/bytemem/gui/preferences-system.png")));
        btnSettings.setToolTipText("Settings...");
        btnSettings.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        btnSettings.setFocusable(false);
        btnSettings.setHorizontalTextPosition(SwingConstants.CENTER);
        btnSettings.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnSettings.addActionListener(this::btnSettingsActionPerformed);
        toolBar.add(btnSettings);

        splitPane.setDividerLocation(390);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0);

        jPanel3.setBorder(BorderFactory.createTitledBorder("Memory control"));

        jLabel1.setText("Page number:");
        jLabel2.setText("/");

        lblPageCount.setFont(lblPageCount.getFont().deriveFont(lblPageCount.getFont().getStyle() | java.awt.Font.BOLD));
        lblPageCount.setText("0");

        jLabel3.setText("Memory bank:");
        jLabel4.setText("/");

        lblBanksCount.setText("0");

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnPage, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblPageCount)
                                .addGap(54, 54, 54)
                                .addComponent(jLabel3)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnBank, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblBanksCount)
                                .addContainerGap(283, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(spnPage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2)
                                        .addComponent(lblPageCount)
                                        .addComponent(jLabel3)
                                        .addComponent(spnBank, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4)
                                        .addComponent(lblBanksCount))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(BorderFactory.createTitledBorder("Selected value"));

        jLabel5.setText("Address:");

        txtAddress.setEditable(false);
        txtAddress.setHorizontalAlignment(JTextField.RIGHT);
        txtAddress.setText("0000");

        jLabel6.setText("Symbol:");

        txtChar.setEditable(false);
        txtChar.setHorizontalAlignment(JTextField.RIGHT);

        jSeparator4.setOrientation(SwingConstants.VERTICAL);

        jLabel7.setText("Value:");

        txtValueDec.setEditable(false);
        txtValueDec.setHorizontalAlignment(JTextField.RIGHT);
        txtValueDec.setText("00");
        txtValueDec.setToolTipText("");

        jLabel8.setText("(dec)");

        txtValueHex.setEditable(false);
        txtValueHex.setHorizontalAlignment(JTextField.RIGHT);
        txtValueHex.setText("00");

        jLabel9.setText("(hex)");

        txtValueOct.setEditable(false);
        txtValueOct.setHorizontalAlignment(JTextField.RIGHT);
        txtValueOct.setText("000");

        jLabel10.setText("(oct)");

        txtValueBin.setEditable(false);
        txtValueBin.setHorizontalAlignment(JTextField.RIGHT);
        txtValueBin.setText("0000 0000");
        txtValueBin.setToolTipText("");

        jLabel11.setText("(bin)");

        GroupLayout jPanel4Layout = new GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel5)
                                        .addComponent(jLabel6))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtChar, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                        .addComponent(txtAddress, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator4, GroupLayout.PREFERRED_SIZE, 13, GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(jLabel7)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtValueHex)
                                        .addComponent(txtValueDec, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel9))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtValueBin)
                                        .addComponent(txtValueOct, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel10))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                                                .addGap(7, 7, 7)
                                                .addComponent(jLabel11)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel7)
                                                        .addComponent(txtValueDec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel8)
                                                        .addComponent(txtValueOct, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel10))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(txtValueHex, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel9)
                                                        .addComponent(txtValueBin, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel11)))
                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addGroup(jPanel4Layout.createSequentialGroup()
                                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                                .addComponent(jLabel5)
                                                                .addComponent(txtAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                .addComponent(jLabel6)
                                                                .addComponent(txtChar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                                .addComponent(jSeparator4)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        splitPane.setBottomComponent(jPanel2);

        paneMemory.setMinimumSize(new java.awt.Dimension(768, 300));
        splitPane.setLeftComponent(paneMemory);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(toolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(splitPane)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnLoadImageActionPerformed(ActionEvent evt) {
        dialogs.chooseFile(
                "Load memory image", "Load", Path.of(System.getProperty("user.dir")), false, IMAGE_EXTENSION_FILTER
        ).ifPresent(path -> {
            boolean isHex = path.toString().toLowerCase().endsWith(".hex");
            boolean hasBanks = context.getBanksCount() > 1;

            int bank = 0;
            int address = 0;
            boolean ok = true;

            if (!isHex || hasBanks) {
                SelectBankAddressDialog dialog = new SelectBankAddressDialog(
                        this, hasBanks, !isHex, dialogs
                );
                dialog.setVisible(true);
                ok = dialog.isOk();
                bank = dialog.getBank();
                address = dialog.getAddress();
            }

            if (isHex && ok) {
                context.loadHex(path, bank);
            }
            if (!isHex && ok) {
                context.loadBin(path, address, bank);
            }

            table.revalidate();
            table.repaint();
        });
    }

    private void btnCleanActionPerformed(ActionEvent evt) {
        context.clear();
        tableModel.fireTableDataChanged();
    }

    private void btnGotoAddressActionPerformed(ActionEvent evt) {
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
    }

    private void btnFindActionPerformed(ActionEvent evt) {
        AtomicInteger foundAddress = new AtomicInteger(-1);
        FindSequenceDialog dialog = new FindSequenceDialog(
                dialogs, this, tableModel, getCurrentAddress(), foundAddress::set
        );

        dialog.setVisible(true);

        int address = foundAddress.get();
        if (address != -1) {
            setPageFromAddress(address);
        }
    }

    private void btnSymbolModeActionPerformed(ActionEvent evt) {
        tableModel.setAsciiMode(btnAsciiMode.isSelected());
    }

    private void btnSettingsActionPerformed(ActionEvent evt) {
        new SettingsDialog(this, memory, context, table, settings, dialogs).setVisible(true);
    }

    private void btnDumpActionPerformed(ActionEvent evt) {
        Path currentDirectory = Path.of(System.getProperty("user.dir"));
        dialogs.chooseFile(
                "Dump memory content into a file", "Save", currentDirectory, true,
                new FileExtensionsFilter("Human-readable dump", "txt"),
                new FileExtensionsFilter("Binary dump", "bin")
        ).ifPresent(path -> {
            try {
                if (path.toString().toLowerCase().endsWith(".txt")) {
                    try (BufferedWriter out = new BufferedWriter(new FileWriter(path.toFile()))) {
                        for (int i = 0; i < context.getSize(); i++) {
                            out.write(String.format("%X:\t%02X\n", i, context.read(i)));
                        }
                    }
                } else {
                    try (DataOutputStream ds = new DataOutputStream(new FileOutputStream(path.toFile()))) {
                        for (int i = 0; i < context.getSize(); i++) {
                            ds.writeByte(context.read(i) & 0xff);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Memory dump could not be created", e);
                dialogs.showError("Memory dump could not be created. Please see log file for more details.");
            }
        });
    }

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
}
