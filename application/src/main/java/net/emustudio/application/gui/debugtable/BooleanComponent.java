/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import javax.swing.*;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_BREAKPOINT;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class BooleanComponent extends JLabel {
    public static final Icon BOOLEAN_ICON = loadIcon(ICON_BREAKPOINT);

    private boolean value;

    public BooleanComponent(boolean value) {
        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        setValue(value);
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        setIcon(value ? BOOLEAN_ICON : null);
    }
}
