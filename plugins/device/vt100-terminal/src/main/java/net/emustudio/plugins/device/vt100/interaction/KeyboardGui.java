/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.interaction;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.device.vt100.api.Keyboard;
import net.jcip.annotations.ThreadSafe;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@ThreadSafe
public class KeyboardGui extends Keyboard implements KeyListener, ContainerListener {

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent evt) {
        int newKeyCode = evt.getKeyChar();
        if (newKeyCode >= 0xFF) {
            return;
        }
        notifyOnKey((byte) newKeyCode);
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
