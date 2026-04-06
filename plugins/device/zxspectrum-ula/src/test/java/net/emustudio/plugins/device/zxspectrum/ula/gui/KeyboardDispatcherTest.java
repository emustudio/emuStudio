/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class KeyboardDispatcherTest {
    private static final JPanel DUMMY = new JPanel();

    private KeyboardDispatcher focused() {
        return new KeyboardDispatcher(() -> true);
    }

    private KeyboardDispatcher unfocused() {
        return new KeyboardDispatcher(() -> false);
    }

    // --- addOnKeyListener ---

    @Test(expected = NullPointerException.class)
    public void testAddNullListenerThrowsNpe() {
        KeyboardDispatcher dispatcher = focused();
        dispatcher.addOnKeyListener(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullTargetWindowThrowsNpe() {
        new KeyboardDispatcher((Window) null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullFocusCheckThrowsNpe() {
        new KeyboardDispatcher((KeyboardDispatcher.FocusCheck) null);
    }

    // --- dispatchKeyEvent (with focused target window) ---

    @Test
    public void testEventIsForwardedToSingleListener() {
        KeyboardDispatcher dispatcher = focused();
        RecordingListener listener = new RecordingListener(true);
        dispatcher.addOnKeyListener(listener);

        KeyEvent event = keyPressed(KeyEvent.VK_A);
        dispatcher.dispatchKeyEvent(event);

        assertEquals(1, listener.events.size());
        assertTrue(event.isConsumed());
    }

    @Test
    public void testEventIsForwardedToMultipleListeners() {
        KeyboardDispatcher dispatcher = focused();
        RecordingListener first = new RecordingListener(false);
        RecordingListener second = new RecordingListener(true);
        dispatcher.addOnKeyListener(first);
        dispatcher.addOnKeyListener(second);

        KeyEvent event = keyPressed(KeyEvent.VK_B);
        dispatcher.dispatchKeyEvent(event);

        assertEquals(1, first.events.size());
        assertEquals(1, second.events.size());
        // second consumed it
        assertTrue(event.isConsumed());
    }

    @Test
    public void testEventNotConsumedWhenNoListenerHandlesIt() {
        KeyboardDispatcher dispatcher = focused();
        RecordingListener listener = new RecordingListener(false);
        dispatcher.addOnKeyListener(listener);

        KeyEvent event = keyPressed(KeyEvent.VK_C);
        boolean dispatched = dispatcher.dispatchKeyEvent(event);

        assertFalse("Should return false when no listener consumed", dispatched);
        assertFalse("Event should not be consumed", event.isConsumed());
    }

    @Test
    public void testAlreadyConsumedEventIsNotDispatched() {
        KeyboardDispatcher dispatcher = focused();
        RecordingListener listener = new RecordingListener(true);
        dispatcher.addOnKeyListener(listener);

        KeyEvent event = keyPressed(KeyEvent.VK_D);
        event.consume(); // pre-consume
        dispatcher.dispatchKeyEvent(event);

        assertEquals("Already consumed event should not be dispatched", 0, listener.events.size());
    }

    @Test
    public void testReturnsTrueWhenConsumed() {
        KeyboardDispatcher dispatcher = focused();
        dispatcher.addOnKeyListener(new RecordingListener(true));

        boolean result = dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_E));
        assertTrue(result);
    }

    @Test
    public void testReturnsFalseWhenNotConsumed() {
        KeyboardDispatcher dispatcher = focused();
        dispatcher.addOnKeyListener(new RecordingListener(false));

        boolean result = dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_F));
        assertFalse(result);
    }

    @Test
    public void testNoListenersDoesNotThrow() {
        KeyboardDispatcher dispatcher = focused();

        boolean result = dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_G));
        assertFalse(result);
    }

    // --- Window-scoped dispatch ---

    @Test
    public void testEventNotDispatchedWhenTargetNotFocused() {
        KeyboardDispatcher dispatcher = unfocused();
        RecordingListener listener = new RecordingListener(true);
        dispatcher.addOnKeyListener(listener);

        KeyEvent event = keyPressed(KeyEvent.VK_ESCAPE);
        boolean result = dispatcher.dispatchKeyEvent(event);

        assertFalse("Should not dispatch when target is not focused", result);
        assertFalse("Event should not be consumed", event.isConsumed());
        assertEquals("Listener should not receive events", 0, listener.events.size());
    }

    @Test
    public void testFocusChangeIsRespectedDynamically() {
        AtomicBoolean focused = new AtomicBoolean(false);
        KeyboardDispatcher dispatcher = new KeyboardDispatcher(focused::get);
        RecordingListener listener = new RecordingListener(true);
        dispatcher.addOnKeyListener(listener);

        // Not focused — event should not be dispatched
        KeyEvent event1 = keyPressed(KeyEvent.VK_A);
        assertFalse(dispatcher.dispatchKeyEvent(event1));
        assertEquals(0, listener.events.size());

        // Now focused — event should be dispatched
        focused.set(true);
        KeyEvent event2 = keyPressed(KeyEvent.VK_A);
        assertTrue(dispatcher.dispatchKeyEvent(event2));
        assertEquals(1, listener.events.size());

        // Unfocused again — event should not be dispatched
        focused.set(false);
        KeyEvent event3 = keyPressed(KeyEvent.VK_A);
        assertFalse(dispatcher.dispatchKeyEvent(event3));
        assertEquals(1, listener.events.size());
    }

    // --- close ---

    @Test
    public void testCloseRemovesAllListeners() {
        KeyboardDispatcher dispatcher = focused();
        RecordingListener listener = new RecordingListener(true);
        dispatcher.addOnKeyListener(listener);

        dispatcher.close();

        dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_H));
        assertEquals("Listener should not receive events after close", 0, listener.events.size());
    }

    @Test
    public void testListenerCanBeAddedAfterClose() {
        KeyboardDispatcher dispatcher = focused();
        dispatcher.close();

        RecordingListener listener = new RecordingListener(true);
        dispatcher.addOnKeyListener(listener);

        dispatcher.dispatchKeyEvent(keyPressed(KeyEvent.VK_I));
        assertEquals(1, listener.events.size());
    }

    // --- Helpers ---

    private static KeyEvent keyPressed(int keyCode) {
        return new KeyEvent(DUMMY, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static final class RecordingListener implements KeyboardDispatcher.OnKeyListener {
        private final boolean consume;
        private final List<KeyEvent> events = new ArrayList<>();

        RecordingListener(boolean consume) {
            this.consume = consume;
        }

        @Override
        public boolean onKeyEvent(KeyEvent e) {
            events.add(e);
            return consume;
        }
    }
}
