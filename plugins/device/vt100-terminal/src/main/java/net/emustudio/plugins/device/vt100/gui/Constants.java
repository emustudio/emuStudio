package net.emustudio.plugins.device.vt100.gui;

import javax.swing.*;

import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class Constants {

    // not waiting for input
    public static final ImageIcon BLUE_ICON = loadIcon("/net/emustudio/plugins/device/vt100/16_circle_blue.png");

    // waiting for input
    public static final ImageIcon RED_ICON = loadIcon("/net/emustudio/plugins/device/vt100/16_circle_red.png");

    public static final ImageIcon ASCII_ICON = loadIcon("/net/emustudio/plugins/device/vt100/16_ascii.png");
}
