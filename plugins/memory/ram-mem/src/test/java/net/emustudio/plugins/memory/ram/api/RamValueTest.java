/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RamValueTest {

    @Test
    public void testTypeValues() {
        assertEquals(3, RamValue.Type.values().length);
        assertEquals(RamValue.Type.NUMBER, RamValue.Type.valueOf("NUMBER"));
        assertEquals(RamValue.Type.STRING, RamValue.Type.valueOf("STRING"));
        assertEquals(RamValue.Type.ID, RamValue.Type.valueOf("ID"));
    }
}

