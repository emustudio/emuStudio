/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.Context8080Impl;
import net.emustudio.plugins.cpu.intel8080.EmulatorEngine;
import org.junit.Before;
import org.junit.Test;

import static net.emustudio.plugins.cpu.intel8080.EmulatorEngine.*;
import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FlagsModelTest {

    private EmulatorEngine engine;
    private FlagsModel flagsModel;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        engine = new EmulatorEngine(memory, new Context8080Impl());
        flagsModel = new FlagsModel(engine);
    }

    @Test
    public void testGetRowCount() {
        assertEquals(2, flagsModel.getRowCount());
    }

    @Test
    public void testGetColumnCount() {
        assertEquals(5, flagsModel.getColumnCount());
    }

    @Test
    public void testGetColumnNames() {
        assertEquals("S", flagsModel.getColumnName(0));
        assertEquals("Z", flagsModel.getColumnName(1));
        assertEquals("A", flagsModel.getColumnName(2));
        assertEquals("P", flagsModel.getColumnName(3));
        assertEquals("C", flagsModel.getColumnName(4));
    }

    @Test
    public void testGetValueAtRow0ReturnsFlagNames() {
        assertEquals("S", flagsModel.getValueAt(0, 0));
        assertEquals("Z", flagsModel.getValueAt(0, 1));
        assertEquals("A", flagsModel.getValueAt(0, 2));
        assertEquals("P", flagsModel.getValueAt(0, 3));
        assertEquals("C", flagsModel.getValueAt(0, 4));
    }

    @Test
    public void testGetValueAtRow1ReturnsZerosInitially() {
        assertEquals(0, flagsModel.getValueAt(1, 0));
        assertEquals(0, flagsModel.getValueAt(1, 1));
        assertEquals(0, flagsModel.getValueAt(1, 2));
        assertEquals(0, flagsModel.getValueAt(1, 3));
        assertEquals(0, flagsModel.getValueAt(1, 4));
    }

    @Test
    public void testGetValueAtInvalidRowReturnsNull() {
        assertNull(flagsModel.getValueAt(2, 0));
    }

    @Test
    public void testFireTableDataChangedUpdatesSignFlag() {
        engine.flags = (short) FLAG_S;
        flagsModel.fireTableDataChanged();
        assertEquals(1, flagsModel.getValueAt(1, 0));
        assertEquals(0, flagsModel.getValueAt(1, 1));
        assertEquals(0, flagsModel.getValueAt(1, 2));
        assertEquals(0, flagsModel.getValueAt(1, 3));
        assertEquals(0, flagsModel.getValueAt(1, 4));
    }

    @Test
    public void testFireTableDataChangedUpdatesZeroFlag() {
        engine.flags = (short) FLAG_Z;
        flagsModel.fireTableDataChanged();
        assertEquals(0, flagsModel.getValueAt(1, 0));
        assertEquals(1, flagsModel.getValueAt(1, 1));
    }

    @Test
    public void testFireTableDataChangedUpdatesAuxCarryFlag() {
        engine.flags = (short) FLAG_AC;
        flagsModel.fireTableDataChanged();
        assertEquals(1, flagsModel.getValueAt(1, 2));
    }

    @Test
    public void testFireTableDataChangedUpdatesParityFlag() {
        engine.flags = (short) FLAG_P;
        flagsModel.fireTableDataChanged();
        assertEquals(1, flagsModel.getValueAt(1, 3));
    }

    @Test
    public void testFireTableDataChangedUpdatesCarryFlag() {
        engine.flags = (short) FLAG_C;
        flagsModel.fireTableDataChanged();
        assertEquals(1, flagsModel.getValueAt(1, 4));
    }

    @Test
    public void testFireTableDataChangedUpdatesAllFlags() {
        engine.flags = (short) (FLAG_S | FLAG_Z | FLAG_AC | FLAG_P | FLAG_C);
        flagsModel.fireTableDataChanged();
        assertEquals(1, flagsModel.getValueAt(1, 0));
        assertEquals(1, flagsModel.getValueAt(1, 1));
        assertEquals(1, flagsModel.getValueAt(1, 2));
        assertEquals(1, flagsModel.getValueAt(1, 3));
        assertEquals(1, flagsModel.getValueAt(1, 4));
    }

    @Test
    public void testFireTableDataChangedClearsFlags() {
        engine.flags = (short) (FLAG_S | FLAG_Z | FLAG_AC | FLAG_P | FLAG_C);
        flagsModel.fireTableDataChanged();
        engine.flags = 0;
        flagsModel.fireTableDataChanged();
        assertEquals(0, flagsModel.getValueAt(1, 0));
        assertEquals(0, flagsModel.getValueAt(1, 1));
        assertEquals(0, flagsModel.getValueAt(1, 2));
        assertEquals(0, flagsModel.getValueAt(1, 3));
        assertEquals(0, flagsModel.getValueAt(1, 4));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNull() {
        new FlagsModel(null);
    }
}

