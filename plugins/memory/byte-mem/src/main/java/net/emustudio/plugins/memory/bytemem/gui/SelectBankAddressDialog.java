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

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class SelectBankAddressDialog extends JDialog {
    private final RadixUtils ru = RadixUtils.getInstance();
    private final Dialogs dialogs;
    private final boolean selectBank;
    private final boolean selectAddress;
    private final JTextField txtBank = new JTextField("0");
    private final JTextField txtAddress = new JTextField("0");
    private int bank;
    private int address;
    private boolean okPressed;

    public SelectBankAddressDialog(JDialog parent, boolean selectBank, boolean selectAddress, Dialogs dialogs) {
        super(parent, true);
        this.selectBank = selectBank;
        this.selectAddress = selectAddress;
        this.dialogs = Objects.requireNonNull(dialogs);
        setLocationRelativeTo(parent);
        initComponents();
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

    private void initComponents() {
        setTitle("Select address");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JLabel lblBank = new JLabel("Memory bank:");
        JLabel lblAddress = new JLabel("Address:");
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

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblBank)
                                        .addComponent(lblAddress))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(txtAddress, 128, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtBank, 128, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnOK)
                                .addContainerGap()));

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblBank)
                                        .addComponent(txtBank, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAddress)
                                        .addComponent(txtAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(btnOK)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pack();
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
