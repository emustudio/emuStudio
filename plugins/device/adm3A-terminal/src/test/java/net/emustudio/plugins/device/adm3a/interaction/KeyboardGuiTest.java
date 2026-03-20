/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.interaction;

import org.junit.Before;
import org.junit.Test;

import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_COLUMNS;
import static net.emustudio.plugins.device.adm3a.DeviceImpl.DEFAULT_ROWS;
import static org.junit.Assert.*;

import java.awt.*;

public class KeyboardGuiTest {
    private KeyboardGui keyboard;

    @Before
    public void setUp() {
        Cursor cursor = new Cursor(DEFAULT_COLUMNS, DEFAULT_ROWS);
        keyboard = new KeyboardGui(cursor);
    }

    @Test
    public void testKeyPressedNotifiesHandler() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_A, 'a');
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 'a', received.get().byteValue());
    }

    @Test
    public void testControlKeyCombo() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        // Ctrl+A should produce code 1
        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_A, 'a');
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 1, received.get().byteValue());
    }

    @Test
    public void testControlZProduces26() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_Z, 'z');
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 26, received.get().byteValue());
    }

    @Test
    public void testEnterKeyProducesCarriageReturn() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_ENTER, '\n');
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 13, received.get().byteValue());
    }

    @Test
    public void testArrowDownProducesLineFeed() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 10, received.get().byteValue());
    }

    @Test
    public void testArrowUpProducesVT() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 11, received.get().byteValue());
    }

    @Test
    public void testArrowRightProducesFF() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 12, received.get().byteValue());
    }

    @Test
    public void testArrowLeftProducesBackspace() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 8, received.get().byteValue());
    }

    @Test
    public void testKeyTypedDoesNothing() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(),
                0, KeyEvent.VK_UNDEFINED, 'a');
        keyboard.keyTyped(event);

        assertNull(received.get());
    }

    @Test
    public void testKeyReleasedDoesNothing() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(),
                0, KeyEvent.VK_A, 'a');
        keyboard.keyReleased(event);

        assertNull(received.get());
    }

    @Test
    public void testHighCharIsIgnored() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        // keyChar >= 0xFF should be ignored
        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_UNDEFINED, (char) 0xFF);
        keyboard.keyPressed(event);

        assertNull(received.get());
    }

    @Test
    public void testProcessDoesNothing() {
        // process() is a no-op for GUI keyboard
        keyboard.process();
    }

    @Test
    public void testMultipleHandlers() {
        AtomicInteger count = new AtomicInteger(0);
        keyboard.addOnKeyHandler(b -> count.incrementAndGet());
        keyboard.addOnKeyHandler(b -> count.incrementAndGet());

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_A, 'a');
        keyboard.keyPressed(event);

        assertEquals(2, count.get());
    }

    @Test
    public void testCloseRemovesHandlers() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        keyboard.close();

        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, KeyEvent.VK_A, 'a');
        keyboard.keyPressed(event);

        assertNull(received.get());
    }

    @Test
    public void testControlLowercaseMapped() {
        // Ctrl+lowercase letter should produce the same code as Ctrl+uppercase
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        // Ctrl+b (lowercase 'b' = 98)
        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, 'b', 'b');
        keyboard.keyPressed(event);

        assertNotNull(received.get());
        assertEquals((byte) 2, received.get().byteValue());
    }

    @Test
    public void testControlBracketProducesEscapeConsumedByLoadCursorPosition() {
        AtomicReference<Byte> received = new AtomicReference<>();
        keyboard.addOnKeyHandler(received::set);

        // Ctrl+[ produces ESC (27), which is consumed by LoadCursorPosition
        // (it starts the cursor positioning sequence), so it's NOT forwarded to handlers
        KeyEvent event = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                KeyEvent.CTRL_DOWN_MASK, '[', '[');
        keyboard.keyPressed(event);

        assertNull(received.get());
    }
}

