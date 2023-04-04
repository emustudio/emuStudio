/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
import net.emustudio.emulib.runtime.interaction.ToolbarButton;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.gui.actions.*;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import static javax.swing.Action.SHORT_DESCRIPTION;
import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatBinaryString;

public class MemoryGui extends JDialog {

    private final MemoryTable table;
    private final MemoryTableModel tableModel;
    private final JLabel lblBanksCount = new JLabel("0");
    private final JLabel lblPageCount = new JLabel("0");
    private final JScrollPane paneMemory = new JScrollPane();
    private final JSpinner spnBank = new JSpinner();
    private final JSpinner spnPage = new JSpinner();
    private final JTextField txtAddress = new JTextField("0000");
    private final JTextField txtChar = new JTextField();
    private final JTextField txtValueBin = new JTextField("0000 0000");
    private final JTextField txtValueDec = new JTextField("00");
    private final JTextField txtValueHex = new JTextField("00");
    private final JTextField txtValueOct = new JTextField("000");
    private final JToggleButton btnAsciiMode = new JToggleButton();

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final GotoAddressAction gotoAddressAction;
    private final FindSequenceAction findSequenceAction;
    private final EraseMemoryAction eraseMemoryAction;
    private final SettingsAction settingsAction;

    public MemoryGui(JFrame parent, MemoryImpl memory, MemoryContextImpl context, PluginSettings settings, Dialogs dialogs) {
        super(parent);

        Objects.requireNonNull(context);
        Objects.requireNonNull(memory);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(dialogs);

        this.tableModel = new MemoryTableModel(context);
        this.table = new MemoryTable(tableModel, paneMemory);

        this.loadImageAction = new LoadImageAction(dialogs, context, this, () -> {
            table.revalidate();
            table.repaint();
        });
        this.dumpMemoryAction = new DumpMemoryAction(dialogs, context);
        this.gotoAddressAction = new GotoAddressAction(dialogs, context, this::setPageFromAddress);
        this.findSequenceAction = new FindSequenceAction(dialogs, this::setPageFromAddress, tableModel,
                this::getCurrentAddress, this);

        AsciiModeAction asciiModeAction = new AsciiModeAction(tableModel, btnAsciiMode);
        btnAsciiMode.setAction(asciiModeAction);
        btnAsciiMode.setHideActionText(true);
        btnAsciiMode.setToolTipText(String.valueOf(asciiModeAction.getValue(SHORT_DESCRIPTION)));
        btnAsciiMode.setFocusable(false);

        this.eraseMemoryAction = new EraseMemoryAction(tableModel, context);
        this.settingsAction = new SettingsAction(dialogs, this, memory, context, table, settings);

        initComponents();
        super.setLocationRelativeTo(parent);

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
        JSplitPane splitPane = new JSplitPane();
        JPanel jPanel2 = new JPanel();
        JPanel jPanel3 = new JPanel();
        JLabel lblPageNumber = new JLabel("Page number:");
        JLabel lblPageFrom = new JLabel("/");
        JLabel lblMemoryBank = new JLabel("Memory bank:");
        JLabel lblMemoryBankFrom = new JLabel("/");
        JPanel jPanel4 = new JPanel();
        JLabel lblAddress = new JLabel("Address:");
        JLabel lblSymbol = new JLabel("Symbol:");
        JSeparator jSeparator4 = new JSeparator();
        JLabel lblValue = new JLabel("Value:");
        JLabel lblValueDec = new JLabel("(dec)");
        JLabel lblValueHex = new JLabel("(hex)");
        JLabel lblValueOct = new JLabel("(oct)");
        JLabel lblValueBin = new JLabel("(bin)");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Byte Operating Memory");
        setSize(new Dimension(794, 629));

        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(new ToolbarButton(loadImageAction));
        toolBar.add(new ToolbarButton(dumpMemoryAction));
        toolBar.addSeparator();
        toolBar.add(new ToolbarButton(gotoAddressAction));
        toolBar.add(new ToolbarButton(findSequenceAction));
        toolBar.addSeparator();
        toolBar.add(btnAsciiMode);
        toolBar.addSeparator();
        toolBar.add(new ToolbarButton(eraseMemoryAction));
        toolBar.addSeparator();
        toolBar.add(new ToolbarButton(settingsAction));

        splitPane.setDividerLocation(390);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0);

        jPanel3.setBorder(BorderFactory.createTitledBorder("Memory control"));

        lblPageCount.setFont(lblPageCount.getFont().deriveFont(lblPageCount.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblPageNumber)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnPage, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblPageFrom)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblPageCount)
                                .addGap(54, 54, 54)
                                .addComponent(lblMemoryBank)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnBank, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblMemoryBankFrom)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblBanksCount)
                                .addContainerGap(283, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblPageNumber)
                                        .addComponent(spnPage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblPageFrom)
                                        .addComponent(lblPageCount)
                                        .addComponent(lblMemoryBank)
                                        .addComponent(spnBank, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblMemoryBankFrom)
                                        .addComponent(lblBanksCount))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(BorderFactory.createTitledBorder("Selected value"));

        txtAddress.setEditable(false);
        txtAddress.setHorizontalAlignment(JTextField.RIGHT);

        txtChar.setEditable(false);
        txtChar.setHorizontalAlignment(JTextField.RIGHT);

        jSeparator4.setOrientation(SwingConstants.VERTICAL);

        txtValueDec.setEditable(false);
        txtValueDec.setHorizontalAlignment(JTextField.RIGHT);
        txtValueHex.setEditable(false);
        txtValueHex.setHorizontalAlignment(JTextField.RIGHT);
        txtValueOct.setEditable(false);
        txtValueOct.setHorizontalAlignment(JTextField.RIGHT);
        txtValueBin.setEditable(false);
        txtValueBin.setHorizontalAlignment(JTextField.RIGHT);

        GroupLayout jPanel4Layout = new GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblAddress)
                                        .addComponent(lblSymbol))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtChar, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                        .addComponent(txtAddress, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator4, GroupLayout.PREFERRED_SIZE, 13, GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(lblValue)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtValueHex)
                                        .addComponent(txtValueDec, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblValueDec)
                                        .addComponent(lblValueHex))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtValueBin)
                                        .addComponent(txtValueOct, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblValueOct))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                                                .addGap(7, 7, 7)
                                                .addComponent(lblValueBin)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(lblValue)
                                                        .addComponent(txtValueDec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lblValueDec)
                                                        .addComponent(txtValueOct, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lblValueOct))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(txtValueHex, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lblValueHex)
                                                        .addComponent(txtValueBin, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lblValueBin)))
                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addGroup(jPanel4Layout.createSequentialGroup()
                                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                                .addComponent(lblAddress)
                                                                .addComponent(txtAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                .addComponent(lblSymbol)
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
