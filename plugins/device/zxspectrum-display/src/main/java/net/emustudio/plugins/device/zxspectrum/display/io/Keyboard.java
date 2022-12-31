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
package net.emustudio.plugins.device.zxspectrum.display.io;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.jcip.annotations.ThreadSafe;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

@ThreadSafe
public class Keyboard implements KeyListener {
    private final List<DeviceContext<Byte>> devices = new ArrayList<>();

    public void connect(DeviceContext<Byte> device) {
        devices.add(device);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keycode = e.getKeyCode();
        if (!((keycode == KeyEvent.VK_SHIFT || keycode == KeyEvent.VK_CONTROL ||
                keycode == KeyEvent.VK_ALT || keycode == KeyEvent.VK_META))) {
            keycode = (e.getKeyChar() & 0xFF);
        }
        inputReceived((byte) keycode);
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private void inputReceived(int input) {
        for (DeviceContext<Byte> device : devices) {
            device.writeData((byte) input);
        }
    }
}
