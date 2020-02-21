/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui.utils;

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
            Container cont = (Container) c;
            Component[] children = cont.getComponents();
            for (Component child : children) {
                addKeyListenerRecursively(child, keyListener);
            }
        }
    }

}
