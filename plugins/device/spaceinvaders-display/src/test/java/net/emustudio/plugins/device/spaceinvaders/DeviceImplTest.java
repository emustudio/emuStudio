/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertThrows;

public class DeviceImplTest {
    @SuppressWarnings("unchecked")
    private DeviceImpl device(Context8080 cpu) throws Exception {
        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Byte.class).anyTimes();
        expect(memory.getSize()).andReturn(65536).anyTimes();
        replay(memory);
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memory);
        replay(pool);
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(api);
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true);
        expect(settings.getString("soundSamplesDirectory", "examples/space-invaders/sounds"))
                .andReturn("examples/space-invaders/sounds");
        replay(settings);
        return new DeviceImpl(0, api, settings);
    }

    @Test
    public void attachesAndReleasesBothSoundPorts() throws Exception {
        Context8080 cpu = createMock(Context8080.class);
        for (int port = 1; port <= 5; port++) {
            expect(cpu.attachDevice(eq(port), anyObject())).andReturn(true);
            cpu.detachDevice(port);
        }
        expect(cpu.isInterruptSupported()).andReturn(false).anyTimes();
        replay(cpu);
        DeviceImpl device = device(cpu);
        try {
            device.initialize();
            device.reset();
        } finally {
            device.destroy();
        }
        device.destroy();
        verify(cpu);
    }

    @Test
    public void portFiveConflictRollsBackWithoutDetachingOtherDevice() throws Exception {
        Context8080 cpu = createMock(Context8080.class);
        for (int port = 1; port <= 4; port++) {
            expect(cpu.attachDevice(eq(port), anyObject())).andReturn(true);
            cpu.detachDevice(port);
        }
        expect(cpu.attachDevice(eq(5), anyObject())).andReturn(false);
        replay(cpu);
        DeviceImpl device = device(cpu);
        assertThrows(PluginInitializationException.class, device::initialize);
        device.destroy();
        verify(cpu);
    }
}
