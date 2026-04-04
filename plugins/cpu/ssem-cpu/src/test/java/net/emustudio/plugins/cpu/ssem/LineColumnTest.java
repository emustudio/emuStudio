/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import org.junit.Test;

import static org.junit.Assert.*;

public class LineColumnTest {

    @Test
    public void testGetClassType() {
        LineColumn column = new LineColumn();
        assertEquals(String.class, column.getClassType());
    }

    @Test
    public void testGetTitle() {
        LineColumn column = new LineColumn();
        assertEquals("line", column.getTitle());
    }

    @Test
    public void testIsEditable() {
        LineColumn column = new LineColumn();
        assertFalse(column.isEditable());
    }

    @Test
    public void testGetValueAtLine0() {
        LineColumn column = new LineColumn();
        assertEquals("0000", column.getValue(0));
    }

    @Test
    public void testGetValueAtLine1() {
        LineColumn column = new LineColumn();
        assertEquals("0001", column.getValue(4));
    }

    @Test
    public void testGetValueAtLine10() {
        LineColumn column = new LineColumn();
        assertEquals("000A", column.getValue(40));
    }

    @Test
    public void testSetValueDoesNotThrow() {
        LineColumn column = new LineColumn();
        column.setValue(0, "anything"); // no-op, should not throw
    }
}

