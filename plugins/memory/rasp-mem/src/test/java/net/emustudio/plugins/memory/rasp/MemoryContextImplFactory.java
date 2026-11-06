/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp;



/**
 * Test helper to create MemoryContextImpl instances from other test packages.
 * Needed because MemoryContextImpl constructor has protected access.
 */
public class MemoryContextImplFactory {

    public static MemoryContextImpl create() {
        return new MemoryContextImpl();
    }
}

