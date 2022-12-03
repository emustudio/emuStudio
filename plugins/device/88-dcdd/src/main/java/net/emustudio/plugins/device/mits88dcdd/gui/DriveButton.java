/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88dcdd.gui;

import javax.swing.*;

import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.MONOSPACED_PLAIN;

public class DriveButton extends JToggleButton {
    private final static ImageIcon ICON_OFF = new ImageIcon(DriveButton.class.getResource("/net/emustudio/plugins/device/mits88dcdd/gui/unselected.gif"));
    private final static ImageIcon ICON_ON = new ImageIcon(DriveButton.class.getResource("/net/emustudio/plugins/device/mits88dcdd/gui/selected.gif"));

    public DriveButton(String text, Runnable action) {
        super(text, ICON_OFF);
        setToolTipText("Disk is unselected");
        setFont(MONOSPACED_PLAIN);
        addActionListener(actionEvent -> action.run());
    }

    public void setSelected() {
        setIcon(ICON_ON);
        setToolTipText("Disk is selected");
    }

    public void setUnselected() {
        setIcon(ICON_OFF);
        setToolTipText("Disk is unselected");
    }
}
