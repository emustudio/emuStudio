/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
import net.sf.emustudio.devices.adm3a.InputProvider;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class Keyboard extends KeyAdapter implements ContainerListener, InputProvider {
    private static final int[] CONTROL_KEYCODES = new int[256];
    private static final int[] CONTROL_KEYCODES_ALWAYS_ACTIVE = new int[256];
    private static final int[] ESC_CURSOR_CODES = new int[256];

    static {
        CONTROL_KEYCODES['@'] = 0;
        CONTROL_KEYCODES['A'] = 1;
        CONTROL_KEYCODES['B'] = 2;
        CONTROL_KEYCODES['C'] = 3;
        CONTROL_KEYCODES['D'] = 4;
        CONTROL_KEYCODES['E'] = 5;
        CONTROL_KEYCODES['F'] = 6;
        CONTROL_KEYCODES['G'] = 7;
        CONTROL_KEYCODES['H'] = 8;
        CONTROL_KEYCODES['I'] = 9;
        CONTROL_KEYCODES['J'] = 10;
        CONTROL_KEYCODES['K'] = 11;
        CONTROL_KEYCODES['L'] = 12;
        CONTROL_KEYCODES['M'] = 13;
        CONTROL_KEYCODES['N'] = 14;
        CONTROL_KEYCODES['O'] = 15;
        CONTROL_KEYCODES['P'] = 16;
        CONTROL_KEYCODES['Q'] = 17;
        CONTROL_KEYCODES['R'] = 18;
        CONTROL_KEYCODES['S'] = 19;
        CONTROL_KEYCODES['T'] = 20;
        CONTROL_KEYCODES['U'] = 21;
        CONTROL_KEYCODES['V'] = 22;
        CONTROL_KEYCODES['W'] = 23;
        CONTROL_KEYCODES['X'] = 24;
        CONTROL_KEYCODES['Y'] = 25;
        CONTROL_KEYCODES['Z'] = 26;
        CONTROL_KEYCODES['['] = 27;
        CONTROL_KEYCODES['\\'] = 28;
        CONTROL_KEYCODES[']'] = 29;
        CONTROL_KEYCODES['^'] = 30;
        CONTROL_KEYCODES['-'] = 31;
        CONTROL_KEYCODES['a'] = 1;
        CONTROL_KEYCODES['b'] = 2;
        CONTROL_KEYCODES['c'] = 3;
        CONTROL_KEYCODES['d'] = 4;
        CONTROL_KEYCODES['e'] = 5;
        CONTROL_KEYCODES['f'] = 6;
        CONTROL_KEYCODES['g'] = 7;
        CONTROL_KEYCODES['h'] = 8;
        CONTROL_KEYCODES['i'] = 9;
        CONTROL_KEYCODES['j'] = 10;
        CONTROL_KEYCODES['k'] = 11;
        CONTROL_KEYCODES['l'] = 12;
        CONTROL_KEYCODES['m'] = 13;
        CONTROL_KEYCODES['n'] = 14;
        CONTROL_KEYCODES['o'] = 15;
        CONTROL_KEYCODES['p'] = 16;
        CONTROL_KEYCODES['q'] = 17;
        CONTROL_KEYCODES['r'] = 18;
        CONTROL_KEYCODES['s'] = 19;
        CONTROL_KEYCODES['t'] = 20;
        CONTROL_KEYCODES['u'] = 21;
        CONTROL_KEYCODES['v'] = 22;
        CONTROL_KEYCODES['w'] = 23;
        CONTROL_KEYCODES['x'] = 24;
        CONTROL_KEYCODES['y'] = 25;
        CONTROL_KEYCODES['z'] = 26;

        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_DOWN] = 10;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_UP] = 11;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_RIGHT] = 12;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_LEFT] = 8;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_ENTER] = 13;

        for (int i = 0; i < ESC_CURSOR_CODES.length; i++) {
            ESC_CURSOR_CODES[i] = -1;
        }

        ESC_CURSOR_CODES[' '] = 0;
        ESC_CURSOR_CODES['!'] = 1;
        ESC_CURSOR_CODES['\"'] = 2;
        ESC_CURSOR_CODES['#'] = 3;
        ESC_CURSOR_CODES['$'] = 4;
        ESC_CURSOR_CODES['%'] = 5;
        ESC_CURSOR_CODES['&'] = 6;
        ESC_CURSOR_CODES['\''] = 7;
        ESC_CURSOR_CODES['('] = 8;
        ESC_CURSOR_CODES[')'] = 9;
        ESC_CURSOR_CODES['*'] = 10;
        ESC_CURSOR_CODES['+'] = 11;
        ESC_CURSOR_CODES[','] = 12;
        ESC_CURSOR_CODES['-'] = 13;
        ESC_CURSOR_CODES['.'] = 14;
        ESC_CURSOR_CODES['/'] = 15;
        ESC_CURSOR_CODES['0'] = 16;
        ESC_CURSOR_CODES['1'] = 17;
        ESC_CURSOR_CODES['2'] = 18;
        ESC_CURSOR_CODES['3'] = 19;
        ESC_CURSOR_CODES['4'] = 20;
        ESC_CURSOR_CODES['5'] = 21;
        ESC_CURSOR_CODES['6'] = 22;
        ESC_CURSOR_CODES['7'] = 23;
        ESC_CURSOR_CODES['8'] = 24;
        ESC_CURSOR_CODES['9'] = 25;
        ESC_CURSOR_CODES[':'] = 26;
        ESC_CURSOR_CODES[';'] = 27;
        ESC_CURSOR_CODES['<'] = 28;
        ESC_CURSOR_CODES['='] = 29;
        ESC_CURSOR_CODES['>'] = 30;
        ESC_CURSOR_CODES['?'] = 31;
        ESC_CURSOR_CODES['@'] = 32;
        ESC_CURSOR_CODES['A'] = 33;
        ESC_CURSOR_CODES['B'] = 34;
        ESC_CURSOR_CODES['C'] = 35;
        ESC_CURSOR_CODES['D'] = 36;
        ESC_CURSOR_CODES['E'] = 37;
        ESC_CURSOR_CODES['F'] = 38;
        ESC_CURSOR_CODES['G'] = 39;
        ESC_CURSOR_CODES['H'] = 40;
        ESC_CURSOR_CODES['I'] = 41;
        ESC_CURSOR_CODES['J'] = 42;
        ESC_CURSOR_CODES['K'] = 43;
        ESC_CURSOR_CODES['L'] = 44;
        ESC_CURSOR_CODES['M'] = 45;
        ESC_CURSOR_CODES['N'] = 46;
        ESC_CURSOR_CODES['O'] = 47;
        ESC_CURSOR_CODES['P'] = 48;
        ESC_CURSOR_CODES['Q'] = 49;
        ESC_CURSOR_CODES['R'] = 50;
        ESC_CURSOR_CODES['S'] = 51;
        ESC_CURSOR_CODES['T'] = 52;
        ESC_CURSOR_CODES['U'] = 53;
        ESC_CURSOR_CODES['V'] = 54;
        ESC_CURSOR_CODES['W'] = 55;
        ESC_CURSOR_CODES['X'] = 56;
        ESC_CURSOR_CODES['Y'] = 57;
        ESC_CURSOR_CODES['Z'] = 58;
        ESC_CURSOR_CODES['['] = 59;
        ESC_CURSOR_CODES['\\'] = 60;
        ESC_CURSOR_CODES[']'] = 61;
        ESC_CURSOR_CODES['^'] = 62;
        ESC_CURSOR_CODES['_'] = 63;
        ESC_CURSOR_CODES['`'] = 64;
        ESC_CURSOR_CODES['a'] = 65;
        ESC_CURSOR_CODES['b'] = 66;
        ESC_CURSOR_CODES['c'] = 67;
        ESC_CURSOR_CODES['d'] = 68;
        ESC_CURSOR_CODES['e'] = 69;
        ESC_CURSOR_CODES['f'] = 70;
        ESC_CURSOR_CODES['g'] = 71;
        ESC_CURSOR_CODES['h'] = 72;
        ESC_CURSOR_CODES['i'] = 73;
        ESC_CURSOR_CODES['j'] = 74;
        ESC_CURSOR_CODES['k'] = 75;
        ESC_CURSOR_CODES['l'] = 76;
        ESC_CURSOR_CODES['m'] = 77;
        ESC_CURSOR_CODES['n'] = 78;
        ESC_CURSOR_CODES['o'] = 79;


    }

    private final List<DeviceContext<Short>> observers = new ArrayList<>();
    private final Cursor cursor;
    private int expect = 0;

    Keyboard(Cursor cursor) {
        this.cursor = Objects.requireNonNull(cursor);
    }

    void addListenerRecursively(Component c) {
        c.addKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            cont.addContainerListener(this);
            Component[] children = cont.getComponents();
            for (Component aChildren : children) {
                addListenerRecursively(aChildren);
            }
        }
    }

    private void removeListenerRecursively(Component c) {
        c.removeKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            cont.removeContainerListener(this);
            Component[] children = cont.getComponents();
            for (Component aChildren : children) {
                removeListenerRecursively(aChildren);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent evt) {
        int originalKeyCode = evt.getKeyCode() & 0xFF;
        int newKeyCode = 0;

        if (evt.isControlDown()) {
            newKeyCode = CONTROL_KEYCODES[originalKeyCode];
        } else {
            newKeyCode = CONTROL_KEYCODES_ALWAYS_ACTIVE[originalKeyCode];
            if (newKeyCode == 0) {
                int tmpKeyChar = evt.getKeyChar();
                if (tmpKeyChar > 254) {
                    return;
                }

                if (expect == 0 && originalKeyCode == KeyEvent.VK_ESCAPE) {
                    expect = '=';
                    return;
                } else if (expect == '=' && tmpKeyChar == '=') {
                    expect = -1;
                    return;
                } else if (expect == -1) {
                    int cursorPos = ESC_CURSOR_CODES[tmpKeyChar];
                    if (cursorPos != -1) {
                        if (cursorPos < 24) {
                            cursor.set(cursorPos, cursorPos);
                        } else {
                            cursor.set(cursorPos, cursor.getPoint().y);
                        }
                    }
                }
                expect = 0;
                newKeyCode = tmpKeyChar;
            }
        }
        if (newKeyCode != 0) {
            notifyObservers((short) newKeyCode);
        }
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
