/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.device.adm3a.api.Keyboard;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;

public class KeyboardGui extends Keyboard implements ContainerListener, KeyListener {

    private static final int[] CONTROL_KEYCODES = new int[256];
    private static final int[] CONTROL_KEYCODES_ALWAYS_ACTIVE = new int[256];

    static {
        // Ctrl+A..Z (upper or lower case) → control codes 1..26
        for (char c = 'A'; c <= 'Z'; c++) {
            CONTROL_KEYCODES[c] = c - 'A' + 1;
            CONTROL_KEYCODES[Character.toLowerCase(c)] = c - 'A' + 1;
        }
        CONTROL_KEYCODES['['] = 27;
        CONTROL_KEYCODES['\\'] = 28;
        CONTROL_KEYCODES[']'] = 29;
        CONTROL_KEYCODES['^'] = 30;
        CONTROL_KEYCODES['-'] = 31;

        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_DOWN] = 10;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_UP] = 11;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_RIGHT] = 12;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_LEFT] = 8;
        CONTROL_KEYCODES_ALWAYS_ACTIVE[KeyEvent.VK_ENTER] = 13;
    }

    private final LoadCursorPosition loadCursorPosition;

    public KeyboardGui(Cursor cursor) {
        this.loadCursorPosition = new LoadCursorPosition(Objects.requireNonNull(cursor));
    }

    @Override
    public void keyTyped(KeyEvent e) {

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
                if (tmpKeyChar >= 0xFF) {
                    return;
                }
                newKeyCode = tmpKeyChar;
            }
        }
        if (newKeyCode != 0) {
            if (loadCursorPosition.notAccepted((byte) newKeyCode)) {
                notifyOnKey((byte) newKeyCode);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void componentAdded(ContainerEvent e) {
        GUI.addKeyListenerRecursively(e.getChild(), this);
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        GUI.removeKeyListenerRecursively(e.getChild(), this);
    }

    @Override
    public void process() {

    }
}
