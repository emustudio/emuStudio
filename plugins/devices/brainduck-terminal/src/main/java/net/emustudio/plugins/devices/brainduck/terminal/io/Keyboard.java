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
package net.emustudio.plugins.devices.brainduck.terminal.io;

import net.jcip.annotations.ThreadSafe;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

@ThreadSafe
public class Keyboard implements InputProvider, KeyListener {
    private final BlockingQueue<Integer> inputBuffer = new LinkedBlockingQueue<>();
    private final List<KeyboardListener> listeners = new CopyOnWriteArrayList<>();

    @ThreadSafe
    public interface KeyboardListener {
        void readStarted();

        void readEnded();
    }

    public void addListener(KeyboardListener listener) {
        listeners.add(listener);
    }

    private void notifyReadStarted() {
        listeners.forEach(KeyboardListener::readStarted);
    }

    private void notifyReadEnded() {
        listeners.forEach(KeyboardListener::readEnded);
    }

    @Override
    public void reset() {
        inputBuffer.clear();
    }

    @Override
    public int read() {
        notifyReadStarted();
        try {
            return inputBuffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            notifyReadEnded();
        }
        return IOProvider.EOF;
    }

    @Override
    public void close() {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keycode = e.getKeyCode();
        if (keycode == KeyEvent.VK_ESCAPE) {
            inputBuffer.add(InputProvider.EOF);
        } else if (!((keycode == KeyEvent.VK_SHIFT || keycode == KeyEvent.VK_CONTROL ||
            keycode == KeyEvent.VK_ALT || keycode == KeyEvent.VK_META))) {
            inputBuffer.add((int) e.getKeyChar());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
