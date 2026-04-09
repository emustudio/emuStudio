/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemoryContextImplTest {

    private MemoryContextImpl memory;

    @Before
    public void setUp() {
        this.memory = new MemoryContextImpl(new Annotations());
    }

    @Test
    public void testMemoryNotificationsAreEnabledByDefault() {
        assertTrue(memory.areMemoryNotificationsEnabled());
    }

    @Test
    public void testNotifyMemoryChangesOnWrite() {
        AtomicInteger memoryChanges = new AtomicInteger();
        AtomicInteger memorySizeChanges = new AtomicInteger();

        memory.addMemoryListener(new MemoryContext.MemoryListener() {
            @Override
            public void memoryContentChanged(int from, int to) {
                memoryChanges.incrementAndGet();
            }

            @Override
            public void memorySizeChanged() {
                memorySizeChanges.incrementAndGet();
            }
        });

        memory.write(0, (RamInstruction) createNiceMock(RamInstruction.class));
        memory.write(0, (RamInstruction) createNiceMock(RamInstruction.class));

        assertEquals(1, memorySizeChanges.get());
        assertEquals(2, memoryChanges.get());
    }

    @Test
    public void testNotifyMemoryChangesOnWrite2() {
        AtomicInteger memoryChanges = new AtomicInteger();
        AtomicInteger memorySizeChanges = new AtomicInteger();

        memory.addMemoryListener(new MemoryContext.MemoryListener() {
            @Override
            public void memoryContentChanged(int from, int to) {
                memoryChanges.incrementAndGet();
            }

            @Override
            public void memorySizeChanged() {
                memorySizeChanges.incrementAndGet();
            }
        });

        memory.write(0, new RamInstruction[]{createNiceMock(RamInstruction.class), createNiceMock(RamInstruction.class)}, 2);

        assertEquals(1, memorySizeChanges.get());
        assertEquals(2, memoryChanges.get());
    }

    @Test
    public void testMemoryChangesAreNotNotifiedOnRead() {
        AtomicInteger memoryChanges = new AtomicInteger();
        AtomicInteger memorySizeChanges = new AtomicInteger();

        memory.addMemoryListener(new MemoryContext.MemoryListener() {
            @Override
            public void memoryContentChanged(int from, int to) {
                memoryChanges.incrementAndGet();
            }

            @Override
            public void memorySizeChanged() {
                memorySizeChanges.incrementAndGet();
            }
        });

        memory.read(0);

        assertEquals(0, memorySizeChanges.get());
        assertEquals(0, memoryChanges.get());
    }
}
