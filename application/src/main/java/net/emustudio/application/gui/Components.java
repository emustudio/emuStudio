/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.gui;

import java.awt.*;
import java.awt.event.KeyListener;

public class Components {

    /**
     * This method adds this key listener to all sub-components of given
     * component.
     *
     * @param c           Component to add this key listener recursively
     * @param keyListener the key listener object
     */
    public static void addKeyListenerRecursively(Component c, KeyListener keyListener) {
        c.addKeyListener(keyListener);
        if (c instanceof Container) {
            for (Component child : ((Container)c).getComponents()) {
                addKeyListenerRecursively(child, keyListener);
            }
        }
    }
}
