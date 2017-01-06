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
package net.sf.emustudio.ssem.memory.gui;

import emulib.plugins.memory.MemoryContext;
import emulib.runtime.NumberUtils;
import emulib.runtime.NumberUtils.Strategy;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MemoryTableModelTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceWithNullModelThrows() throws Exception {
        new MemoryTableModel(null);
    }

    @Test
    public void testEditableAreOnlyValueColumns() throws Exception {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        assertTrue(model.isCellEditable(0, MemoryTableModel.COLUMN_HEX_VALUE));
        assertTrue(model.isCellEditable(0, MemoryTableModel.COLUMN_RAW_VALUE));
        assertFalse(model.isCellEditable(0, 34));

        for (int i = 0; i < 32; i++) {
            assertTrue(model.isCellEditable(0, i));
        }
    }

    @Test
    public void testClearCallsClearOnMemoryMock() throws Exception {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        memoryContext.clear();
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.clear();

        verify(memoryContext);
    }

    @Test
    public void testSetBinaryValueCellsMemoryWrite() throws Exception {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[] { 1,2,3,4 };
        Byte[] modified = new Byte[] { 1,2,(byte)0x83,4 }; // 16th bit set to 1, but original 3 wasnt'modified
        
        expect(memoryContext.readWord(10 * 4)).andReturn(row);
        memoryContext.writeWord(eq(10 * 4), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("1", 10, 16);

        verify(memoryContext);
    }

    @Test
    public void testSetHexValueCellsMemoryWrite() throws Exception {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[] { 1,2,3,4 };
        Byte[] modified = new Byte[] { (byte)0xFF,0,0,0 };
        
        expect(memoryContext.readWord(10 * 4)).andReturn(row);
        memoryContext.writeWord(eq(10 * 4), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);
        
        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("0xFF", 10, MemoryTableModel.COLUMN_HEX_VALUE);

        verify(memoryContext);
    }

    @Test
    public void testSetCharValueCellsMemoryWrite() throws Exception {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[] { 1,2,3,4 };
        Byte[] modified = new Byte[] { 0x56, (byte)0xf6, 0x16, (byte)0x86 };
        
        expect(memoryContext.readWord(10 * 4)).andReturn(row);
        memoryContext.writeWord(eq(10*4), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);
        
        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("ahoj", 10, MemoryTableModel.COLUMN_RAW_VALUE);

        verify(memoryContext);
    }

    
    @Test
    public void testSetValueAtInvalidIndexDoesNotThrow() throws Exception {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        model.setValueAt("10", -1, MemoryTableModel.COLUMN_RAW_VALUE);
    }

    @Test
    public void testSetNullValueDoesNotThrow() throws Exception {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        model.setValueAt(null, 0, MemoryTableModel.COLUMN_RAW_VALUE);
    }

    @Test
    public void testGetValueCallsMemoryRead() throws Exception {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[4];
        NumberUtils.writeInt(0x61686F6A, row, Strategy.REVERSE_BITS);

        expect(memoryContext.readWord(10 * 4)).andReturn(row).anyTimes();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);

        int value = NumberUtils.readInt(row, Strategy.REVERSE_BITS);
        assertEquals(Integer.toHexString(value).toUpperCase(), model.getValueAt(10, MemoryTableModel.COLUMN_HEX_VALUE));
        assertEquals("ahoj", model.getValueAt(10, MemoryTableModel.COLUMN_RAW_VALUE));
    }

    @Test
    public void testGetValueAtInvalidIndexDoesNotThrow() throws Exception {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        expect(memoryContext.readWord(-4)).andThrow(new IndexOutOfBoundsException()).times(2);
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);

        model.getValueAt(-1, MemoryTableModel.COLUMN_HEX_VALUE);
        model.getValueAt(-1, MemoryTableModel.COLUMN_RAW_VALUE);
    }
}
