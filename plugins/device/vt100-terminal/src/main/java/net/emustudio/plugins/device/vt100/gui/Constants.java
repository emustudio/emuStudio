package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.emulib.runtime.ui.StandardIcons;

import javax.swing.*;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class Constants {

    // not waiting for input
    public static final ImageIcon BLUE_ICON = loadIcon(StandardIcons.ICON_INDICATOR_BLUE);

    // waiting for input
    public static final ImageIcon RED_ICON = loadIcon(StandardIcons.ICON_INDICATOR_RED);

    public static final ImageIcon ASCII_ICON = loadIcon(StandardIcons.ICON_ASCII);
}
