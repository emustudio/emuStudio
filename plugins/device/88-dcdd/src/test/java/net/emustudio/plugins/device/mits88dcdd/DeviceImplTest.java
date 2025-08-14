/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotEquals;

public class DeviceImplTest {
    private DeviceImpl device;

    @Before
    public void setup() throws PluginInitializationException {
        Context8080 cpu = mock(Context8080.class);
        expect(cpu.attachDevice(anyInt(), anyObject())).andReturn(true).anyTimes();
        cpu.detachDevice(anyInt());
        expectLastCall().anyTimes();
        replay(cpu);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getCPUContext(0, Context8080.class)).andReturn(cpu).once();
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(mock(Dialogs.class)).once();
        replay(applicationApi);

        this.device = new DeviceImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        device.initialize();
    }

    @After
    public void tearDown() {
        device.destroy();
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", device.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", device.getCopyright());
    }
}
