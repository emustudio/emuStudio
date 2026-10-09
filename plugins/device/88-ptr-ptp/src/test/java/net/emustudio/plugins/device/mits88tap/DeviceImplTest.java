/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DeviceImplTest {
    @Test public void noGuiModeNeverReadsStartupPreferenceOrRequestsUiServices() throws Exception {
        checkUiSupport(true);
    }

    @Test public void defaultStartupLeavesGuiClosedAndEnablesSettings() throws Exception {
        checkUiSupport(false);
    }

    private void checkUiSupport(boolean noGui) throws Exception {
        PluginSettings settings = createStrictMock(PluginSettings.class);
        expect(settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(noGui);
        if (!noGui) {
            expect(settings.getBoolean("showGuiAtStartup", false)).andReturn(false);
        }
        Context8080 cpu = createStrictMock(Context8080.class);
        expect(cpu.attachDevice(eq(0x12), anyObject())).andReturn(true);
        expect(cpu.attachDevice(eq(0x13), anyObject())).andReturn(true);
        cpu.detachDevice(0x12);
        cpu.detachDevice(0x13);
        ContextPool pool = createStrictMock(ContextPool.class);
        pool.register(eq(0L), anyObject(PaperTapeContext.class), eq(PaperTapeContext.class));
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        ApplicationApi api = createStrictMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).times(2);
        replay(settings, cpu, pool, api);
        DeviceImpl device = new DeviceImpl(0, api, settings);
        device.initialize();
        assertEquals(!noGui, device.isGuiSupported());
        assertEquals(!noGui, device.isShowSettingsSupported());
        if (noGui) {
            device.showGUI(null);
            device.showSettings(null);
        }
        device.destroy();
        verify(settings, cpu, pool, api);
    }
}
