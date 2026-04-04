/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp.ast;

import org.junit.Test;

import static org.junit.Assert.*;

public class LabelTest {

    @Test
    public void testGetLabel() {
        Label label = new Label(1, 0, "test", 5);
        assertEquals("TEST", label.getLabel());
    }

    @Test
    public void testGetAddress() {
        Label label = new Label(1, 0, "test", 5);
        assertEquals(5, label.getAddress());
    }

    @Test
    public void testEqualsAndHashCode() {
        Label l1 = new Label(1, 0, "test", 5);
        Label l2 = new Label(2, 3, "test", 5);
        assertEquals(l1, l2);
        assertEquals(l1.hashCode(), l2.hashCode());
    }

    @Test
    public void testNotEqualsDifferentLabel() {
        Label l1 = new Label(1, 0, "test1", 5);
        Label l2 = new Label(1, 0, "test2", 5);
        assertNotEquals(l1, l2);
    }

    @Test
    public void testNotEqualsDifferentAddress() {
        Label l1 = new Label(1, 0, "test", 5);
        Label l2 = new Label(1, 0, "test", 6);
        assertNotEquals(l1, l2);
    }

    @Test
    public void testEqualsWithNull() {
        Label label = new Label(1, 0, "test", 5);
        assertNotEquals(label, null);
    }

    @Test
    public void testEqualsWithSameObject() {
        Label label = new Label(1, 0, "test", 5);
        assertEquals(label, label);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Label label = new Label(1, 0, "test", 5);
        assertNotEquals(label, "test");
    }

    @Test
    public void testToString() {
        Label label = new Label(1, 0, "test", 5);
        String str = label.toString();
        assertTrue(str.contains("TEST"));
        assertTrue(str.contains("5"));
    }

    @Test
    public void testLabelIsUpperCased() {
        Label label = new Label(1, 0, "myLabel", 0);
        assertEquals("MYLABEL", label.getLabel());
    }

    @Test
    public void testLineAndColumn() {
        Label label = new Label(3, 7, "test", 5);
        assertEquals(3, label.line);
        assertEquals(7, label.column);
    }
}

