/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.plugins.memory.ram.api.RamMemoryContext;

/**
 * Test helper to create RamTableModel instances from tests
 * in subpackages that cannot access the package-private constructor.
 */
public class TestRamTableModelFactory {
    public static RamTableModel create(RamMemoryContext context) {
        return new RamTableModel(context);
    }
}

