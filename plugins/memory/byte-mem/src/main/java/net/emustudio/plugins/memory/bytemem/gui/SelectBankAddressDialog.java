/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class SelectBankAddressDialog extends DialogBase {
    private final GUI gui;
    private final RadixUtils ru = RadixUtils.getInstance();
    private final Dialogs dialogs;
    private final boolean selectBank;
    private final boolean selectAddress;
    private final JTextField txtBank = new JTextField("0");
    private final JTextField txtAddress = new JTextField("0");
    private int bank;
    private int address;
    private boolean okPressed;

    public SelectBankAddressDialog(JDialog parent, boolean selectBank, boolean selectAddress, Dialogs dialogs, GUI gui) {
        super(parent, "Select address", true);
        this.gui = gui;
        this.selectBank = selectBank;
        this.selectAddress = selectAddress;
        this.dialogs = Objects.requireNonNull(dialogs);
        buildContent();
        txtAddress.grabFocus();
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
            txtAddress.setSelectionStart(0);
            txtAddress.setSelectionEnd(txtAddress.getText().length());
        }

        JPanel content = gui.panel("insets dialog", "[][128!]", "[][][18][]]");
        content.add(lblBank);
        content.add(txtBank, "growx, wrap");
        content.add(lblAddress);
        content.add(txtAddress, "growx, wrap");
        content.add(btnOK, "span, align right");
        return content;
    }

    private void clickBtnOK(ActionEvent e) {
        try {
            this.bank = ru.parseRadix(txtBank.getText().trim());
        } catch (NumberFormatException ex) {
            dialogs.showError("Cannot parse memory bank", "Select address");
            txtBank.grabFocus();
            return;
        }
        try {
            this.address = ru.parseRadix(txtAddress.getText().trim());
        } catch (NumberFormatException ex) {
            dialogs.showError("Cannot parse memory address", "Select address");
            txtAddress.grabFocus();
            return;
        }
        okPressed = true;
        dispose();
    }
}
