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
package net.emustudio.plugins.device.mits88dcdd.gui;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class Constants {
    public final static Font MONOSPACED_PLAIN = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public final static Font DRIVE_BUTTON_FONT = new Font("Monospaced", Font.PLAIN, 14);

    public final static String DIALOG_TITLE = "MITS 88-DCDD";

    public final static ImageIcon ICON_UNSELECTED = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/unselected.png");
    public final static ImageIcon ICON_SELECTED = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/selected.png");

    public final static ImageIcon ICON_OFF = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/off.png");
    public final static ImageIcon ICON_ON = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/on.png");
}
