/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.abstracttape;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AbstractTapeTest {
    private AbstractTape device;

    @Before
    public void setup() {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        expect(applicationApi.getDialogs()).andReturn(createNiceMock(Dialogs.class)).anyTimes();
        replay(applicationApi);

        this.device = new AbstractTape(0, applicationApi, PluginSettings.UNAVAILABLE);
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
    public void testGetDescription() {
        assertNotNull(device.getDescription());
        assertFalse(device.getDescription().isEmpty());
    }

    @Test
    public void testIsAutomationSupported() {
        assertTrue(device.isAutomationSupported());
    }

    @Test
    public void testDefaultTitleIsAbstractTape() {
        assertEquals("Abstract tape", device.getTitle());
    }

    @Test
    public void testSetGUITitle() {
        device.setGUITitle("Custom Title");
        assertEquals("Custom Title", device.getTitle());
    }

    @Test(expected = NullPointerException.class)
    public void testSetGUITitleNullThrows() {
        device.setGUITitle(null);
    }

    @Test
    public void testGuiNotSupportedWithUnavailableSettings() {
        // PluginSettings.UNAVAILABLE returns false for EMUSTUDIO_NO_GUI by default (it returns false)
        // But the actual behavior depends on getBoolean default
        // The key point is isGuiSupported reflects the settings
        // With UNAVAILABLE settings, getBoolean returns default value (false for NO_GUI), so GUI is supported
        assertTrue(device.isGuiSupported());
    }

    @Test
    public void testShowSettingsSupported() {
        // Should match isGuiSupported
        assertEquals(device.isGuiSupported(), device.isShowSettingsSupported());
    }

    @Test
    public void testDestroyCanBeCalledMultipleTimes() {
        device.destroy();
        device.destroy(); // should not throw
    }

    @Test
    public void testResetDoesNotThrow() {
        device.reset();
    }
}
