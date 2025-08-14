/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import javax.swing.*;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class BooleanComponent extends JLabel {
    public static final Icon BOOLEAN_ICON = loadIcon("/net/emustudio/application/gui/dialogs/breakpoint.png");

    private boolean value;

    public BooleanComponent(boolean value) {
        this.value = value;
        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        if (value) {
            setIcon(BOOLEAN_ICON);
        } else {
            setIcon(null);
        }
    }
}
