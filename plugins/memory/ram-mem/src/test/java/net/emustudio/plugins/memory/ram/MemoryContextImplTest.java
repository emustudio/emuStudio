/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.memory.Memory;
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
        this.memory = new MemoryContextImpl();
    }

    @Test
    public void testMemoryNotificationsAreEnabledByDefault() {
        assertTrue(memory.areMemoryNotificationsEnabled());
    }

    @Test
    public void testNotifyMemoryChangesOnWrite() {
        AtomicInteger memoryChanges = new AtomicInteger();
        AtomicInteger memorySizeChanges = new AtomicInteger();

        memory.addMemoryListener(new Memory.MemoryListener() {
            @Override
            public void memoryChanged(int memoryPosition) {
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

        memory.addMemoryListener(new Memory.MemoryListener() {
            @Override
            public void memoryChanged(int memoryPosition) {
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

        memory.addMemoryListener(new Memory.MemoryListener() {
            @Override
            public void memoryChanged(int memoryPosition) {
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
