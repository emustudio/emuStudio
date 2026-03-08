/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.DISPLAY_LINE_TSTATES;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ZxSpectrumBusImplTest {
    private static final long FIRST_CONTENDED = 14335;
    private static final long FIRST_FLOATING_BUS = 14338;

    @Test
    public void testAllCpuPortsAreDispatchedAndDeferredDeviceIsRouted() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();

        Context8080.CpuPortDevice ula = new Context8080.CpuPortDevice() {
            @Override
            public byte read(int portAddress) {
                return (byte) 0x5A;
            }

            @Override
            public void write(int portAddress, byte data) {
                // no-op
            }

            @Override
            public String getName() {
                return "ULA";
            }
        };
        bus.attachDevice(0xFE, ula);
        bus.initialize(env.cpu, env.memory);

        assertEquals(256, env.cpuPortDispatchers.size());
        Context8080.CpuPortDevice portDispatcher = env.cpuPortDispatchers.get(0xFE);
        assertNotNull(portDispatcher);
        assertEquals((byte) 0x5A, portDispatcher.read(0x7FFE));

        verify(env.cpu, env.memory);
    }

    @Test
    public void testContendedInAddsCyclesEvenOnUnattachedPorts() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);
        byte value = env.cpuPortDispatchers.get(0xFF).read(0x40FF);

        assertEquals((byte) 0xFF, value);
        assertEquals(18L, env.addedCycles.get());

        verify(env.cpu, env.memory);
    }

    @Test
    public void testFloatingBusReturnsScreenAndAttributeBytes() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        env.memoryValues.put(0x4000, (byte) 0x12);
        env.memoryValues.put(0x5800, (byte) 0x34);
        env.memoryValues.put(0x4001, (byte) 0x56);
        env.memoryValues.put(0x5801, (byte) 0x78);
        env.memoryValues.put(0x4100, (byte) 0x9A);

        Context8080.CpuPortDevice portDispatcher = env.cpuPortDispatchers.get(0xFF);
        // Port reads sample floating bus after three non-contented I/O T-states.
        bus.passedCycles(FIRST_FLOATING_BUS - 3);
        assertEquals((byte) 0x12, portDispatcher.read(0x00FF));
        bus.passedCycles(1);
        assertEquals((byte) 0x34, portDispatcher.read(0x00FF));
        bus.passedCycles(1);
        assertEquals((byte) 0x56, portDispatcher.read(0x00FF));
        bus.passedCycles(1);
        assertEquals((byte) 0x78, portDispatcher.read(0x00FF));
        bus.passedCycles(1);
        assertEquals((byte) 0xFF, portDispatcher.read(0x00FF));

        // On the next raster line, the attribute byte is the same for the first eight pixel lines.
        bus.passedCycles(DISPLAY_LINE_TSTATES - 4);
        assertEquals((byte) 0x9A, portDispatcher.read(0x00FF));
        bus.passedCycles(1);
        assertEquals((byte) 0x34, portDispatcher.read(0x00FF));

        verify(env.cpu, env.memory);
    }

    private TestEnvironment newTestEnvironment() {
        TestEnvironment env = new TestEnvironment();

        ContextZ80 cpu = createStrictMock(ContextZ80.class);
        cpu.setInterruptDuration(32);
        expectLastCall().once();
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andAnswer(() -> {
            Object[] args = getCurrentArguments();
            env.cpuPortDispatchers.put((Integer) args[0], (Context8080.CpuPortDevice) args[1]);
            return true;
        }).times(256);
        cpu.addPassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once();
        cpu.addCycles(anyLong());
        expectLastCall().andAnswer(() -> {
            env.addedCycles.addAndGet((Long) getCurrentArguments()[0]);
            return null;
        }).anyTimes();

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        expect(memory.read(anyInt())).andAnswer(() -> {
            int address = ((Integer) getCurrentArguments()[0]) & 0xFFFF;
            Byte value = env.memoryValues.get(address);
            return value != null ? value : (byte) 0xFF;
        }).anyTimes();

        replay(cpu, memory);
        env.cpu = cpu;
        env.memory = memory;
        return env;
    }

    private static class TestEnvironment {
        private final Map<Integer, Context8080.CpuPortDevice> cpuPortDispatchers = new HashMap<>();
        private final Map<Integer, Byte> memoryValues = new HashMap<>();
        private final AtomicLong addedCycles = new AtomicLong();

        private ContextZ80 cpu;
        private MemoryContext<Byte> memory;
    }
}
