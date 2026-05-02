/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

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

    @Test
    public void testBusExposesConfiguredCpuFrequency() {
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
        expect(cpu.getCPUFrequency()).andReturn(3500).once();

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);

        replay(cpu, memory);

        env.cpu = cpu;
        env.memory = memory;

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        assertEquals(3500, bus.getCPUFrequency());

        verify(env.cpu, env.memory);
    }

    // ========== Memory contention tests ==========

    @Test
    public void testReadContendedMemoryAddsCycles() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // Move to first contended cycle (delay = 6)
        bus.passedCycles(FIRST_CONTENDED);

        bus.read(0x4000);

        assertEquals(6L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testReadNonContendedMemoryDoesNotAddCycles() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);

        // Below 0x4000 - not contended
        bus.read(0x3FFF);
        // At or above 0x8000 - not contended
        bus.read(0x8000);

        assertEquals(0L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testReadContendedMemoryNoCyclesOutsideContentionWindow() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // At cycle 0 (top border) - no contention
        bus.read(0x4000);

        assertEquals(0L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testReadArrayContendedMemoryAddsCycles() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);

        bus.read(0x5000, 4);

        assertEquals(6L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testWriteContendedMemoryAddsCycles() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);

        bus.write(0x4000, (byte) 0x42);

        assertEquals(6L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testWriteArrayContendedMemoryAddsCycles() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);

        bus.write(0x6000, new Byte[]{0x01, 0x02}, 2);

        assertEquals(6L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testWriteNonContendedMemoryDoesNotAddCycles() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);

        bus.write(0x0000, (byte) 0x42);
        bus.write(0xC000, (byte) 0x42);

        assertEquals(0L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testMemoryContentionDelayDecreasesAcrossPattern() {
        // Pattern: 6,5,4,3,2,1,0,0 repeating every 8 cycles
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // First contended cycle -> delay 6
        bus.passedCycles(FIRST_CONTENDED);
        bus.read(0x4000);
        assertEquals(6L, env.addedCycles.get());

        env.addedCycles.set(0);
        bus.passedCycles(1); // FIRST_CONTENDED+1 -> delay 5
        bus.read(0x4000);
        assertEquals(5L, env.addedCycles.get());

        env.addedCycles.set(0);
        bus.passedCycles(1); // FIRST_CONTENDED+2 -> delay 4
        bus.read(0x4000);
        assertEquals(4L, env.addedCycles.get());

        env.addedCycles.set(0);
        bus.passedCycles(1); // FIRST_CONTENDED+3 -> delay 3
        bus.read(0x4000);
        assertEquals(3L, env.addedCycles.get());

        env.addedCycles.set(0);
        bus.passedCycles(1); // FIRST_CONTENDED+4 -> delay 2
        bus.read(0x4000);
        assertEquals(2L, env.addedCycles.get());

        env.addedCycles.set(0);
        bus.passedCycles(1); // FIRST_CONTENDED+5 -> delay 1
        bus.read(0x4000);
        assertEquals(1L, env.addedCycles.get());

        // FIRST_CONTENDED+6 and +7 have no delay
        env.addedCycles.set(0);
        bus.passedCycles(1);
        bus.read(0x4000);
        assertEquals(0L, env.addedCycles.get());

        env.addedCycles.set(0);
        bus.passedCycles(1);
        bus.read(0x4000);
        assertEquals(0L, env.addedCycles.get());

        // Next group starts at FIRST_CONTENDED+8 -> delay 6 again
        env.addedCycles.set(0);
        bus.passedCycles(1);
        bus.read(0x4000);
        assertEquals(6L, env.addedCycles.get());

        verify(env.cpu, env.memory);
    }

    // ========== Non-contended memory access tests ==========

    @Test
    public void testReadMemoryNotContendedBypassesContention() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        env.memoryValues.put(0x4000, (byte) 0xAB);
        bus.passedCycles(FIRST_CONTENDED);

        byte value = bus.readMemoryNotContended(0x4000);

        assertEquals((byte) 0xAB, value);
        assertEquals(0L, env.addedCycles.get()); // No contention cycles added
        verify(env.cpu, env.memory);
    }

    @Test
    public void testWriteMemoryNotContendedBypassesContention() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);

        bus.writeMemoryNotContended(0x4000, (byte) 0xCD);

        assertEquals(0L, env.addedCycles.get()); // No contention cycles added
        verify(env.cpu, env.memory);
    }

    // ========== Bus data tests ==========

    @Test
    public void testReadWriteBusData() {
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();

        bus.writeData((byte) 0x42);
        assertEquals((byte) 0x42, (byte) bus.readData());

        bus.writeData((byte) 0x00);
        assertEquals((byte) 0x00, (byte) bus.readData());

        bus.writeData((byte) 0xFF);
        assertEquals((byte) 0xFF, (byte) bus.readData());
    }

    // ========== Interrupt delegation tests ==========

    @Test
    public void testSignalNonMaskableInterruptDelegatesToCpu() {
        ContextZ80 cpu = createStrictMock(ContextZ80.class);
        cpu.setInterruptDuration(32);
        expectLastCall().once();
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).times(256);
        cpu.addPassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once();
        cpu.signalNonMaskableInterrupt();
        expectLastCall().once();

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        replay(cpu, memory);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);
        bus.signalNonMaskableInterrupt();

        verify(cpu, memory);
    }

    @Test
    public void testSignalInterruptDelegatesToCpu() {
        byte[] interruptData = new byte[]{(byte) 0xFF};
        ContextZ80 cpu = createStrictMock(ContextZ80.class);
        cpu.setInterruptDuration(32);
        expectLastCall().once();
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).times(256);
        cpu.addPassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once();
        cpu.signalInterrupt(aryEq(interruptData));
        expectLastCall().once();

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        replay(cpu, memory);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);
        bus.signalInterrupt(interruptData);

        verify(cpu, memory);
    }

    @Test
    public void testClearInterruptDelegatesToCpu() {
        ContextZ80 cpu = createStrictMock(ContextZ80.class);
        cpu.setInterruptDuration(32);
        expectLastCall().once();
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).times(256);
        cpu.addPassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once();
        cpu.clearInterrupt();
        expectLastCall().once();

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        replay(cpu, memory);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);
        bus.clearInterrupt();

        verify(cpu, memory);
    }

    // ========== Port contention pattern tests ==========

    @Test
    public void testContendedPortEvenLowBitPattern() {
        // High byte in 0x40-0x7F, low bit reset: C:1, C:3
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);
        // Port 0x40FE: high byte 0x40 (contended), low bit 0 (even)
        env.cpuPortDispatchers.get(0xFE).read(0x40FE);

        // C:1 at FIRST_CONTENDED = 6, then C:3 at FIRST_CONTENDED+1 = 5 => total 11
        assertEquals(11L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testContendedPortOddLowBitPattern() {
        // High byte in 0x40-0x7F, low bit set: C:1, C:1, C:1, C:1
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);
        // Port 0x40FF: high byte 0x40 (contended), low bit 1 (odd)
        env.cpuPortDispatchers.get(0xFF).read(0x40FF);

        // C:1 at 14335=6, at 14336=5, at 14337=4, at 14338=3 => total 18
        assertEquals(18L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testNonContendedPortEvenLowBitPattern() {
        // High byte NOT in 0x40-0x7F, low bit reset: N:1, C:3
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);
        // Port 0x00FE: high byte 0x00 (non-contended), low bit 0 (even)
        env.cpuPortDispatchers.get(0xFE).read(0x00FE);

        // N:1 (no contention), then C:3 at FIRST_CONTENDED+1 = 5 => total 5
        assertEquals(5L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testNonContendedPortOddLowBitNoContention() {
        // High byte NOT in 0x40-0x7F, low bit set: N:4 (no contention at all)
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);
        // Port 0x00FF: high byte 0x00 (non-contended), low bit 1 (odd)
        env.cpuPortDispatchers.get(0xFF).read(0x00FF);

        // No contention cycles added (just floating bus read, but no contention)
        assertEquals(0L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    // ========== Port write tests ==========

    @Test
    public void testPortWriteDispatchesToAttachedDevice() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();

        AtomicBoolean written = new AtomicBoolean(false);
        Context8080.CpuPortDevice device = new Context8080.CpuPortDevice() {
            @Override
            public byte read(int portAddress) {
                return 0;
            }

            @Override
            public void write(int portAddress, byte data) {
                assertEquals(0x00FE, portAddress);
                assertEquals((byte) 0x18, data);
                written.set(true);
            }

            @Override
            public String getName() {
                return "TestDevice";
            }
        };
        bus.attachDevice(0xFE, device);
        bus.initialize(env.cpu, env.memory);

        env.cpuPortDispatchers.get(0xFE).write(0x00FE, (byte) 0x18);

        assertTrue("Device write should have been called", written.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testPortWriteToUnattachedPortDoesNotThrow() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // Write to port 0xAB which has no device - should not throw
        env.cpuPortDispatchers.get(0xAB).write(0x00AB, (byte) 0x42);

        verify(env.cpu, env.memory);
    }

    @Test
    public void testReadFromUnattachedPortOutsideFloatingBusReturnsFF() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // At cycle 0 (in top border), floating bus should return 0xFF
        byte value = env.cpuPortDispatchers.get(0xFF).read(0x00FF);

        assertEquals((byte) 0xFF, value);
        verify(env.cpu, env.memory);
    }

    // ========== Device attachment tests ==========

    @Test(expected = NullPointerException.class)
    public void testAttachNullDeviceThrows() {
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.attachDevice(0xFE, null);
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicateDeferredAttachmentThrows() {
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        Context8080.CpuPortDevice device1 = createDevice("Device1");
        Context8080.CpuPortDevice device2 = createDevice("Device2");

        bus.attachDevice(0xFE, device1);
        bus.attachDevice(0xFE, device2); // Should throw
    }

    @Test(expected = RuntimeException.class)
    public void testDuplicatePostInitAttachmentThrows() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        Context8080.CpuPortDevice device1 = createDevice("Device1");
        Context8080.CpuPortDevice device2 = createDevice("Device2");

        bus.attachDevice(0xFE, device1);
        bus.attachDevice(0xFE, device2); // Should throw
    }

    @Test
    public void testAttachDeviceAfterInitialization() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        Context8080.CpuPortDevice device = new Context8080.CpuPortDevice() {
            @Override
            public byte read(int portAddress) {
                return (byte) 0xBE;
            }

            @Override
            public void write(int portAddress, byte data) {
            }

            @Override
            public String getName() {
                return "LateDevice";
            }
        };
        bus.attachDevice(0xAA, device);

        assertEquals((byte) 0xBE, env.cpuPortDispatchers.get(0xAA).read(0x00AA));
        verify(env.cpu, env.memory);
    }

    @Test
    public void testAttachDeviceMasksPortTo8Bits() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();

        Context8080.CpuPortDevice device = new Context8080.CpuPortDevice() {
            @Override
            public byte read(int portAddress) {
                return (byte) 0x77;
            }

            @Override
            public void write(int portAddress, byte data) {
            }

            @Override
            public String getName() {
                return "MaskedDevice";
            }
        };
        // Port 0x1FE should be masked to 0xFE
        bus.attachDevice(0x1FE, device);
        bus.initialize(env.cpu, env.memory);

        assertEquals((byte) 0x77, env.cpuPortDispatchers.get(0xFE).read(0x00FE));
        verify(env.cpu, env.memory);
    }

    @Test
    public void testAddPassedCyclesListenerBeforeInit() {
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        CPUContext.PassedCyclesListener listener = (cycles) -> {};

        bus.addPassedCyclesListener(listener);
        // initialize will call cpu.addPassedCyclesListener for the deferred one + for bus itself
        TestEnvironment env = newTestEnvironmentWithMultipleListeners(2);
        bus.initialize(env.cpu, env.memory);

        verify(env.cpu, env.memory);
    }

    @Test
    public void testRemovePassedCyclesListenerBeforeInit() {
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        CPUContext.PassedCyclesListener listener = (cycles) -> {};

        bus.addPassedCyclesListener(listener);
        bus.removePassedCyclesListener(listener);

        // After removal, only bus's own listener should be deferred
        TestEnvironment env = newTestEnvironment();
        bus.initialize(env.cpu, env.memory);

        verify(env.cpu, env.memory);
    }

    @Test
    public void testAddPassedCyclesListenerAfterInit() {
        CPUContext.PassedCyclesListener listener = (cycles) -> {};

        ContextZ80 cpu = createStrictMock(ContextZ80.class);
        cpu.setInterruptDuration(32);
        expectLastCall().once();
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).times(256);
        cpu.addPassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once(); // bus itself
        cpu.addPassedCyclesListener(listener);
        expectLastCall().once(); // our listener

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        replay(cpu, memory);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);
        bus.addPassedCyclesListener(listener);

        verify(cpu, memory);
    }

    @Test
    public void testRemovePassedCyclesListenerAfterInit() {
        CPUContext.PassedCyclesListener listener = (cycles) -> {};

        ContextZ80 cpu = createStrictMock(ContextZ80.class);
        cpu.setInterruptDuration(32);
        expectLastCall().once();
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).times(256);
        cpu.addPassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once();
        cpu.removePassedCyclesListener(listener);
        expectLastCall().once();

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        replay(cpu, memory);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);
        bus.removePassedCyclesListener(listener);

        verify(cpu, memory);
    }

    // ========== Proxy method tests ==========

    @Test
    public void testGetCellTypeClassReturnsByte() {
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        assertEquals(Byte.class, bus.getCellTypeClass());
    }

    @Test
    public void testGetDataTypeReturnsByte() {
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        assertEquals(Byte.class, bus.getDataType());
    }

    @Test
    public void testClearDelegatesToMemory() {
        ContextZ80 cpu = createNiceMock(ContextZ80.class);
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).anyTimes();

        MemoryContext<Byte> memory = createStrictMock(MemoryContext.class);
        memory.clear();
        expectLastCall().once();
        replay(cpu, memory);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);
        bus.clear();

        verify(memory);
    }

    @Test
    public void testGetSizeDelegatesToMemory() {
        ContextZ80 cpu = createNiceMock(ContextZ80.class);
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).anyTimes();

        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        expect(memory.getSize()).andReturn(65536).once();
        replay(cpu, memory);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);

        assertEquals(65536, bus.getSize());
        verify(memory);
    }

    @Test
    public void testAnnotationsDelegatesToMemory() {
        ContextZ80 cpu = createNiceMock(ContextZ80.class);
        expect(cpu.attachDevice(anyInt(), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).anyTimes();

        MemoryContextAnnotations mockAnnotations = createMock(MemoryContextAnnotations.class);
        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        expect(memory.annotations()).andReturn(mockAnnotations).once();
        replay(cpu, memory, mockAnnotations);

        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(cpu, memory);

        assertSame(mockAnnotations, bus.annotations());
        verify(memory);
    }

    // ========== Frame cycle management tests ==========

    @Test
    public void testPassedCyclesWrapsAroundFrameTstates() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // Advance one full frame + enough to reach FIRST_CONTENDED
        bus.passedCycles(DISPLAY_FRAME_TSTATES + FIRST_CONTENDED);

        // Should be equivalent to being at FIRST_CONTENDED in a new frame
        bus.read(0x4000);
        assertEquals(6L, env.addedCycles.get());

        verify(env.cpu, env.memory);
    }

    @Test
    public void testPassedCyclesMultipleFrames() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // Advance several full frames
        bus.passedCycles(3L * DISPLAY_FRAME_TSTATES + FIRST_CONTENDED);

        bus.read(0x4000);
        assertEquals(6L, env.addedCycles.get());

        verify(env.cpu, env.memory);
    }

    // ========== Floating bus edge-case tests ==========

    @Test
    public void testFloatingBusReturnsFFBeforeScreenArea() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // Position before FIRST_FLOATING_BUS (in top border)
        bus.passedCycles(100);
        byte value = env.cpuPortDispatchers.get(0xFF).read(0x00FF);
        assertEquals((byte) 0xFF, value);

        verify(env.cpu, env.memory);
    }

    @Test
    public void testFloatingBusReturnsFFAfterScreenArea() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // Position after all 192 screen lines
        long afterScreen = FIRST_FLOATING_BUS + (long) SCREEN_HEIGHT_PIXELS * DISPLAY_LINE_TSTATES;
        bus.passedCycles(afterScreen - 3); // offset by IO_READ_SAMPLE_OFFSET
        byte value = env.cpuPortDispatchers.get(0xFF).read(0x00FF);
        assertEquals((byte) 0xFF, value);

        verify(env.cpu, env.memory);
    }

    @Test
    public void testFloatingBusReturnsFFDuringHorizontalRetrace() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        // Position at horizontal retrace part of first screen line (cycle 128+ within line)
        // SCREEN_FETCH_CYCLES = 128, so at offset 128 within a line, floating bus returns 0xFF
        long retracePos = FIRST_FLOATING_BUS + 128; // past fetch cycles in first line
        bus.passedCycles(retracePos - 3); // offset by IO_READ_SAMPLE_OFFSET
        byte value = env.cpuPortDispatchers.get(0xFF).read(0x00FF);
        assertEquals((byte) 0xFF, value);

        verify(env.cpu, env.memory);
    }

    // ========== Port dispatcher name test ==========

    @Test
    public void testPortDispatcherHasName() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        Context8080.CpuPortDevice dispatcher = env.cpuPortDispatchers.get(0x00);
        assertNotNull(dispatcher.getName());
        assertFalse(dispatcher.getName().isEmpty());

        verify(env.cpu, env.memory);
    }

    // ========== Port contention with write ==========

    @Test
    public void testContendedPortWriteAddsCycles() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);
        // Contended port write: high byte 0x40 (contended), low bit 0 (even) -> C:1, C:3
        env.cpuPortDispatchers.get(0xFE).write(0x40FE, (byte) 0x00);

        assertEquals(11L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    @Test
    public void testNonContendedPortWriteOddLowBitNoContention() {
        TestEnvironment env = newTestEnvironment();
        ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();
        bus.initialize(env.cpu, env.memory);

        bus.passedCycles(FIRST_CONTENDED);
        // Non-contended port, odd low bit: N:4 - no contention
        env.cpuPortDispatchers.get(0xFF).write(0x00FF, (byte) 0x00);

        assertEquals(0L, env.addedCycles.get());
        verify(env.cpu, env.memory);
    }

    // ========== Helper methods ==========

    private Context8080.CpuPortDevice createDevice(String name) {
        return new Context8080.CpuPortDevice() {
            @Override
            public byte read(int portAddress) {
                return 0;
            }

            @Override
            public void write(int portAddress, byte data) {
            }

            @Override
            public String getName() {
                return name;
            }
        };
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

    private TestEnvironment newTestEnvironmentWithMultipleListeners(int expectedListenerCalls) {
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
        expectLastCall().times(expectedListenerCalls);
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
