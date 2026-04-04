/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class SerializableOptionalTest {

    @Test
    public void testEmpty() {
        SerializableOptional<String> opt = SerializableOptional.empty();
        assertNull(opt.value);
        assertFalse(opt.opt().isPresent());
    }

    @Test
    public void testOfNullableWithValue() {
        SerializableOptional<String> opt = SerializableOptional.ofNullable("hello");
        assertEquals("hello", opt.value);
        assertTrue(opt.opt().isPresent());
        assertEquals("hello", opt.opt().get());
    }

    @Test
    public void testOfNullableWithNull() {
        SerializableOptional<String> opt = SerializableOptional.ofNullable(null);
        assertNull(opt.value);
        assertFalse(opt.opt().isPresent());
    }

    @Test
    public void testFromOptPresent() {
        SerializableOptional<Integer> opt = SerializableOptional.fromOpt(Optional.of(42));
        assertEquals(Integer.valueOf(42), opt.value);
        assertTrue(opt.opt().isPresent());
    }

    @Test
    public void testFromOptEmpty() {
        SerializableOptional<Integer> opt = SerializableOptional.fromOpt(Optional.empty());
        assertNull(opt.value);
        assertFalse(opt.opt().isPresent());
    }

    @Test
    public void testEqualsWithSameValues() {
        SerializableOptional<String> o1 = SerializableOptional.ofNullable("hello");
        SerializableOptional<String> o2 = SerializableOptional.ofNullable("hello");
        assertEquals(o1, o2);
    }

    @Test
    public void testEqualsWithDifferentValues() {
        SerializableOptional<String> o1 = SerializableOptional.ofNullable("hello");
        SerializableOptional<String> o2 = SerializableOptional.ofNullable("world");
        assertNotEquals(o1, o2);
    }

    @Test
    public void testEqualsBothEmpty() {
        SerializableOptional<String> o1 = SerializableOptional.empty();
        SerializableOptional<String> o2 = SerializableOptional.empty();
        assertEquals(o1, o2);
    }

    @Test
    public void testEqualsWithNull() {
        SerializableOptional<String> o1 = SerializableOptional.ofNullable("hello");
        assertNotEquals(o1, null);
    }

    @Test
    public void testEqualsWithSameObject() {
        SerializableOptional<String> o1 = SerializableOptional.ofNullable("hello");
        assertEquals(o1, o1);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        SerializableOptional<String> o1 = SerializableOptional.ofNullable("hello");
        assertNotEquals(o1, "hello");
    }

    @Test
    public void testHashCodeConsistent() {
        SerializableOptional<String> o1 = SerializableOptional.ofNullable("hello");
        SerializableOptional<String> o2 = SerializableOptional.ofNullable("hello");
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    public void testHashCodeEmpty() {
        SerializableOptional<String> opt = SerializableOptional.empty();
        assertEquals(0, opt.hashCode());
    }

    @Test
    public void testToStringWithValue() {
        SerializableOptional<String> opt = SerializableOptional.ofNullable("hello");
        assertEquals("hello", opt.toString());
    }

    @Test
    public void testToStringEmpty() {
        SerializableOptional<String> opt = SerializableOptional.empty();
        assertEquals("null", opt.toString());
    }
}

