/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.framework.EDialog;
import net.emustudio.application.gui.framework.EPanel;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioUI.*;

/**
 * The breakpoint dialog - it asks user for the address where should be
 * set or unset the breakpoint.
 */
public class BreakpointDialog extends EDialog {
    private final Dialogs dialogs;

    private int address = -1; // if adr == -1 then it means cancel
    private boolean set = false;
    private JTextField txtAddress;

    public BreakpointDialog(JFrame parent, Dialogs dialogs) {
        super(parent, "Set/unset breakpoint", true);

        this.dialogs = Objects.requireNonNull(dialogs);
        setResizable(false);
        buildContent();
    }

    public int getAddress() {
        return address;
    }

    public boolean isSet() {
        return set;
    }

    @Override
    protected JComponent initializeComponents() {
        EPanel panel = EPanel.vertical();

        panel.add(label("Set/unset breakpoint to address:"), "wrap, gapbottom 5");

        txtAddress = textField("0");
        panel.add(txtAddress, "growx, wrap, gapbottom 10");

        EPanel buttonPanel = EPanel.buttonBar();
        buttonPanel.add(button("Unset", this::btnUnsetActionPerformed), "");
        buttonPanel.add(button("Set", this::btnSetActionPerformed), "");

        panel.add(buttonPanel, "growx, span");

        SwingUtilities.invokeLater(() -> txtAddress.grabFocus());

        return panel;
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

    private void btnSetActionPerformed() {
        if (parseAddress()) {
            set = true;
            dispose();
        }
    }

    private void btnUnsetActionPerformed() {
        if (parseAddress()) {
            set = false;
            dispose();
        }
    }
}
