/*
 * Keyboard.java
 *
 * Copyright (C) 2009-2013 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.devices.adm3a.impl;

import emulib.plugins.device.DeviceContext;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import net.sf.emustudio.devices.adm3a.InputProvider;

public class Keyboard extends KeyAdapter implements ContainerListener, InputProvider {
    private int keyCode = 0;
    private List<DeviceContext<Short>> observers = new ArrayList<DeviceContext<Short>>();

    public void addListenerRecursively(Component c) {
        c.addKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            cont.addContainerListener(this);
            Component[] children = cont.getComponents();
            for (int i = 0; i < children.length; i++) {
                addListenerRecursively(children[i]);
            }
        }
    }

    public void removeListenerRecursively(Component c) {
        c.removeKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            cont.removeContainerListener(this);
            Component[] children = cont.getComponents();
            for (int i = 0; i < children.length; i++) {
                removeListenerRecursively(children[i]);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent evt) {
        if (evt.isControlDown()) {
            switch (evt.getKeyCode()) {
                case (int) '@':
                    keyCode = 0;
                    break;
                case (int) 'A':
                    keyCode = 1;
                    break;
                case (int) 'B':
                    keyCode = 2;
                    break;
                case (int) 'C':
                    keyCode = 3;
                    break;
                case (int) 'D':
                    keyCode = 4;
                    break;
                case (int) 'E':
                    keyCode = 5;
                    break;
                case (int) 'F':
                    keyCode = 6;
                    break;
                case (int) 'G':
                    keyCode = 7;
                    break;
                case (int) 'H':
                    keyCode = 8;
                    break;
                case (int) 'I':
                    keyCode = 9;
                    break;
                case (int) 'J':
                    keyCode = 10;
                    break;
                case (int) 'K':
                    keyCode = 11;
                    break;
                case (int) 'L':
                    keyCode = 12;
                    break;
                case (int) 'M':
                    keyCode = 13;
                    break;
                case (int) 'N':
                    keyCode = 14;
                    break;
                case (int) 'O':
                    keyCode = 15;
                    break;
                case (int) 'P':
                    keyCode = 16;
                    break;
                case (int) 'Q':
                    keyCode = 17;
                    break;
                case (int) 'R':
                    keyCode = 18;
                    break;
                case (int) 'S':
                    keyCode = 19;
                    break;
                case (int) 'T':
                    keyCode = 20;
                    break;
                case (int) 'U':
                    keyCode = 21;
                    break;
                case (int) 'V':
                    keyCode = 22;
                    break;
                case (int) 'W':
                    keyCode = 23;
                    break;
                case (int) 'X':
                    keyCode = 24;
                    break;
                case (int) 'Y':
                    keyCode = 25;
                    break;
                case (int) 'Z':
                    keyCode = 26;
                    break;
                case (int) '[':
                    keyCode = 27;
                    break;
                case (int) '*':
                    keyCode = 28;
                    break;
                case (int) ']':
                    keyCode = 29;
                    break;
                case (int) '^':
                    keyCode = 30;
                    break;
                default:
                    return;
            }
        } else {
            int kC = evt.getKeyCode();
            if (kC == KeyEvent.VK_DOWN) {
                keyCode = 10;
            } else if (kC == KeyEvent.VK_UP) {
                keyCode = 11;
            } else if (kC == KeyEvent.VK_RIGHT) {
                keyCode = 12;
            } else if (kC == KeyEvent.VK_LEFT) {
                keyCode = 8;
            } else if (kC == KeyEvent.VK_ENTER) {
                keyCode = 13;
            } else {
                keyCode = (int) evt.getKeyChar();
                if (keyCode > 254) {
                    keyCode = 0;
                    return;
                }
            }
        }
        notifyObservers((short)keyCode);
    }

    private void notifyObservers(short input) {
        for (DeviceContext<Short> observer : observers) {
            observer.write(input);
        }
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        addListenerRecursively(e.getChild());
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        removeListenerRecursively(e.getChild());
    }

    @Override
    public void addDeviceObserver(DeviceContext<Short> listener) {
        observers.add(listener);
    }

    @Override
    public void removeDeviceObserver(DeviceContext<Short> listener) {
        observers.remove(listener);
    }

    @Override
    public void destroy() {
        observers.clear();
    }
}
