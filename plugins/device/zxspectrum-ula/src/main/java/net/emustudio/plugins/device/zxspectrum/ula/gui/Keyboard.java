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
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.emulib.runtime.interaction.GuiUtils;
import net.jcip.annotations.ThreadSafe;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
public class Keyboard implements AutoCloseable, KeyListener, ContainerListener {
    private final List<OnKeyListener> onKeyListeners = new CopyOnWriteArrayList<>();

    public interface OnKeyListener {

        void onKeyDown(KeyEvent evt);

        void onKeyUp(KeyEvent evt);
    }

    public void addOnKeyListener(OnKeyListener onKeyListener) {
        onKeyListeners.add(Objects.requireNonNull(onKeyListener));
    }

    protected void notifyOnKeyDown(KeyEvent evt) {
        onKeyListeners.forEach(c -> c.onKeyDown(evt));
    }

    protected void notifyOnKeyUp(KeyEvent evt) {
        onKeyListeners.forEach(c -> c.onKeyUp(evt));
    }

    public void close() {
        onKeyListeners.clear();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent evt) {
        notifyOnKeyDown(evt);
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        notifyOnKeyUp(evt);
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        GuiUtils.addKeyListener(e.getChild(), this);
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        GuiUtils.removeKeyListener(e.getChild(), this);
    }
}
