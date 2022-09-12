package net.emustudio.plugins.device.adm3a.gui;

import java.awt.*;
import java.awt.event.KeyListener;

public class GuiUtils {

    public static void addListenerRecursively(Component c, KeyListener listener) {
        c.addKeyListener(listener);
        if (c instanceof Container) {
            Container cont = (Container) c;
            Component[] children = cont.getComponents();
            for (Component child : children) {
                addListenerRecursively(child, listener);
            }
        }
    }

    public static void removeListenerRecursively(Component c, KeyListener listener) {
        c.removeKeyListener(listener);
        if (c instanceof Container) {
            Container cont = (Container) c;
            Component[] children = cont.getComponents();
            for (Component child : children) {
                removeListenerRecursively(child, listener);
            }
        }
    }
}
