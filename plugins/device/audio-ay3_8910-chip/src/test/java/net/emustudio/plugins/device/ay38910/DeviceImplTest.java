/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DeviceImplTest {

    @Test
    public void testInitializeUsesDirectCpuWhenAvailable() throws PluginInitializationException {
        Context8080 cpu = createMock(Context8080.class);
        expect(cpu.getCPUFrequency()).andReturn(3500).once();
        expect(cpu.attachDevice(eq(Ay38910Chip.DATA_PORT & 0xFF), anyObject(Context8080.CpuPortDevice.class))).andReturn(true).once();
        cpu.addPassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once();
        cpu.removePassedCyclesListener(anyObject(CPUContext.PassedCyclesListener.class));
        expectLastCall().once();
        cpu.detachDevice(Ay38910Chip.DATA_PORT & 0xFF);
        expectLastCall().once();

        ContextPool contextPool = createMock(ContextPool.class);
        expect(contextPool.getCPUContext(0, Context8080.class)).andReturn(cpu).once();

        ApplicationApi applicationApi = createMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getGUI()).andReturn(null).once();

        replay(cpu, contextPool, applicationApi);

        Ay38910Chip chip = Ay38910Chip.silent();
        DeviceImpl device = new DeviceImpl(0, applicationApi, PluginSettings.UNAVAILABLE, () -> chip);
        device.initialize();
        assertEquals(3_500_000, chip.getCpuClockHz());
        device.destroy();

        verify(cpu, contextPool, applicationApi);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeFallsBackToReflectiveDeviceContext() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        expect(contextPool.getCPUContext(0, Context8080.class))
                .andThrow(new ContextNotFoundException("CPU not connected"))
                .once();

        ReflectiveBusContext busContext = new ReflectiveBusContext();
        expect(contextPool.getDeviceContext(0, DeviceContext.class)).andReturn(busContext).once();

        ApplicationApi applicationApi = createMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getGUI()).andReturn(null).once();

        replay(contextPool, applicationApi);

        Ay38910Chip chip = Ay38910Chip.silent();
        DeviceImpl device = new DeviceImpl(0, applicationApi, PluginSettings.UNAVAILABLE, () -> chip);
        device.initialize();
        assertEquals(4_000_000, chip.getCpuClockHz());
        device.destroy();

        verify(contextPool, applicationApi);

        assertEquals(Ay38910Chip.DATA_PORT & 0xFF, busContext.attachedPort);
        assertEquals(1, busContext.addListenerCalls);
        assertEquals(1, busContext.removeListenerCalls);
    }

    @Test
    public void testVersionIsKnown() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE, Ay38910Chip::silent);
        assertNotEquals("(unknown)", device.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE, Ay38910Chip::silent);
        assertNotEquals("(unknown)", device.getCopyright());
    }

    @Test
    public void testDescriptionIsPresent() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE, Ay38910Chip::silent);
        assertNotNull(device.getDescription());
        assertTrue(!device.getDescription().isEmpty());
    }

    @Test
    public void testAutomationIsSupported() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE, Ay38910Chip::silent);
        assertTrue(device.isAutomationSupported());
    }

    @Test
    public void testGuiIsDisabledWithoutGuiRuntime() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE, Ay38910Chip::silent);
        assertTrue(!device.isGuiSupported());
    }

    public static class ReflectiveBusContext implements DeviceContext<Byte> {
        int attachedPort = -1;
        int addListenerCalls;
        int removeListenerCalls;

        public int getCPUFrequency() {
            return 4000;
        }

        public void attachDevice(int port, Context8080.CpuPortDevice device) {
            this.attachedPort = port;
        }

        public void addPassedCyclesListener(CPUContext.PassedCyclesListener listener) {
            addListenerCalls++;
        }

        public void removePassedCyclesListener(CPUContext.PassedCyclesListener listener) {
            removeListenerCalls++;
        }

        @Override
        public Byte readData() {
            return 0;
        }

        @Override
        public void writeData(Byte data) {
        }

        @Override
        public Class<Byte> getDataType() {
            return Byte.class;
        }
    }
}
