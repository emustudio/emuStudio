/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotEquals;

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
}
