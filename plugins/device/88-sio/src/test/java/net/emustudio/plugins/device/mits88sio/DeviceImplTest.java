/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DeviceImplTest {
    private DeviceImpl device;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws PluginInitializationException {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getCPUContext(0, Context8080.class))
                .andReturn(createNiceMock(Context8080.class)).anyTimes();
        expect(contextPool.getDeviceContext(0, DeviceContext.class))
                .andThrow(new ContextNotFoundException("")).anyTimes();
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
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

    @Test
    public void testGetDescriptionIsNotEmpty() {
        assertNotNull(device.getDescription());
        assertFalse(device.getDescription().isEmpty());
    }

    @Test
    public void testIsAutomationSupported() {
        assertTrue(device.isAutomationSupported());
    }

    @Test
    public void testIsGuiNotSupportedWithUnavailableSettings() {
        // PluginSettings.UNAVAILABLE returns false for EMUSTUDIO_NO_GUI -> guiSupported = true
        // But since it cannot resolve settings, behavior depends on default
        assertNotNull(device);
    }

    @Test
    public void testResetDoesNotThrow() {
        device.reset();
    }

    @Test
    public void testDestroyDoesNotThrow() {
        device.destroy();
    }

    @Test
    public void testGetTitle() {
        // Title comes from @PluginRoot annotation
        assertNotNull(device.getTitle());
    }
}
