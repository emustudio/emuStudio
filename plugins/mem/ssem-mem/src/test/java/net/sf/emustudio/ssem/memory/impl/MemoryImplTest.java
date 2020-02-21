/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.memory.impl;

import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryImplTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceWithNullContextPoolThrows() {
        new MemoryImpl(0L, null);
    }

    @Test
    public void testMemoryContextIsRegisteredInContextPool() throws Exception {
        ContextPool contextPool = createMock(ContextPool.class);
        contextPool.register(eq(0L), anyObject(), same(MemoryContext.class));
        expectLastCall().once();
        replay(contextPool);

        new MemoryImpl(0L, contextPool);

        verify(contextPool);
    }

    @Test
    public void testGetSizeReturnsNumberOfCells() {
        MemoryImpl memory = new MemoryImpl(0L, createNiceMock(ContextPool.class));

        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, memory.getSize());
    }

    @Test
    public void testIsShowSettingsSupportedReturnsTrueByDefault() {
        MemoryImpl memory = new MemoryImpl(0L, createNiceMock(ContextPool.class));

        assertTrue(memory.isShowSettingsSupported());
    }

    @Test
    public void testGetVersionDoesNotReturnNull() {
        MemoryImpl memory = new MemoryImpl(0L, createNiceMock(ContextPool.class));

        assertNotNull(memory.getVersion());
    }
}
