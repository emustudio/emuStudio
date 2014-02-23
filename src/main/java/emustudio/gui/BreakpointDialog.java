/*
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui;

import emulib.runtime.RadixUtils;
import emustudio.gui.utils.ConstantSizeButton;
import emustudio.main.Main;
import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 * The breakpoint dialog - it asks user for the address where should be
 * set or unset the breakpoint.
 */
@SuppressWarnings("serial")
public class BreakpointDialog extends JDialog {
    private int adr = -1; // if adr == -1 then it means cancel
    private boolean set = false;

    public BreakpointDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
        txtAddress.grabFocus();
    }

    public int getAddress() {
        return adr;
    }

    public boolean isSet() {
        return set;
    }

    private void initComponents() {
        lblSetUnset = new JLabel();
        txtAddress = new JTextField();
        btnSet = new ConstantSizeButton();
        btnUnset = new ConstantSizeButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set/unset breakpoint");
        setResizable(false);

        lblSetUnset.setText("Set/unset breakpoint to address:");
        lblSetUnset.setFont(lblSetUnset.getFont().deriveFont(lblSetUnset.getFont().getStyle() & ~java.awt.Font.BOLD));

        txtAddress.setText("0");

        btnSet.setText("Set");
        btnSet.setFont(btnSet.getFont().deriveFont(btnSet.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnSet.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetActionPerformed(evt);
            }
        });

        btnUnset.setText("Unset");
        btnUnset.setFont(btnUnset.getFont().deriveFont(btnUnset.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnUnset.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUnsetActionPerformed(evt);
            }
        });

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
            adr = RadixUtils.getInstance().parseRadix(txtAddress.getText());
        } catch (NumberFormatException e) {
            Main.tryShowErrorMessage("Invalid address, try again !");
            txtAddress.grabFocus();
            return false;
        }
        return true;
    }

    private void btnSetActionPerformed(java.awt.event.ActionEvent evt) {
        if (!parseAddress()) {
            return;
        }
        set = true;
        dispose();
    }

    private void btnUnsetActionPerformed(java.awt.event.ActionEvent evt) {
        if (!parseAddress()) {
            return;
        }
        set = false;
        dispose();
    }

    private ConstantSizeButton btnSet;
    private ConstantSizeButton btnUnset;
    private JLabel lblSetUnset;
    private JTextField txtAddress;
}
