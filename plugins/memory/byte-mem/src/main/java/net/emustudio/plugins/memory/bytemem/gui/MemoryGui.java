/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.MemoryImpl;
import net.emustudio.plugins.memory.bytemem.gui.actions.*;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static javax.swing.Action.SHORT_DESCRIPTION;
import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatBinaryString;

public class MemoryGui extends DialogBase {
    private final GUI gui;

    private final MemoryTable table;
    private final MemoryTableModel tableModel;
    private final JLabel lblBanksCount = new JLabel("0");
    private JLabel lblPageCount;
    private final JScrollPane paneMemory = new JScrollPane();
    private final JSpinner spnBank = new JSpinner();
    private final JSpinner spnPage = new JSpinner();
    private final JTextField txtAddress = readOnlyField("0000");
    private final JTextField txtChar = readOnlyField("");
    private final JTextField txtValueBin = readOnlyField("0000 0000");
    private final JTextField txtValueDec = readOnlyField("00");
    private final JTextField txtValueHex = readOnlyField("00");
    private final JTextField txtValueOct = readOnlyField("000");
    private final JToggleButton btnAsciiMode = new JToggleButton();

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final GotoAddressAction gotoAddressAction;
    private final FindSequenceAction findSequenceAction;
    private final EraseMemoryAction eraseMemoryAction;
    private final SettingsAction settingsAction;

    public MemoryGui(JFrame parent, MemoryImpl memory, MemoryContextImpl context, PluginSettings settings, Dialogs dialogs, GUI gui) {
        super(parent, "Byte Operating Memory", false);
        this.gui = gui;
        this.lblPageCount = gui.labelBold("0");

        Objects.requireNonNull(context);
        Objects.requireNonNull(memory);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(dialogs);

        this.tableModel = new MemoryTableModel(context);
        this.table = new MemoryTable(tableModel, paneMemory);

        this.loadImageAction = new LoadImageAction(dialogs, context, this, () -> {
            table.revalidate();
            table.repaint();
        }, gui);
        this.dumpMemoryAction = new DumpMemoryAction(dialogs, context);
        this.gotoAddressAction = new GotoAddressAction(dialogs, context, this::setPageFromAddress);
        this.findSequenceAction = new FindSequenceAction(dialogs, this::setPageFromAddress, tableModel,
                this::getCurrentAddress, this, gui);

        AsciiModeAction asciiModeAction = new AsciiModeAction(tableModel, btnAsciiMode);
        btnAsciiMode.setAction(asciiModeAction);
        btnAsciiMode.setHideActionText(true);
        btnAsciiMode.setToolTipText(String.valueOf(asciiModeAction.getValue(SHORT_DESCRIPTION)));
        btnAsciiMode.setFocusable(false);

        this.eraseMemoryAction = new EraseMemoryAction(tableModel, context);
        this.settingsAction = new SettingsAction(dialogs, this, memory, context, table, settings, gui);

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
                spnBank.getModel().setValue(tableModel.getCurrentBank());
            }
        });

        tableModel.addTableModelListener(e -> updateMemVal(e.getFirstRow(), e.getColumn()));
        MouseHandler mouseHandler = new MouseHandler(
                tableModel, () -> updateMemVal(table.getSelectedRow(), table.getSelectedColumn()));
        table.addMouseListener(mouseHandler);
        table.addMouseWheelListener(mouseHandler);
        table.addKeyListener(new KeyboardHandler(table, spnPage.getModel(), this));

        buildContent();
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

    @Override
    protected JComponent initializeComponents() {
        JToolBar toolBar = gui.toolBar();
        toolBar.add(gui.toolbarButton(loadImageAction));
        toolBar.add(gui.toolbarButton(dumpMemoryAction));
        toolBar.addSeparator();
        toolBar.add(gui.toolbarButton(gotoAddressAction));
        toolBar.add(gui.toolbarButton(findSequenceAction));
        toolBar.addSeparator();
        toolBar.add(btnAsciiMode);
        toolBar.addSeparator();
        toolBar.add(gui.toolbarButton(eraseMemoryAction));
        toolBar.addSeparator();
        toolBar.add(gui.toolbarButton(settingsAction));

        // Memory control section
        JPanel panelControl = gui.section("Memory control", "insets dialog", "[]6[75!]6[]6[]54[]6[75!]6[]6[]push", "[]");
        panelControl.add(gui.label("Page number:"));
        panelControl.add(spnPage);
        panelControl.add(gui.label("/"));
        panelControl.add(lblPageCount);
        panelControl.add(gui.label("Memory bank:"));
        panelControl.add(spnBank);
        panelControl.add(gui.label("/"));
        panelControl.add(lblBanksCount);

        // Selected value section
        JPanel panelValue = gui.section("Selected value", "insets dialog", "[][80!]20[][80!][][80!][]", "[][]");
        panelValue.add(gui.label("Address:"));
        panelValue.add(txtAddress);
        panelValue.add(gui.label("Value:"));
        panelValue.add(txtValueDec);
        panelValue.add(gui.label("(dec)"));
        panelValue.add(txtValueOct);
        panelValue.add(gui.label("(oct)"), "wrap");
        panelValue.add(gui.label("Symbol:"));
        panelValue.add(txtChar);
        panelValue.add(gui.label(""));
        panelValue.add(txtValueHex);
        panelValue.add(gui.label("(hex)"));
        panelValue.add(txtValueBin);
        panelValue.add(gui.label("(bin)"));

        JPanel bottomPanel = gui.panel("insets dialog", "[grow]", "[]6[]");
        bottomPanel.add(panelControl, "growx, wrap");
        bottomPanel.add(panelValue, "growx");

        paneMemory.setViewportView(table);
        paneMemory.setMinimumSize(new Dimension(768, 300));

        JSplitPane splitPane = gui.splitPaneTopToBottom(paneMemory, bottomPanel, 1.0);
        splitPane.setDividerLocation(390);

        JPanel content = gui.panel("insets 0", "[grow]", "[]6[grow]");
        content.add(toolBar, "growx, wrap");
        content.add(splitPane, "grow");

        setPreferredSize(new Dimension(794, 629));
        return content;
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

    private static JTextField readOnlyField(String text) {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setHorizontalAlignment(JTextField.RIGHT);
        return field;
    }
}
