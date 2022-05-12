package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
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

        memory.write(0, (RAMInstruction)createNiceMock(RAMInstruction.class));
        memory.write(0, (RAMInstruction)createNiceMock(RAMInstruction.class));

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

        memory.write(0, new RAMInstruction[] { createNiceMock(RAMInstruction.class), createNiceMock(RAMInstruction.class)}, 2);

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
