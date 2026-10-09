/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.easymock.Capture;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.fail;

public class DeviceImplTest {
    @Test public void originalBoardUsesTwoPortsAndConfiguredRestartVector() throws Exception {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString("boardType", "88-4PIO")).andReturn("88-PIO");
        expect(settings.getInt("basePort", 4)).andReturn(4);
        expect(settings.getInt("interruptVector", 7)).andReturn(3);
        Context8080 cpu = createStrictMock(Context8080.class);
        Capture<Context8080.CpuPortDevice> ports = newCapture();
        expect(cpu.attachDevice(eq(4), capture(ports))).andReturn(true);
        expect(cpu.attachDevice(eq(5), anyObject())).andReturn(true);
        expect(cpu.isInterruptSupported()).andReturn(true);
        cpu.signalInterrupt(aryEq(new byte[]{(byte) 0xDF}));
        cpu.detachDevice(5); cpu.detachDevice(4);
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(settings, cpu, pool, api);
        DeviceImpl device = new DeviceImpl(0, api, settings);
        device.initialize();
        ports.getValue().write(4, (byte) 2);
        ((PioUnit) ports.getValue()).writeData((byte) 42);
        device.destroy();
        verify(settings, cpu);
    }

    @Test public void attachFailureNeverDetachesConflictingPortOrUnownedPorts() throws Exception {
        for (int conflict = 0; conflict < 8; conflict++) {
            Context8080 cpu = createStrictMock(Context8080.class);
            for (int port = 0; port < conflict; port++) {
                expect(cpu.attachDevice(eq(0xA0 + port), anyObject())).andReturn(true);
            }
            expect(cpu.attachDevice(eq(0xA0 + conflict), anyObject())).andReturn(false);
            for (int port = conflict - 1; port >= 0; port--) { cpu.detachDevice(0xA0 + port); }
            ContextPool pool = createNiceMock(ContextPool.class);
            expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
            ApplicationApi api = createNiceMock(ApplicationApi.class);
            expect(api.getContextPool()).andReturn(pool).anyTimes();
            replay(cpu, pool, api);
            DeviceImpl device = new DeviceImpl(0, api, PluginSettings.UNAVAILABLE);
            try { device.initialize(); fail("Conflict must fail initialization"); }
            catch (PluginInitializationException expected) { }
            device.destroy();
            verify(cpu);
        }
    }

    @Test public void destroyDetachesEachOwnedPortOnce() throws Exception {
        Context8080 cpu = createStrictMock(Context8080.class);
        for (int port = 0xA0; port <= 0xA7; port++) {
            expect(cpu.attachDevice(eq(port), anyObject())).andReturn(true);
        }
        for (int port = 0xA7; port >= 0xA0; port--) { cpu.detachDevice(port); }
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(cpu, pool, api);
        DeviceImpl device = new DeviceImpl(0, api, PluginSettings.UNAVAILABLE);
        device.initialize();
        device.destroy();
        device.destroy();
        verify(cpu);
    }
}
