/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import net.emustudio.plugins.memory.ssem.gui.table.MemoryTableModel;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryTableModelTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceWithNullModelThrows() {
        new MemoryTableModel(null);
    }

    @Test
    public void testGetRowCountReturns32() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        assertEquals(32, model.getRowCount());
    }

    @Test
    public void testGetColumnCountReturns35() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        assertEquals(35, model.getColumnCount());
    }

    @Test
    public void testGetColumnNameForHexColumn() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        assertEquals("Hex", model.getColumnName(MemoryTableModel.COLUMN_HEX_VALUE));
    }

    @Test
    public void testGetColumnNameForDecColumn() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        assertEquals("Dec", model.getColumnName(MemoryTableModel.COLUMN_DEC_VALUE));
    }

    @Test
    public void testGetColumnNameForCharColumn() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        assertEquals("Char", model.getColumnName(MemoryTableModel.COLUMN_RAW_VALUE));
    }

    @Test
    public void testGetColumnNameForLineBitColumns() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        for (int i = 0; i < 5; i++) {
            assertEquals("L", model.getColumnName(i));
        }
    }

    @Test
    public void testGetColumnNameForInstructionBitColumns() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        for (int i = 13; i <= 15; i++) {
            assertEquals("I", model.getColumnName(i));
        }
    }

    @Test
    public void testGetColumnNameForOtherBitColumns() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        // Columns 5..12 and 16..31 are not Line and not Instruction
        assertEquals("", model.getColumnName(5));
        assertEquals("", model.getColumnName(10));
        assertEquals("", model.getColumnName(20));
        assertEquals("", model.getColumnName(31));
    }

    @Test
    public void testLineBitColumnsIdentifiedViaColumnName() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        // Columns 0-4 should be named "L" (line bits)
        for (int i = 0; i < 5; i++) {
            assertEquals("Column " + i + " should be line bit", "L", model.getColumnName(i));
        }
        // Column 5 should not be a line bit
        assertNotEquals("L", model.getColumnName(5));
    }

    @Test
    public void testInstructionBitColumnsIdentifiedViaColumnName() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        // Column 12 should not be instruction bit
        assertNotEquals("I", model.getColumnName(12));
        // Columns 13-15 should be named "I" (instruction bits)
        for (int i = 13; i <= 15; i++) {
            assertEquals("Column " + i + " should be instruction bit", "I", model.getColumnName(i));
        }
        // Column 16 should not be instruction bit
        assertNotEquals("I", model.getColumnName(16));
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
    public void testCellNotEditableForNegativeColumn() {
        MemoryTableModel model = new MemoryTableModel(createNiceMock(MemoryContext.class));
        assertFalse(model.isCellEditable(0, -1));
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
    public void testSetBinaryValueToZero() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        Byte[] row = new Byte[]{(byte) 0x80, 0, 0, 0};
        // Bit 0 (MSB of byte 0) set to 0 => 0x80 & ~0x80 = 0x00
        Byte[] modified = new Byte[]{0x00, 0, 0, 0};

        expect(memoryContext.read(0, 4)).andReturn(row);
        memoryContext.write(eq(0), aryEq(modified));
        expectLastCall().once();
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("0", 0, 0);

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
    public void testSetValueAtNonEditableColumnDoesNothing() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        model.setValueAt("1", 0, 35); // column 35 is not editable

        verify(memoryContext); // no read/write calls should have been made
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
    public void testGetValueAtDecColumn() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        MemoryTableModel model = new MemoryTableModel(memory);

        Object decValue = model.getValueAt(0, MemoryTableModel.COLUMN_DEC_VALUE);
        assertNotNull(decValue);
        assertEquals("0", decValue);
    }

    @Test
    public void testGetValueAtBitColumn() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        // Write a known value to row 0
        memory.write(0, (byte) 0x80); // bit 0 = 1 (MSB)

        MemoryTableModel model = new MemoryTableModel(memory);

        // Column 0 = bit 7 of byte 0 = MSB = should be 1
        Object bitValue = model.getValueAt(0, 0);
        assertEquals((byte) 1, bitValue);

        // Column 1 = bit 6 of byte 0 = should be 0
        Object bitValue2 = model.getValueAt(0, 1);
        assertEquals((byte) 0, bitValue2);
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

    @Test
    public void testGetValueAtInvalidIndexReturnsEmptyString() {
        MemoryContext<Byte> memoryContext = createMock(MemoryContext.class);

        expect(memoryContext.read(-4, 4)).andThrow(new IndexOutOfBoundsException());
        replay(memoryContext);

        MemoryTableModel model = new MemoryTableModel(memoryContext);
        assertEquals("", model.getValueAt(-1, MemoryTableModel.COLUMN_HEX_VALUE));
    }

    @Test
    public void testDataChangedAtFiresTableCellUpdated() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        MemoryTableModel model = new MemoryTableModel(memory);

        // This should not throw - just exercises the dataChangedAt path
        model.dataChangedAt(0, 3);
    }

    @Test
    public void testDataChangedAtSingleAddress() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        MemoryTableModel model = new MemoryTableModel(memory);

        // Should not throw
        model.dataChangedAt(4, 4);
    }

    @Test
    public void testSetValueWithInvalidHexDoesNotThrow() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        MemoryTableModel model = new MemoryTableModel(memory);

        // Invalid hex should be caught and not throw
        model.setValueAt("not_a_number", 0, MemoryTableModel.COLUMN_HEX_VALUE);
    }

    @Test
    public void testSetCharValueShorterThan4Chars() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        MemoryTableModel model = new MemoryTableModel(memory);

        // Should handle short strings without error
        model.setValueAt("ab", 0, MemoryTableModel.COLUMN_RAW_VALUE);
    }

    @Test
    public void testSetCharValueLongerThan4Chars() {
        MemoryContextImpl memory = new MemoryContextImpl(new Annotations());
        MemoryTableModel model = new MemoryTableModel(memory);

        // Should truncate to 4 chars
        model.setValueAt("abcdefgh", 0, MemoryTableModel.COLUMN_RAW_VALUE);
    }

    @Test
    public void testColumnConstants() {
        assertEquals(32, MemoryTableModel.COLUMN_HEX_VALUE);
        assertEquals(33, MemoryTableModel.COLUMN_DEC_VALUE);
        assertEquals(34, MemoryTableModel.COLUMN_RAW_VALUE);
    }
}
