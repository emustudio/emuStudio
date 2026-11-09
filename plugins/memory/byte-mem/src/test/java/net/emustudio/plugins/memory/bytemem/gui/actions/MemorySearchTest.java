/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class MemorySearchTest {
    private MemoryContextImpl memory;
    private AtomicInteger selectedAddress;
    private AtomicReference<String> status;
    private MemorySearch search;

    @Before
    public void setUp() {
        memory = TestMemoryContextFactory.create(256, 1, 0);
        selectedAddress = new AtomicInteger(-1);
        status = new AtomicReference<>();
        search = new MemorySearch(new MemoryTableModel(memory), selectedAddress::set, status::set);
    }

    @Test
    public void navigatesMatchesInBothDirectionsAndWraps() {
        memory.write(5, (byte) 0x42);
        memory.write(10, (byte) 0x42);

        search.start(new byte[]{0x42}, 6);
        assertEquals(10, selectedAddress.get());
        assertEquals("Match 2 of 2", status.get());

        search.next();
        assertEquals(5, selectedAddress.get());
        assertEquals("Match 1 of 2", status.get());

        search.previous();
        assertEquals(10, selectedAddress.get());
    }

    @Test
    public void reportsMissingMatches() {
        search.start(new byte[]{0x42}, 0);
        assertEquals(-1, selectedAddress.get());
        assertEquals("No more matches", status.get());

        search.next();
        assertEquals("No more matches", status.get());
    }
}
