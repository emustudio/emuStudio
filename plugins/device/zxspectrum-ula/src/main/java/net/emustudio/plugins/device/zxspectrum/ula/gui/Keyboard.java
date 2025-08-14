/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
