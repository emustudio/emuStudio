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
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.emulib.plugins.device.DeviceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyboardGui extends KeyAdapter implements ContainerListener, Keyboard {
    private final static Logger LOGGER = LoggerFactory.getLogger(KeyboardGui.class);

    private static final int[] CONTROL_KEYCODES = new int[256];
    private static final int[] CONTROL_KEYCODES_ALWAYS_ACTIVE = new int[256];

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
    }

    private final List<DeviceContext<Short>> devices = new ArrayList<>();
    private final LoadCursorPosition loadCursorPosition;

    public KeyboardGui(Cursor cursor) {
        this.loadCursorPosition = new LoadCursorPosition(Objects.requireNonNull(cursor));
    }

    public void addListenerRecursively(Component c) {
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

    @Override
    public void keyPressed(KeyEvent evt) {
        int originalKeyCode = evt.getKeyCode() & 0xFF;
        int newKeyCode;

        if (evt.isControlDown()) {
            newKeyCode = CONTROL_KEYCODES[originalKeyCode];
        } else {
            newKeyCode = CONTROL_KEYCODES_ALWAYS_ACTIVE[originalKeyCode];
            if (newKeyCode == 0) {
                int tmpKeyChar = evt.getKeyChar();
                if (tmpKeyChar > 254) {
                    return;
                }
                newKeyCode = tmpKeyChar;
            }
        }
        if (newKeyCode != 0) {
            if (loadCursorPosition.notAccepted((short) newKeyCode)){
                inputReceived((short) newKeyCode);
            }
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
    public void connect(DeviceContext<Short> device) {
        devices.add(device);
    }

    @Override
    public void disconnect(DeviceContext<Short> device) {
        devices.remove(device);
    }

    @Override
    public void destroy() {
        devices.clear();
    }

    private void inputReceived(short input) {
        for (DeviceContext<Short> device : devices) {
            try {
                device.writeData(input);
            } catch (IOException e) {
                LOGGER.error("[device={}, input={}] Could not notify device about key pressed", device, input, e);
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
}
