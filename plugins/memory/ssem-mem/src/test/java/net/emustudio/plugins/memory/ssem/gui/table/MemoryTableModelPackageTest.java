/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.table;

import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for MemoryTableModel that require package-level access to static methods.
 */
public class MemoryTableModelPackageTest {

    private MemoryContextImpl context;
    private MemoryTableModel model;

    @Before
    public void setUp() {
        context = new MemoryContextImpl(new Annotations());
        model = new MemoryTableModel(context);
    }

    @Test
    public void testIsBitLineReturnsTrueForColumns0to4() {
        assertTrue(MemoryTableModel.isBitLine(0));
        assertTrue(MemoryTableModel.isBitLine(1));
        assertTrue(MemoryTableModel.isBitLine(2));
        assertTrue(MemoryTableModel.isBitLine(3));
        assertTrue(MemoryTableModel.isBitLine(4));
    }

    @Test
    public void testIsBitLineReturnsFalseForOtherColumns() {
        assertFalse(MemoryTableModel.isBitLine(-1));
        assertFalse(MemoryTableModel.isBitLine(5));
        assertFalse(MemoryTableModel.isBitLine(13));
        assertFalse(MemoryTableModel.isBitLine(32));
    }

    @Test
    public void testIsBitInstructionReturnsTrueForColumns13to15() {
        assertTrue(MemoryTableModel.isBitInstruction(13));
        assertTrue(MemoryTableModel.isBitInstruction(14));
        assertTrue(MemoryTableModel.isBitInstruction(15));
    }

    @Test
    public void testIsBitInstructionReturnsFalseForOtherColumns() {
        assertFalse(MemoryTableModel.isBitInstruction(0));
        assertFalse(MemoryTableModel.isBitInstruction(12));
        assertFalse(MemoryTableModel.isBitInstruction(16));
        assertFalse(MemoryTableModel.isBitInstruction(32));
    }

    @Test
    public void testGetValueAtAllBitColumnsForZeroRow() {
        // All zeros should produce 0 bits
        for (int col = 0; col < 32; col++) {
            Object val = model.getValueAt(0, col);
            assertEquals("Bit at column " + col + " should be 0", (byte) 0, val);
        }
    }

    @Test
    public void testGetValueAtBitColumnsAfterWrite() {
        // Write 0xFF to first byte of row 0 (all bits set)
        context.write(0, (byte) 0xFF);
        // Columns 0-7 correspond to bits of byte 0 in reverse order (bit 7 down to bit 0)
        for (int col = 0; col < 8; col++) {
            Object val = model.getValueAt(0, col);
            assertEquals("Bit at column " + col + " should be 1", (byte) 1, val);
        }
        // Remaining columns in other bytes should still be 0
        for (int col = 8; col < 32; col++) {
            Object val = model.getValueAt(0, col);
            assertEquals("Bit at column " + col + " should be 0", (byte) 0, val);
        }
    }

    @Test
    public void testSetValueAtBitColumnAndReadBack() {
        // Set bit 0 (column 0) of row 0 to 1
        model.setValueAt("1", 0, 0);

        // Bit 0 is the MSB of byte 0 (bit index 7)
        // So byte 0 should be 0x80
        assertEquals((byte) 0x80, (byte) context.read(0));
    }

    @Test
    public void testWriteBitInvalidValueDoesNotModify() {
        context.write(0, (byte) 0x00);

        // "11" parsed as binary is 3, which (3 & 1) != 3, so writeBit should log error and not modify
        model.setValueAt("11", 0, 0);

        // Memory should remain unchanged
        assertEquals((byte) 0x00, (byte) context.read(0));
    }

    @Test
    public void testGetRowCount() {
        assertEquals(32, model.getRowCount());
    }

    @Test
    public void testGetColumnCount() {
        assertEquals(35, model.getColumnCount());
    }

    @Test
    public void testDataChangedAtMultipleAddresses() {
        // Should not throw
        model.dataChangedAt(0, 7);
    }
}

