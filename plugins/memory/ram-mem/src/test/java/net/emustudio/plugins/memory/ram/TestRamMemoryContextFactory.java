/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.memory.annotations.Annotations;

/**
 * Test helper to create MemoryContextImpl instances from tests
 * in subpackages that cannot access the protected constructor.
 */
public class TestRamMemoryContextFactory {
    public static MemoryContextImpl create() {
        return new MemoryContextImpl(new Annotations());
    }
}

