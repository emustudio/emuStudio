/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.brainduck.terminal.interaction;

import net.emustudio.emulib.runtime.interaction.GuiUtils;
import net.emustudio.plugins.device.brainduck.terminal.api.Keyboard;
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
        GuiUtils.addKeyListener(e.getChild(), this);
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        GuiUtils.removeKeyListener(e.getChild(), this);
    }

    @Override
    public void process() {

    }
}
