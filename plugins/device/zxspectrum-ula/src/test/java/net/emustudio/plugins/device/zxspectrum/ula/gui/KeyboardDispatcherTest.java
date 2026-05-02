/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import org.junit.Test;

import javax.swing.JPanel;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class KeyboardDispatcherTest {
    private static final JPanel DUMMY = new JPanel();

    @Test
    public void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> new KeyboardDispatcher((Window) null));
        assertThrows(NullPointerException.class, () -> new KeyboardDispatcher((KeyboardDispatcher.FocusCheck) null));
        assertThrows(NullPointerException.class, () -> dispatcher(true).addOnKeyListener(null));
    }

    @Test
    public void dispatchesToAllListenersAndConsumesWhenHandled() {
        KeyboardDispatcher dispatcher = dispatcher(true);
        Listener first = new Listener(false);
        Listener second = new Listener(true);
        dispatcher.addOnKeyListener(first);
        dispatcher.addOnKeyListener(second);
        KeyEvent event = keyPressed(KeyEvent.VK_A);
        assertTrue(dispatcher.dispatchKeyEvent(event));
        assertEquals(1, first.calls);
        assertEquals(1, second.calls);
        assertTrue(event.isConsumed());
    }

    @Test
    public void returnsFalseWhenEventIsIgnored() {
        KeyboardDispatcher unhandled = dispatcher(true);
        Listener listener = new Listener(false);
        unhandled.addOnKeyListener(listener);
        assertFalse(unhandled.dispatchKeyEvent(keyPressed(KeyEvent.VK_B)));
        assertEquals(1, listener.calls);
        KeyboardDispatcher preConsumed = dispatcher(true);
        Listener skipped = new Listener(true);
        preConsumed.addOnKeyListener(skipped);
        KeyEvent event = keyPressed(KeyEvent.VK_C);
        event.consume();
        assertFalse(preConsumed.dispatchKeyEvent(event));
        assertEquals(0, skipped.calls);
        KeyboardDispatcher unfocused = dispatcher(false);
        Listener blocked = new Listener(true);
        unfocused.addOnKeyListener(blocked);
        assertFalse(unfocused.dispatchKeyEvent(keyPressed(KeyEvent.VK_D)));
        assertEquals(0, blocked.calls);
        assertFalse(dispatcher(true).dispatchKeyEvent(keyPressed(KeyEvent.VK_E)));
    }

    @Test
    public void respectsDynamicFocusChanges() {
        AtomicBoolean focused = new AtomicBoolean(false);
        KeyboardDispatcher dispatcher = new KeyboardDispatcher(focused::get);
        Listener listener = new Listener(true);
        dispatcher.addOnKeyListener(listener);
        assertFalse(dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_F)));
        focused.set(true);
        assertTrue(dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_F)));
        focused.set(false);
        assertFalse(dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_F)));
        assertEquals(1, listener.calls);
    }

    @Test
    public void closeClearsListenersButStillAllowsReuse() {
        KeyboardDispatcher dispatcher = dispatcher(true);
        Listener removed = new Listener(true);
        dispatcher.addOnKeyListener(removed);
        dispatcher.close();
        assertFalse(dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_G)));
        assertEquals(0, removed.calls);
        Listener addedLater = new Listener(true);
        dispatcher.addOnKeyListener(addedLater);
        assertTrue(dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_H)));
        assertEquals(1, addedLater.calls);
    }

    private static KeyboardDispatcher dispatcher(boolean focused) {
        return new KeyboardDispatcher(() -> focused);
    }

    private static KeyEvent keyPressed(int keyCode) {
        return new KeyEvent(DUMMY, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static final class Listener implements KeyboardDispatcher.OnKeyListener {
        private final boolean consume;
        private int calls;

        private Listener(boolean consume) {
            this.consume = consume;
        }

        @Override
        public boolean onKeyEvent(KeyEvent event) {
            calls++;
            return consume;
        }
    }
}
