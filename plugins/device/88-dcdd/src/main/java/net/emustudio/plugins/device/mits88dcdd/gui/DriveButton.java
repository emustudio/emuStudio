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
    private final static String ICON_OFF = "/net/emustudio/plugins/device/mits88dcdd/gui/off.gif";
    private final static String ICON_ON = "/net/emustudio/plugins/device/mits88dcdd/gui/on.gif";

    public DriveButton(String text, Runnable action) {
        super(text, new ImageIcon(DriveButton.class.getResource(ICON_OFF)));
        addActionListener(actionEvent -> action.run());
        setFont(MONOSPACED_PLAIN);
    }

    public void turnOn() {
        setIcon(new ImageIcon(getClass().getResource(ICON_ON)));
    }

    public void turnOff() {
        setIcon(new ImageIcon(getClass().getResource(ICON_OFF)));
    }
}
