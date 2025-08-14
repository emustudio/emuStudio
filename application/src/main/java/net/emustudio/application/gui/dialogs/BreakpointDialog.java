/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.ConstantSizeButton;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * The breakpoint dialog - it asks user for the address where should be
 * set or unset the breakpoint.
 */
public class BreakpointDialog extends JDialog {
    private final Dialogs dialogs;

    private int address = -1; // if adr == -1 then it means cancel
    private boolean set = false;
    private JTextField txtAddress;

    public BreakpointDialog(JFrame parent, Dialogs dialogs) {
        super(parent, true);

        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        setLocationRelativeTo(parent);
        txtAddress.grabFocus();
    }

    public int getAddress() {
        return address;
    }

    public boolean isSet() {
        return set;
    }

    private void initComponents() {
        JLabel lblSetUnset = new JLabel();
        txtAddress = new JTextField();
        ConstantSizeButton btnSet = new ConstantSizeButton(this::btnSetActionPerformed);
        ConstantSizeButton btnUnset = new ConstantSizeButton(this::btnUnsetActionPerformed);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Set/unset breakpoint");
        setResizable(false);

        lblSetUnset.setText("Set/unset breakpoint to address:");
        txtAddress.setText("0");

        btnSet.setText("Set");
        btnUnset.setText("Unset");

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
}
