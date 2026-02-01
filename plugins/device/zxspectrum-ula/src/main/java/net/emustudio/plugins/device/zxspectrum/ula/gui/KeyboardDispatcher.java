/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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

        boolean onKeyEvent(KeyEvent e);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        boolean isConsumed = false;
        if (!e.isConsumed()) {
            boolean consumed = false;
            for (OnKeyListener listener : onKeyListeners) {
                consumed |= listener.onKeyEvent(e);
            }
            if (consumed) {
                e.consume();
                isConsumed = true;
            }
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
