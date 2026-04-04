/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamValue;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class ValueTest {

    @Test
    public void testNumberValue() {
        Value value = new Value(42);
        assertEquals(RamValue.Type.NUMBER, value.getType());
        assertEquals(42, value.getNumberValue());
        assertNull(value.getStringValue());
    }

    @Test
    public void testStringValue() {
        Value value = new Value("hello", false);
        assertEquals(RamValue.Type.STRING, value.getType());
        assertEquals("hello", value.getStringValue());
        assertEquals(0, value.getNumberValue());
    }

    @Test
    public void testIdValue() {
        Value value = new Value("myLabel", true);
        assertEquals(RamValue.Type.ID, value.getType());
        assertEquals("myLabel", value.getStringValue());
    }

    @Test
    public void testGetStringRepresentationNumber() {
        Value value = new Value(42);
        assertEquals("42", value.getStringRepresentation());
    }

    @Test
    public void testGetStringRepresentationString() {
        Value value = new Value("hello", false);
        assertEquals("hello", value.getStringRepresentation());
    }

    @Test
    public void testGetStringRepresentationId() {
        Value value = new Value("myLabel", true);
        assertEquals("myLabel", value.getStringRepresentation());
    }

    @Test
    public void testToStringNumber() {
        Value value = new Value(42);
        assertEquals("42", value.toString());
    }

    @Test
    public void testToStringString() {
        Value value = new Value("hello", false);
        assertEquals("hello", value.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        Value v1 = new Value(42);
        Value v2 = new Value(42);
        Value v3 = new Value(43);

        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
        assertNotEquals(v1, v3);
    }

    @Test
    public void testEqualsWithDifferentTypes() {
        Value number = new Value(0);
        Value string = new Value("0", false);
        assertNotEquals(number, string);
    }

    @Test
    public void testEqualsWithNull() {
        Value value = new Value(42);
        assertNotEquals(value, null);
    }

    @Test
    public void testEqualsWithSameObject() {
        Value value = new Value(42);
        assertEquals(value, value);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Value value = new Value(42);
        assertNotEquals(value, "42");
    }

    @Test
    public void testHashCodeConsistentWithStringValues() {
        Value v1 = new Value("test", false);
        Value v2 = new Value("test", false);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    public void testHashCodeNullString() {
        Value v1 = new Value(42);
        // should not throw
        v1.hashCode();
    }
}

