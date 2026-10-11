/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.gui;

import net.emustudio.emulib.runtime.ui.StandardIcons;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class Constants {
    public final static String DIALOG_TITLE = "MITS 88-DCDD";

    public final static Font DRIVE_BUTTON_FONT = FONT_MONOSPACED.deriveFont(14.0f);

    public final static ImageIcon ICON_UNSELECTED = loadIcon(StandardIcons.ICON_INDICATOR_RED);
    public final static ImageIcon ICON_SELECTED = loadIcon(StandardIcons.ICON_INDICATOR_GREEN);

    public final static ImageIcon ICON_OFF = loadIcon(StandardIcons.ICON_DISK_EMPTY);
    public final static ImageIcon ICON_ON = loadIcon(StandardIcons.ICON_DISK_MOUNTED);
}
