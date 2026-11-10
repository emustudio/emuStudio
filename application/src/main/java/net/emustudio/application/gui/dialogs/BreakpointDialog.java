/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.application.ui.GUI;
import net.emustudio.emulib.ui.components.DialogBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.IntPredicate;

/** Set or remove an execution breakpoint at a CPU address. */
public class BreakpointDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreakpointDialog.class);
    public enum Operation { SET, UNSET }

    public static final class Request {
        public final Operation operation;
        public final int address;

        public Request(Operation operation, int address) {
            this.operation = Objects.requireNonNull(operation);
            this.address = address;
        }
    }

    private final GUI gui;
    private final int initialAddress, breakpointLimit;
    private final Predicate<Request> submit;
    private final IntPredicate breakpointSet;
    private JTextField txtAddress;
    private JButton unset;

    public BreakpointDialog(JFrame parent, GUI gui, int initialAddress, int breakpointLimit,
                            IntPredicate breakpointSet, Predicate<Request> submit) {
        super(parent, "Set breakpoint", true);
        this.gui = Objects.requireNonNull(gui);
        this.initialAddress = initialAddress;
        this.breakpointLimit = breakpointLimit;
        this.submit = Objects.requireNonNull(submit);
        this.breakpointSet = Objects.requireNonNull(breakpointSet);
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

        JPanel buttons = gui.panel("insets 0, fillx", "[grow][][]", "");
        unset = gui.button("Unset");
        unset.addActionListener(e -> submit(Operation.UNSET));
        JButton set = gui.button("Set");
        set.addActionListener(e -> submit(Operation.SET));
        buttons.add(unset, "skip 1");
        buttons.add(set);
        panel.add(buttons, "span, growx");

        getRootPane().setDefaultButton(set);
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
        try {
            int address = Numbers.parseRadix(txtAddress.getText());
            if (address < 0 || address >= breakpointLimit) throw new NumberFormatException();
            boolean enabled = breakpointSet.test(address);
            unset.setEnabled(enabled);
        } catch (NumberFormatException e) {
            unset.setEnabled(false);
        }
    }

    private void submit(Operation operation) {
        Integer address = parseAddress(breakpointLimit);
        if (address != null && submit.test(new Request(operation, address))) dispose();
    }

    private Integer parseAddress(int limit) {
        try {
            int address = Numbers.parseRadix(txtAddress.getText());
            if (address < 0 || address >= limit) throw new NumberFormatException();
            return address;
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid address. Enter a value from 0 to " + (limit - 1) + ".", e);
            txtAddress.requestFocusInWindow();
            return null;
        }
    }
}
