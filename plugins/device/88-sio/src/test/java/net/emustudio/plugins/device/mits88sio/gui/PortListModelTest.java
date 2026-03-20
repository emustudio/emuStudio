/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio.gui;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PortListModelTest {
    private PortListModel model;

    @Before
    public void setup() {
        model = new PortListModel();
    }

    @Test
    public void testInitialSizeIsZero() {
        assertEquals(0, model.getSize());
    }

    @Test
    public void testInitialGetAllIsEmpty() {
        assertTrue(model.getAll().isEmpty());
    }

    @Test
    public void testAddPort() {
        assertTrue(model.add(0x10));
        assertEquals(1, model.getSize());
        assertTrue(model.contains(0x10));
    }

    @Test
    public void testAddDuplicateReturnsFalse() {
        model.add(0x10);
        assertFalse(model.add(0x10));
        assertEquals(1, model.getSize());
    }

    @Test
    public void testAddMultiplePorts() {
        model.add(0x10);
        model.add(0x11);
        model.add(0x14);
        assertEquals(3, model.getSize());
    }

    @Test
    public void testAddAll() {
        model.addAll(List.of(0x10, 0x11, 0x14));
        assertEquals(3, model.getSize());
        assertTrue(model.contains(0x10));
        assertTrue(model.contains(0x11));
        assertTrue(model.contains(0x14));
    }

    @Test
    public void testSetAll() {
        model.add(0x10);
        model.add(0x11);

        model.setAll(List.of(0x20, 0x21));
        assertEquals(2, model.getSize());
        assertFalse(model.contains(0x10));
        assertFalse(model.contains(0x11));
        assertTrue(model.contains(0x20));
        assertTrue(model.contains(0x21));
    }

    @Test
    public void testGetAll() {
        model.add(0x10);
        model.add(0x11);

        List<Integer> all = model.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(0x10));
        assertTrue(all.contains(0x11));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAllReturnsUnmodifiableList() {
        model.add(0x10);
        model.getAll().add(0x11);
    }

    @Test
    public void testClear() {
        model.add(0x10);
        model.add(0x11);

        model.clear();
        assertEquals(0, model.getSize());
        assertTrue(model.getAll().isEmpty());
    }

    @Test
    public void testRemoveAt() {
        model.add(0x10);
        model.add(0x11);
        model.add(0x14);

        model.removeAt(1); // removes 0x11
        assertEquals(2, model.getSize());
        assertTrue(model.contains(0x10));
        assertFalse(model.contains(0x11));
        assertTrue(model.contains(0x14));
    }

    @Test
    public void testRemoveAtFirstElement() {
        model.add(0x10);
        model.add(0x11);

        model.removeAt(0);
        assertEquals(1, model.getSize());
        assertFalse(model.contains(0x10));
        assertTrue(model.contains(0x11));
    }

    @Test
    public void testRemoveAtLastElement() {
        model.add(0x10);
        model.add(0x11);

        model.removeAt(1);
        assertEquals(1, model.getSize());
        assertTrue(model.contains(0x10));
        assertFalse(model.contains(0x11));
    }

    @Test
    public void testContainsReturnsFalseForMissingPort() {
        assertFalse(model.contains(0x10));
    }

    @Test
    public void testGetElementAtFormatsHex() {
        model.add(0x10);
        assertEquals("0x10", model.getElementAt(0));
    }

    @Test
    public void testGetElementAtFormatsHexMultipleDigits() {
        model.add(0xFF);
        assertEquals("0xff", model.getElementAt(0));
    }

    @Test
    public void testGetElementAtZeroPort() {
        model.add(0);
        assertEquals("0x0", model.getElementAt(0));
    }

    @Test
    public void testGetElementAtPreservesOrder() {
        model.add(0x10);
        model.add(0x20);
        model.add(0x30);
        assertEquals("0x10", model.getElementAt(0));
        assertEquals("0x20", model.getElementAt(1));
        assertEquals("0x30", model.getElementAt(2));
    }

    @Test
    public void testSetAllClearsPreviousThenAdds() {
        model.add(0x10);
        model.setAll(List.of(0x20));
        assertEquals(1, model.getSize());
        assertEquals("0x20", model.getElementAt(0));
    }

    @Test
    public void testAddAllDoesNotPreventDuplicates() {
        // addAll does not check for duplicates (unlike add)
        model.add(0x10);
        model.addAll(List.of(0x10, 0x11));
        // addAll does not filter duplicates
        assertEquals(3, model.getSize());
    }

    @Test
    public void testClearOnEmptyModelDoesNotFail() {
        model.clear();
        assertEquals(0, model.getSize());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetElementAtInvalidIndex() {
        model.getElementAt(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveAtInvalidIndex() {
        model.removeAt(0);
    }
}

