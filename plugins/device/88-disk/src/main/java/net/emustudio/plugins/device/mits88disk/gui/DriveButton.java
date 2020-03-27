package net.emustudio.plugins.device.mits88disk.gui;

import javax.swing.*;

import static net.emustudio.plugins.device.mits88disk.gui.Constants.MONOSPACED_PLAIN;

public class DriveButton extends JToggleButton {
    private final static String ICON_OFF = "/net/emustudio/plugins/device/mits88disk/gui/off.gif";
    private final static String ICON_ON = "/net/emustudio/plugins/device/mits88disk/gui/on.gif";

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
