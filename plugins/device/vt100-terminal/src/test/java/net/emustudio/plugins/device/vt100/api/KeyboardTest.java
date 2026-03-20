/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class KeyboardTest {

    private static class TestKeyboard extends Keyboard {
        @Override
        public void process() {
            // no-op for testing
        }

        public void pressKey(byte key) {
            notifyOnKey(key);
        }
    }

    @Test
    public void testAddOnKeyHandlerReceivesKeys() {
        TestKeyboard keyboard = new TestKeyboard();
        List<Byte> received = new ArrayList<>();
        keyboard.addOnKeyHandler(received::add);

        keyboard.pressKey((byte) 'A');
        keyboard.pressKey((byte) 'B');

        assertEquals(2, received.size());
        assertEquals((byte) 'A', (byte) received.get(0));
        assertEquals((byte) 'B', (byte) received.get(1));
    }

    @Test
    public void testMultipleOnKeyHandlersAllReceiveKeys() {
        TestKeyboard keyboard = new TestKeyboard();
        List<Byte> handler1 = new ArrayList<>();
        List<Byte> handler2 = new ArrayList<>();
        keyboard.addOnKeyHandler(handler1::add);
        keyboard.addOnKeyHandler(handler2::add);

        keyboard.pressKey((byte) 'X');

        assertEquals(1, handler1.size());
        assertEquals(1, handler2.size());
        assertEquals((byte) 'X', (byte) handler1.get(0));
        assertEquals((byte) 'X', (byte) handler2.get(0));
    }

    @Test
    public void testAddInputRequestHandler() {
        TestKeyboard keyboard = new TestKeyboard();
        AtomicReference<Boolean> requested = new AtomicReference<>();
        keyboard.addInputRequestHandler(requested::set);

        keyboard.inputRequested(true);
        assertTrue(requested.get());

        keyboard.inputRequested(false);
        assertFalse(requested.get());
    }

    @Test
    public void testMultipleInputRequestHandlers() {
        TestKeyboard keyboard = new TestKeyboard();
        List<Boolean> values1 = new ArrayList<>();
        List<Boolean> values2 = new ArrayList<>();
        keyboard.addInputRequestHandler(values1::add);
        keyboard.addInputRequestHandler(values2::add);

        keyboard.inputRequested(true);

        assertEquals(1, values1.size());
        assertEquals(1, values2.size());
        assertTrue(values1.get(0));
        assertTrue(values2.get(0));
    }

    @Test
    public void testCloseRemovesOnKeyHandlers() {
        TestKeyboard keyboard = new TestKeyboard();
        AtomicBoolean called = new AtomicBoolean(false);
        keyboard.addOnKeyHandler(b -> called.set(true));

        keyboard.close();
        keyboard.pressKey((byte) 'A');

        assertFalse("Handler should not be called after close", called.get());
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullOnKeyHandlerThrows() {
        TestKeyboard keyboard = new TestKeyboard();
        keyboard.addOnKeyHandler(null);
    }

    @Test
    public void testNoKeyHandlersDoesNotThrow() {
        TestKeyboard keyboard = new TestKeyboard();
        keyboard.pressKey((byte) 0x42); // should not throw
    }

    @Test
    public void testInputRequestedWithNoHandlers() {
        TestKeyboard keyboard = new TestKeyboard();
        keyboard.inputRequested(true); // should not throw
    }

    @Test
    public void testKeyValues() {
        TestKeyboard keyboard = new TestKeyboard();
        List<Byte> received = new ArrayList<>();
        keyboard.addOnKeyHandler(received::add);

        keyboard.pressKey((byte) 0x00);
        keyboard.pressKey((byte) 0x7F);
        keyboard.pressKey((byte) 0xFF);

        assertEquals(3, received.size());
        assertEquals((byte) 0x00, (byte) received.get(0));
        assertEquals((byte) 0x7F, (byte) received.get(1));
        assertEquals((byte) 0xFF, (byte) received.get(2));
    }
}

