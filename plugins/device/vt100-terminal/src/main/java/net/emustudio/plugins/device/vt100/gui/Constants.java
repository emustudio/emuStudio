package net.emustudio.plugins.device.vt100.gui;


import javax.swing.*;

import static net.emustudio.emulib.application.ui.GUI.loadIcon;

public class Constants {

    // not waiting for input
    public static final ImageIcon BLUE_ICON = loadIcon("/net/emustudio/plugins/device/vt100/gui/icons/indicator-blue.png", Constants.class);

    // waiting for input
    public static final ImageIcon RED_ICON = loadIcon("/net/emustudio/plugins/device/vt100/gui/icons/indicator-red.png", Constants.class);

    public static final ImageIcon ASCII_ICON = loadIcon("/net/emustudio/plugins/device/vt100/gui/icons/ascii.png", Constants.class);
}
