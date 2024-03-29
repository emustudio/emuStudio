/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotEquals;

public class CpuImplTest {
    private CpuImpl cpu;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws PluginInitializationException {
        ContextPool contextPool = createNiceMock(ContextPool.class);
        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Byte.class).anyTimes();
        replay(memory);
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        replay(contextPool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        this.cpu = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
        this.cpu.initialize();
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", cpu.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", cpu.getCopyright());
    }
}
