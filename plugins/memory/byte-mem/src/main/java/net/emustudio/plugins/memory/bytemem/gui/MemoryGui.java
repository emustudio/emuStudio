/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.plugins.cpu.CPUContext;

import net.emustudio.emulib.runtime.ui.StandardIcons;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
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
import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatBinary;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class MemoryGui extends DialogBase {
    private final GUI gui;

    private final MemoryTable table;
    private final MemoryTableModel tableModel;
    private final MemoryAnnotations annotations;
    private final CPUContext cpuContext;
    private boolean listening;
    private final CPUContext.CPUListener executionListener =
            new CPUContext.CPUListener() {
                @Override public void runStateChanged(net.emustudio.emulib.plugins.cpu.CPU.RunState state) { }
                @Override public void internalStateChanged() { debuggerListener.run(); }
            };
    private boolean handlingSearchEscape;
    private final Runnable debuggerListener;
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
    private final JTextField txtLabel = readOnlyDescription();
    private final JToggleButton btnAsciiMode = new JToggleButton();

    private final LoadImageAction loadImageAction;
    private final GotoAddressAction gotoAddressAction;
    private final FindSequenceAction findSequenceAction;
    private final FindSequencePanel findSequencePanel;
    private final NavigateSequenceAction findNextAction;
    private final NavigateSequenceAction findPreviousAction;
    private final JLabel lblSearchStatus = new JLabel(" ");
    private final EraseMemoryAction eraseMemoryAction;
    private final SettingsAction settingsAction;

    public MemoryGui(JFrame parent, MemoryImpl memory, MemoryContextImpl context, PluginSettings settings, Dialogs dialogs, GUI gui,
                     MemoryAnnotations annotations, CPUContext cpuContext) {
        super(parent, "Byte Operating Memory", false);
        this.gui = gui;
        this.annotations = annotations;
        this.cpuContext = cpuContext;
        this.lblPageCount = gui.labelBold("0");

        Objects.requireNonNull(context);
        Objects.requireNonNull(memory);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(dialogs);

        this.tableModel = new MemoryTableModel(context);
        this.table = new MemoryTable(tableModel, paneMemory, annotations, cpuContext);
        this.debuggerListener = () -> SwingUtilities.invokeLater(() -> {
            if (listening) {
                table.repaint();
                updateMemVal(table.getSelectedRow(), table.getSelectedColumn());
            }
        });

        this.loadImageAction = new LoadImageAction(dialogs, context, this, () -> {
            table.revalidate();
            table.repaint();
        }, gui);
        this.gotoAddressAction = new GotoAddressAction(dialogs, context, this::setPageFromAddress);
        MemorySearch search = new MemorySearch(tableModel, this::setPageFromAddress, lblSearchStatus::setText);
        this.findSequencePanel = new FindSequencePanel(dialogs, gui, this::getCurrentAddress, search::start, this::hideFindSequence);
        this.findSequenceAction = new FindSequenceAction(findSequencePanel::showSearch);
        this.findNextAction = new NavigateSequenceAction(search, false);
        this.findPreviousAction = new NavigateSequenceAction(search, true);

        AsciiModeAction asciiModeAction = new AsciiModeAction(tableModel, btnAsciiMode);
        btnAsciiMode.setAction(asciiModeAction);
        btnAsciiMode.setHideActionText(true);
        btnAsciiMode.setToolTipText(String.valueOf(asciiModeAction.getValue(SHORT_DESCRIPTION)));
        btnAsciiMode.setFocusable(false);

        this.eraseMemoryAction = new EraseMemoryAction(
                tableModel, context, loadIcon(StandardIcons.ICON_ERASE_MEMORY, MemoryGui.class));
        this.settingsAction = new SettingsAction(dialogs, this, memory, context, table, settings, gui);

        tableModel.addTableModelListener(e -> spnPage.getModel().setValue(tableModel.getPage()));
        lblPageCount.setText(String.valueOf(tableModel.getPageCount()));
        lblBanksCount.setText(String.valueOf(context.getBanksCount()));
        spnPage.addChangeListener(e -> {
            int i = (Integer) spnPage.getModel().getValue();
            try {
                tableModel.setPage(i);
                updateMemVal(table.getSelectedRow(), table.getSelectedColumn());
            } catch (IndexOutOfBoundsException ex) {
                spnPage.getModel().setValue(tableModel.getPage());
            }
        });
        spnBank.addChangeListener(e -> {
            int i = (Integer) spnBank.getModel().getValue();
            try {
                tableModel.setCurrentBank(i);
                updateMemVal(table.getSelectedRow(), table.getSelectedColumn());
            } catch (IndexOutOfBoundsException ex) {
                spnBank.getModel().setValue(tableModel.getCurrentBank());
            }
        });

        tableModel.addTableModelListener(e -> updateMemVal(table.getSelectedRow(), table.getSelectedColumn()));
        table.getSelectionModel().addListSelectionListener(e -> updateMemVal(table.getSelectedRow(), table.getSelectedColumn()));
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateMemVal(table.getSelectedRow(), table.getSelectedColumn()));
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
        txtValueBin.setText(formatBinary(data, 8));
        boolean mapped = tableModel.isMappedAddress(address);
        String label = mapped ? AnnotationLabels.at(annotations, address) : "";
        txtLabel.setText(label);
        txtLabel.setToolTipText(label.isEmpty() ? null : "Label: " + label);
    }

    @Override
    protected JComponent initializeComponents() {
        JToolBar toolBar = gui.toolBar();
        toolBar.add(gui.toolbarButton(loadImageAction));
        toolBar.addSeparator();
        toolBar.add(gui.toolbarButton(gotoAddressAction));
        toolBar.add(gui.toolbarButton(findSequenceAction));
        toolBar.add(gui.toolbarButton(findNextAction));
        toolBar.add(gui.toolbarButton(findPreviousAction));
        toolBar.addSeparator();
        toolBar.add(btnAsciiMode);
        toolBar.addSeparator();
        toolBar.add(gui.toolbarButton(eraseMemoryAction));
        toolBar.addSeparator();
        toolBar.add(gui.toolbarButton(settingsAction));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(lblSearchStatus);

        bindSearchShortcut("findSequence", findSequenceAction);
        bindSearchShortcut("findNext", findNextAction);
        bindSearchShortcut("findPrevious", findPreviousAction);

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
        panelValue.add(gui.label("(bin)"), "wrap");
        panelValue.add(gui.label("Label:"));
        panelValue.add(txtLabel, "span, growx, wrap");

        JPanel bottomPanel = gui.panel("insets dialog", "[grow]", "[]6[]");
        bottomPanel.add(panelControl, "growx, wrap");
        bottomPanel.add(panelValue, "growx");

        paneMemory.setViewportView(table);
        paneMemory.setMinimumSize(new Dimension(768, 300));
        paneMemory.setPreferredSize(new Dimension(768, 390));

        JSplitPane splitPane = gui.splitPane(JSplitPane.VERTICAL_SPLIT, paneMemory, bottomPanel, 1.0);

        JPanel content = gui.panel("insets 0, hidemode 3", "[grow]", "[][]6[grow]");
        content.add(toolBar, "growx, wrap");
        content.add(findSequencePanel, "growx, wrap");
        content.add(splitPane, "grow");

        return content;
    }

    private int getCurrentAddress() {
        return tableModel.getPage() * (tableModel.getRowCount() * tableModel.getColumnCount());
    }

    private void hideFindSequence() {
        findSequencePanel.setVisible(false);
        table.requestFocusInWindow();
    }

    @Override
    protected boolean shouldCloseOnEscape() {
        if (handlingSearchEscape) return false;
        if (findSequencePanel != null && findSequencePanel.isVisible()) {
            Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focused instanceof JComboBox && ((JComboBox<?>) focused).isPopupVisible()) return false;
            handlingSearchEscape = true;
            hideFindSequence();
            SwingUtilities.invokeLater(() -> handlingSearchEscape = false);
            return false;
        }
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && !listening) {
            if (annotations != null) annotations.addListener(debuggerListener);
            if (cpuContext != null) cpuContext.addCPUListener(executionListener);
            listening = true;
            table.repaint();
            updateMemVal(table.getSelectedRow(), table.getSelectedColumn());
        } else if (!visible && listening) {
            listening = false;
            if (annotations != null) annotations.removeListener(debuggerListener);
            if (cpuContext != null) cpuContext.removeCPUListener(executionListener);
        }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        if (listening) {
            listening = false;
            if (annotations != null) annotations.removeListener(debuggerListener);
            if (cpuContext != null) cpuContext.removeCPUListener(executionListener);
        }
        super.dispose();
    }

    private void bindSearchShortcut(String name, Action action) {
        KeyStroke shortcut = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shortcut, name);
        getRootPane().getActionMap().put(name, action);
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

    private static JTextField readOnlyDescription() {
        JTextField field = new JTextField();
        field.setEditable(false);
        return field;
    }
}
