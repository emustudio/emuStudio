/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.memory.ssem.impl;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import net.emustudio.plugins.memory.ssem.MemoryImpl;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryImplTest {
    private MemoryImpl memory;

    @Before
    public void setup() {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        memory = new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
    }


    @Test
    public void testMemoryContextIsRegisteredInContextPool() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().once();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        new MemoryImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);

        verify(contextPool);
    }

    @Test
    public void testGetSizeReturnsNumberOfCells() {
        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, memory.getSize());
    }

    @Test
    public void testIsShowSettingsSupportedReturnsTrueByDefault() {
        assertTrue(memory.isShowSettingsSupported());
    }

    @Test
    public void testGetVersionDoesNotReturnNull() {
        assertNotNull(memory.getVersion());
    }
}
