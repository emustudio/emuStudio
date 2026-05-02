/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DeviceImplTest {

    @Test(expected = PluginInitializationException.class)
    public void testInitializeFailsWhenCpuContextIsUnavailable() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        expect(contextPool.getCPUContext(0, net.emustudio.plugins.cpu.intel8080.api.Context8080.class))
                .andThrow(new net.emustudio.emulib.runtime.ContextNotFoundException("CPU not connected"))
                .once();

        ApplicationApi applicationApi = createMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();

        replay(contextPool, applicationApi);

        DeviceImpl device = new DeviceImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        try {
            device.initialize();
        } finally {
            verify(contextPool, applicationApi);
        }
    }

    @Test
    public void testGuiIsEnabledByDefault() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE);
        assertTrue(device.isGuiSupported());
    }

    @Test
    public void testVersionIsKnown() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE);
        assertNotEquals("(unknown)", device.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE);
        assertNotEquals("(unknown)", device.getCopyright());
    }

    @Test
    public void testDescriptionIsPresent() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE);
        assertNotNull(device.getDescription());
        assertTrue(!device.getDescription().isEmpty());
    }

    @Test
    public void testAutomationIsSupported() {
        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), PluginSettings.UNAVAILABLE);
        assertTrue(device.isAutomationSupported());
    }

    @Test
    public void testGuiIsDisabledWhenNoGuiSettingIsSet() {
        PluginSettings settings = createMock(PluginSettings.class);
        expect(settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true).once();
        replay(settings);

        DeviceImpl device = new DeviceImpl(0, createNiceMock(ApplicationApi.class), settings);
        assertTrue(!device.isGuiSupported());

        verify(settings);
    }
}
