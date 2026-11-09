/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.RadixUtils;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.application.ui.GUI;
import net.emustudio.emulib.ui.components.DialogBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

public class SelectBankAddressDialog extends DialogBase {
    private final GUI gui;
    private final Dialogs dialogs;
    private final boolean selectBank;
    private final boolean selectAddress;
    private final JTextField txtBank;
    private final JTextField txtAddress;
    private int bank;
    private int address;
    private boolean okPressed;

    public SelectBankAddressDialog(JDialog parent, boolean selectBank, boolean selectAddress, Dialogs dialogs, GUI gui) {
        super(parent, "Select address", true);
        this.gui = gui;
        this.txtBank = gui.textField("0", 20);
        this.txtAddress = gui.textField("0", 20);
        this.selectBank = selectBank;
        this.selectAddress = selectAddress;
        this.dialogs = Objects.requireNonNull(dialogs);
        buildContent();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                toFront();
                requestFocus();
                if (selectAddress) {
                    txtAddress.requestFocusInWindow();
                } else if (selectBank) {
                    txtBank.requestFocusInWindow();
                }
            }
        });
    }

    public int getBank() {
        return bank;
    }

    public int getAddress() {
        return address;
    }

    public boolean isOk() {
        return okPressed;
    }

    @Override
    protected JComponent initializeComponents() {
        JLabel lblBank = gui.label("Memory bank:");
        JLabel lblAddress = gui.label("Address:");
        JButton btnOK = new JButton("OK");
        btnOK.setDefaultCapable(true);
        btnOK.addActionListener(this::clickBtnOK);
        getRootPane().setDefaultButton(btnOK);

        if (!selectBank) {
            lblBank.setEnabled(false);
            txtBank.setEnabled(false);
        }
        if (!selectAddress) {
            lblAddress.setEnabled(false);
            txtAddress.setEnabled(false);
        } else {
            txtAddress.setEnabled(true);
            txtAddress.setSelectionStart(0);
            txtAddress.setSelectionEnd(txtAddress.getText().length());
        }

        JPanel content = gui.panel("insets dialog", "[][128!]", "[][]18[]");
        content.add(lblBank);
        content.add(txtBank, "growx, wrap");
        content.add(lblAddress);
        content.add(txtAddress, "growx, wrap");
        content.add(btnOK, "span, align right");
        return content;
    }

    private void clickBtnOK(ActionEvent e) {
        try {
            this.bank = parseNonNegative(txtBank.getText().trim());
        } catch (NumberFormatException ex) {
            dialogs.showError("Cannot parse memory bank", "Select address");
            txtBank.grabFocus();
            return;
        }
        try {
            this.address = parseNonNegative(txtAddress.getText().trim());
        } catch (NumberFormatException ex) {
            dialogs.showError("Cannot parse memory address", "Select address");
            txtAddress.grabFocus();
            return;
        }
        okPressed = true;
        dispose();
    }

    private static int parseNonNegative(String text) {
        int value = RadixUtils.parseRadix(text);
        if (value < 0) {
            throw new NumberFormatException("Negative number: " + text);
        }
        return value;
    }
}
