/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.jcip.annotations.ThreadSafe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A key event dispatcher that forwards keyboard events to registered listeners,
 * but only when the specified target window is the focused window.
 * <p>
 * This prevents the ZX Spectrum keyboard emulation from consuming key events
 * (including ESC) in other windows such as plugin dialogs or file choosers.
 */
@ThreadSafe
public class KeyboardDispatcher implements AutoCloseable, KeyEventDispatcher {
    private final List<OnKeyListener> onKeyListeners = new CopyOnWriteArrayList<>();
    private final FocusCheck focusCheck;

    public interface OnKeyListener {

        boolean onKeyEvent(KeyEvent e);
    }

    /**
     * Determines whether key events should be intercepted by this dispatcher.
     */
    @FunctionalInterface
    interface FocusCheck {
        boolean isTargetFocused();
    }

    /**
     * Creates a dispatcher that intercepts key events only when the given window is focused.
     *
     * @param targetWindow the window whose key events should be intercepted; must not be null
     */
    public KeyboardDispatcher(Window targetWindow) {
        Objects.requireNonNull(targetWindow);
        this.focusCheck = () -> {
            Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (focusedWindow == targetWindow) {
                return true;
            }
            // Also check if the focused window is a child of the target window (e.g. popup menus)
            return focusedWindow != null && SwingUtilities.isDescendingFrom(focusedWindow, targetWindow);
        };
    }

    /**
     * Package-private constructor for testing — accepts a custom focus check.
     */
    KeyboardDispatcher(FocusCheck focusCheck) {
        this.focusCheck = Objects.requireNonNull(focusCheck);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.isConsumed()) {
            return false;
        }

        // Only intercept events when the target window (DisplayWindow) is the focused window.
        // Without this check, ALL key events across the entire application would be consumed
        // by the emulated keyboard, preventing ESC from closing other dialogs, file choosers, etc.
        if (!focusCheck.isTargetFocused()) {
            return false;
        }

        boolean consumed = false;
        for (OnKeyListener listener : onKeyListeners) {
            consumed |= listener.onKeyEvent(e);
        }
        if (consumed) {
            e.consume();
        }
        return consumed;
    }

    @Override
    public void close() {
        onKeyListeners.clear();
    }

    public void addOnKeyListener(OnKeyListener onKeyListener) {
        onKeyListeners.add(Objects.requireNonNull(onKeyListener));
    }
}
