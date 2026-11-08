/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.debugger.BreakpointState;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.IntFunction;

/** One address entry point for execution breakpoints and live memory inspection. */
public class BreakpointDialog extends DialogBase {
    public enum Operation { SET, UNSET, INSPECT_ONLY }

    public static final class Request {
        public final Operation operation;
        public final int address;
        public final int bank;
        public final boolean keepInspection;

        public Request(Operation operation, int address, int bank, boolean keepInspection) {
            this.operation = Objects.requireNonNull(operation);
            this.address = address;
            this.bank = bank;
            this.keepInspection = keepInspection;
        }
    }

    private final Dialogs dialogs;
    private final GUI gui;
    private final int initialAddress, initialBank, banksCount, breakpointLimit, memoryLimit;
    private final boolean breakpointSupported;
    private final Predicate<Request> submit;
    private final IntFunction<BreakpointState> breakpointState;
    private JTextField txtAddress;
    private JComboBox<Integer> bank;
    private JCheckBox keepInspection;
    private JLabel existingState;
    private JButton unset;
    private boolean keepEdited;

    public BreakpointDialog(JFrame parent, Dialogs dialogs, GUI gui, int initialAddress, int initialBank, int banksCount,
                            int breakpointLimit, int memoryLimit, boolean breakpointSupported,
                            IntFunction<BreakpointState> breakpointState, Predicate<Request> submit) {
        super(parent, "Set breakpoint", true);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);
        this.initialAddress = initialAddress;
        this.initialBank = initialBank;
        this.banksCount = banksCount;
        this.breakpointLimit = breakpointLimit;
        this.memoryLimit = memoryLimit;
        this.breakpointSupported = breakpointSupported;
        this.submit = Objects.requireNonNull(submit);
        this.breakpointState = Objects.requireNonNull(breakpointState);
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panel = gui.panel("insets dialog, fillx", "[][grow]", "");
        txtAddress = gui.textField(String.format("0x%04X", initialAddress), 16);
        txtAddress.getAccessibleContext().setAccessibleName("Address");
        txtAddress.setToolTipText("Decimal or hexadecimal address, for example 42 or 0x2A");
        JLabel label = gui.label("Address");
        label.setLabelFor(txtAddress);
        panel.add(label);
        panel.add(txtAddress, "growx, wrap");

        bank = new JComboBox<>();
        for (int b = 0; b < banksCount; b++) bank.addItem(b);
        bank.setSelectedItem(initialBank);
        bank.setEnabled(banksCount > 1);
        bank.getAccessibleContext().setAccessibleName("Bank");
        bank.setToolTipText("Memory bank to inspect; does not switch the CPU's active bank");
        JLabel bankLabel = gui.label("Bank");
        bankLabel.setLabelFor(bank);
        panel.add(bankLabel);
        panel.add(bank, "growx, wrap");

        keepInspection = new JCheckBox("Keep inspection while stepping");
        keepInspection.setToolTipText("Every breakpoint opens memory at its address. Keep it there while stepping.");
        keepInspection.setEnabled(memoryLimit > 0 && breakpointSupported);
        keepInspection.addActionListener(event -> keepEdited = true);
        panel.add(keepInspection, "span, wrap, gapbottom unrel");
        if (memoryLimit > 0 && breakpointSupported)
            panel.add(gui.label("Every breakpoint inspects memory at this address."), "span, wrap");
        existingState = gui.label("");
        existingState.getAccessibleContext().setAccessibleName("Existing breakpoint state");
        panel.add(existingState, "span, wrap, gapbottom unrel");

        JPanel buttons = gui.panel("insets 0, fillx", "[][grow][][]", "");
        JButton inspect = gui.button("Inspect only", () -> submit(Operation.INSPECT_ONLY));
        inspect.setEnabled(memoryLimit > 0);
        unset = gui.button("Unset", () -> submit(Operation.UNSET));
        unset.setEnabled(breakpointSupported);
        JButton set = gui.button("Set", () -> submit(Operation.SET));
        set.setEnabled(breakpointSupported);
        buttons.add(inspect);
        buttons.add(unset, "skip 1");
        buttons.add(set);
        panel.add(buttons, "span, growx");
        getRootPane().setDefaultButton(breakpointSupported ? set : inspect);
        txtAddress.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { refreshExistingState(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { refreshExistingState(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { refreshExistingState(); }
        });
        refreshExistingState();
        SwingUtilities.invokeLater(() -> txtAddress.requestFocusInWindow());
        return panel;
    }

    private void refreshExistingState() {
        if (!breakpointSupported) {
            existingState.setText("Inspection only — execution breakpoints unavailable");
            return;
        }
        try {
            int address = RadixUtils.getInstance().parseRadix(txtAddress.getText());
            if (address < 0 || address >= breakpointLimit) throw new NumberFormatException();
            BreakpointState state = breakpointState.apply(address);
            existingState.setText(state == BreakpointState.OFF ? "No execution breakpoint at this address"
                    : state == BreakpointState.KEEP_INSPECTION ? "Breakpoint on · keeps inspection while stepping" : "Breakpoint on · inspects on hit");
            if (!keepEdited) keepInspection.setSelected(state == BreakpointState.KEEP_INSPECTION);
            unset.setEnabled(state != BreakpointState.OFF);
        } catch (NumberFormatException e) {
            existingState.setText("Enter a decimal or hexadecimal address");
            unset.setEnabled(false);
        }
    }

    private void submit(Operation operation) {
        int limit = operation == Operation.INSPECT_ONLY ? memoryLimit : breakpointLimit;
        if (operation == Operation.SET && memoryLimit > 0) limit = Math.min(limit, memoryLimit);
        final int address;
        try {
            address = RadixUtils.getInstance().parseRadix(txtAddress.getText());
            if (address < 0 || address >= limit) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid address. Enter a value from 0 to " + (limit - 1) + ".");
            txtAddress.requestFocusInWindow();
            return;
        }
        if (submit.test(new Request(operation, address, (Integer) bank.getSelectedItem(), keepInspection.isSelected()))) dispose();
    }
}
