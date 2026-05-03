/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.device.DeviceContext;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class BrainCPUContextImplTest {

    @Test
    public void testWriteToDeviceWithAttachedDevice() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        DeviceContext<Byte> device = createMock(DeviceContext.class);
        device.writeData((byte) 42);
        expectLastCall().once();
        replay(device);

        context.attachDevice(device);
        context.writeToDevice((byte) 42);
        verify(device);
    }

    @Test
    public void testWriteToDeviceWithNoDevice() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        context.writeToDevice((byte) 42); // should not throw
    }

    @Test
    public void testReadFromDeviceWithAttachedDevice() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        DeviceContext<Byte> device = createMock(DeviceContext.class);
        expect(device.readData()).andReturn((byte) 42).once();
        replay(device);

        context.attachDevice(device);
        assertEquals(42, context.readFromDevice());
        verify(device);
    }

    @Test
    public void testReadFromDeviceWithNoDevice() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        assertEquals(0, context.readFromDevice());
    }

    @Test
    public void testReadFromDeviceReturnsNull() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        DeviceContext<Byte> device = createMock(DeviceContext.class);
        expect(device.readData()).andReturn(null).once();
        replay(device);

        context.attachDevice(device);
        assertEquals(0, context.readFromDevice());
        verify(device);
    }

    @Test
    public void testDetachDevice() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        DeviceContext<Byte> device = createMock(DeviceContext.class);
        replay(device);

        context.attachDevice(device);
        context.detachDevice();
        // after detach, read should return 0
        assertEquals(0, context.readFromDevice());
    }

    @Test
    public void testPassedCyclesSupported() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        assertFalse(context.isPassedCyclesSupported());
    }

    @Test
    public void testAddRemovePassedCyclesListener() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        // should not throw
        context.addPassedCyclesListener(cycles -> {});
        context.removePassedCyclesListener(cycles -> {});
    }

    @Test
    public void testWriteAfterDetach() {
        BrainCPUContextImpl context = new BrainCPUContextImpl();
        DeviceContext<Byte> device = createMock(DeviceContext.class);
        replay(device);

        context.attachDevice(device);
        context.detachDevice();
        context.writeToDevice((byte) 42); // should not throw
    }
}
