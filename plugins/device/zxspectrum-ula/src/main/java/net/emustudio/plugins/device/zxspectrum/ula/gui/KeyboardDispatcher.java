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

import net.jcip.annotations.ThreadSafe;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
public class KeyboardDispatcher implements AutoCloseable, KeyEventDispatcher {
    private final List<OnKeyListener> onKeyListeners = new CopyOnWriteArrayList<>();

    public interface OnKeyListener {

        void onKeyEvent(KeyEvent e);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        boolean isConsumed = false;
        if (!e.isConsumed()) {
            onKeyListeners.forEach(c -> c.onKeyEvent(e));
            e.consume();
            isConsumed = true;
        }
        return isConsumed;
    }

    @Override
    public void close() {
        onKeyListeners.clear();
    }

    public void addOnKeyListener(OnKeyListener onKeyListener) {
        onKeyListeners.add(Objects.requireNonNull(onKeyListener));
    }
}
