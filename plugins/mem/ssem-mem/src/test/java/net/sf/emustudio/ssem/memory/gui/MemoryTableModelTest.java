/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.ssem.memory.gui;

import emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MemoryTableModelTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceWithNullModelThrows() throws Exception {
        new MemoryTableModel(null);
    }

    @Test
    public void testEditableAreOnlyValueColumns() throws Exception {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        assertTrue(model.isCellEditable(0, MemoryTableModel.COLUMN_BINARY_VALUE));
        assertTrue(model.isCellEditable(0, MemoryTableModel.COLUMN_HEX_VALUE));

        assertFalse(model.isCellEditable(
            0, MemoryTableModel.COLUMN_BINARY_VALUE + MemoryTableModel.COLUMN_HEX_VALUE
        ));
    }

    @Test
    public void testClearCallsClearOnMemoryMock() throws Exception {
        MemoryContext<Integer> memoryContext = createMock(MemoryContext.class);

        memoryContext.clear();
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.clear();

        verify(memoryContext);
    }

    @Test
    public void testSetBinaryValueCallsMemoryWrite() throws Exception {
        MemoryContext<Integer> memoryContext = createMock(MemoryContext.class);

        memoryContext.write(10, 3);
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("11", 10, MemoryTableModel.COLUMN_BINARY_VALUE);

        verify(memoryContext);
    }

    @Test
    public void testSetHexValueCallsMemoryWrite() throws Exception {
        MemoryContext<Integer> memoryContext = createMock(MemoryContext.class);

        memoryContext.write(10, 0xFF);
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("FF", 10, MemoryTableModel.COLUMN_HEX_VALUE);

        verify(memoryContext);
    }

    @Test
    public void testSetValueAtInvalidIndexDoesNotThrow() throws Exception {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        model.setValueAt("10", -1, MemoryTableModel.COLUMN_BINARY_VALUE);
    }

    @Test
    public void testSetNullValueDoesNotThrow() throws Exception {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        model.setValueAt(null, 0, MemoryTableModel.COLUMN_BINARY_VALUE);
    }

    @Test
    public void testGetValueCallsMemoryRead() throws Exception {
        MemoryContext<Integer> memoryContext = createMock(MemoryContext.class);

        expect(memoryContext.read(10)).andReturn(1).anyTimes();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);

        assertEquals("0001", model.getValueAt(10, MemoryTableModel.COLUMN_HEX_VALUE));
        assertEquals("00000000000000000000000000000001", model.getValueAt(10, MemoryTableModel.COLUMN_BINARY_VALUE));
    }

    @Test
    public void testGetValueAtInvalidIndexDoesNotThrow() throws Exception {
        MemoryContext<Integer> memoryContext = createMock(MemoryContext.class);

        expect(memoryContext.read(-1)).andThrow(new IndexOutOfBoundsException()).times(2);
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);

        model.getValueAt(-1, MemoryTableModel.COLUMN_HEX_VALUE);
        model.getValueAt(-1, MemoryTableModel.COLUMN_BINARY_VALUE);
    }
}
