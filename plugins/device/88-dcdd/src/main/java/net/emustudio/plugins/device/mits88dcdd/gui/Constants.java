/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.gui;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class Constants {
    public final static String DIALOG_TITLE = "MITS 88-DCDD";

    public final static Font DRIVE_BUTTON_FONT = FONT_MONOSPACED.deriveFont(14.0f);

    public final static ImageIcon ICON_UNSELECTED = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/unselected.png");
    public final static ImageIcon ICON_SELECTED = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/selected.png");

    public final static ImageIcon ICON_OFF = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/off.png");
    public final static ImageIcon ICON_ON = loadIcon("/net/emustudio/plugins/device/mits88dcdd/gui/on.png");
}
