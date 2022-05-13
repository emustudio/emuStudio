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
package net.emustudio.plugins.memory.ssem.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryTableModelTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceWithNullModelThrows() {
        new MemoryTableModel(null);
    }

    @Test
    public void testEditableAreOnlyValueColumns() {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        assertTrue(model.isCellEditable(0, MemoryTableModel.COLUMN_HEX_VALUE));
        assertTrue(model.isCellEditable(0, MemoryTableModel.COLUMN_DEC_VALUE));
        assertTrue(model.isCellEditable(0, MemoryTableModel.COLUMN_RAW_VALUE));
        assertFalse(model.isCellEditable(0, 35));

        for (int i = 0; i < 32; i++) {
            assertTrue(model.isCellEditable(0, i));
        }
    }

    @Test
    public void testClearCallsClearOnMemoryMock() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        memoryContext.clear();
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.clear();

        verify(memoryContext);
    }

    @Test
    public void testSetBinaryValueCellsMemoryWrite() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[]{1, 2, 3, 4};
        Byte[] modified = new Byte[]{1, 2, (byte) 0x83, 4}; // 16th bit set to 1, but original 3 wasnt'modified

        expect(memoryContext.read(10 * 4, 4)).andReturn(row);
        memoryContext.write(eq(10 * 4), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("1", 10, 16);

        verify(memoryContext);
    }

    @Test
    public void testSetHexValueCellsMemoryWrite() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[]{1, 2, 3, 4};
        Byte[] modified = new Byte[]{(byte) 0xFF, 0, 0, 0};

        expect(memoryContext.read(10 * 4, 4)).andReturn(row);
        memoryContext.write(eq(10 * 4), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("0xFF", 10, MemoryTableModel.COLUMN_HEX_VALUE);

        verify(memoryContext);
    }

    @Test
    public void testSetDecValueCellsMemoryWrite() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[]{1, 2, 3, 4};
        Byte[] modified = new Byte[]{(byte) 0xFF, 0, 0, 0};

        expect(memoryContext.read(10 * 4, 4)).andReturn(row);
        memoryContext.write(eq(10 * 4), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("0xFF", 10, MemoryTableModel.COLUMN_DEC_VALUE);

        verify(memoryContext);
    }

    @Test
    public void testSetCharValueCellsMemoryWrite() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[]{1, 2, 3, 4};
        Byte[] modified = new Byte[]{0x56, (byte) 0xf6, 0x16, (byte) 0x86};

        expect(memoryContext.read(10 * 4, 4)).andReturn(row);
        memoryContext.write(eq(10 * 4), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("ahoj", 10, MemoryTableModel.COLUMN_RAW_VALUE);

        verify(memoryContext);
    }


    @Test
    public void testSetValueAtInvalidIndexDoesNotThrow() {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        model.setValueAt("10", -1, MemoryTableModel.COLUMN_RAW_VALUE);
    }

    @Test
    public void testSetNullValueDoesNotThrow() {
        MemoryTableModel model = new MemoryTableModel(createMock(MemoryContext.class));

        model.setValueAt(null, 0, MemoryTableModel.COLUMN_RAW_VALUE);
    }

    @Test
    public void testGetValueCallsMemoryRead() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[4];
        NumberUtils.writeInt(0x61686F6A, row, NumberUtils.Strategy.REVERSE_BITS);

        expect(memoryContext.read(10 * 4, 4)).andReturn(row).anyTimes();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);

        int value = NumberUtils.readInt(row, NumberUtils.Strategy.REVERSE_BITS);
        assertEquals(Integer.toHexString(value).toUpperCase(), model.getValueAt(10, MemoryTableModel.COLUMN_HEX_VALUE));
        assertEquals("ahoj", model.getValueAt(10, MemoryTableModel.COLUMN_RAW_VALUE));
    }

    @Test
    public void testGetValueAtInvalidIndexDoesNotThrow() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        expect(memoryContext.read(-4, 4)).andThrow(new IndexOutOfBoundsException()).times(2);
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);

        model.getValueAt(-1, MemoryTableModel.COLUMN_HEX_VALUE);
        model.getValueAt(-1, MemoryTableModel.COLUMN_RAW_VALUE);
    }
}
