/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.gui;

import javax.swing.*;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.ICON_SELECTED;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.ICON_UNSELECTED;

public class DriveButton extends JToggleButton {

    public DriveButton(String text, Runnable action) {
        super(text, ICON_UNSELECTED);
        setToolTipText("Disk is unselected");
        setFont(FONT_MONOSPACED);
        setFocusPainted(false);
        addActionListener(actionEvent -> action.run());
    }

    public void setSelected() {
        setIcon(ICON_SELECTED);
        setToolTipText("Disk is selected");
    }

    public void setUnselected() {
        setIcon(ICON_UNSELECTED);
        setToolTipText("Disk is unselected");
    }
}
