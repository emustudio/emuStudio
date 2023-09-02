package net.emustudio.plugins.memory.rasp.gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Constants {
    public static Font MONOSPACED_PLAIN = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public static ImageIcon loadIcon(String resource) {
        URL url = Constants.class.getResource(resource);
        return url == null ? null : new ImageIcon(url);
    }
}
