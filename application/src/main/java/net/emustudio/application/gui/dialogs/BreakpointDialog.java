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
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.ConstantSizeButton;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.util.Objects;

/**
 * The breakpoint dialog - it asks user for the address where should be
 * set or unset the breakpoint.
 */
class BreakpointDialog extends JDialog {
    private final Dialogs dialogs;

    private int address = -1; // if adr == -1 then it means cancel
    private boolean set = false;

    BreakpointDialog(JFrame parent, Dialogs dialogs) {
        super(parent, true);

        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        setLocationRelativeTo(parent);
        txtAddress.grabFocus();
    }

    int getAddress() {
        return address;
    }

    public boolean isSet() {
        return set;
    }

    private void initComponents() {
        JLabel lblSetUnset = new JLabel();
        txtAddress = new JTextField();
        ConstantSizeButton btnSet = new ConstantSizeButton();
        ConstantSizeButton btnUnset = new ConstantSizeButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set/unset breakpoint");
        setResizable(false);

        lblSetUnset.setText("Set/unset breakpoint to address:");
        txtAddress.setText("0");

        btnSet.setText("Set");
        btnSet.addActionListener(this::btnSetActionPerformed);

        btnUnset.setText("Unset");
        btnUnset.addActionListener(this::btnUnsetActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup().addContainerGap()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblSetUnset).addComponent(txtAddress)
                .addGroup(GroupLayout.Alignment.CENTER, layout.createSequentialGroup()
                    .addComponent(btnUnset).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnSet))).addContainerGap());
        layout.setVerticalGroup(layout.createSequentialGroup().addContainerGap().addComponent(lblSetUnset)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(txtAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(btnUnset).addComponent(btnSet)).addContainerGap());


        pack();
    }

    private boolean parseAddress() {
        try {
            address = RadixUtils.getInstance().parseRadix(txtAddress.getText());
        } catch (NumberFormatException e) {
            dialogs.showError("Invalid address, try again !");
            txtAddress.grabFocus();
            return false;
        }
        return true;
    }

    private void btnSetActionPerformed(java.awt.event.ActionEvent evt) {
        if (parseAddress()) {
            set = true;
            dispose();
        }
    }

    private void btnUnsetActionPerformed(java.awt.event.ActionEvent evt) {
        if (parseAddress()) {
            set = false;
            dispose();
        }
    }

    private JTextField txtAddress;
}
