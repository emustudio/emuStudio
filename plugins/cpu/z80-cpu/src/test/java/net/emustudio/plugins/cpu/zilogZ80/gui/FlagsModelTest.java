/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.zilogZ80.ContextZ80Impl;
import net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class FlagsModelTest {
    private EmulatorEngine engine;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Byte.class).anyTimes();
        replay(memory);
        ContextZ80Impl context = new ContextZ80Impl();
        engine = new EmulatorEngine(memory, context);
    }

    @Test
    public void testRowCount() {
        FlagsModel model = new FlagsModel(0, engine);
        assertEquals(1, model.getRowCount());
    }

    @Test
    public void testColumnCount() {
        FlagsModel model = new FlagsModel(0, engine);
        assertEquals(6, model.getColumnCount());
    }

    @Test
    public void testColumnNames() {
        FlagsModel model = new FlagsModel(0, engine);
        assertEquals("S", model.getColumnName(0));
        assertEquals("Z", model.getColumnName(1));
        assertEquals("H", model.getColumnName(2));
        assertEquals("P/V", model.getColumnName(3));
        assertEquals("N", model.getColumnName(4));
        assertEquals("C", model.getColumnName(5));
    }

    @Test
    public void testDefaultValuesBeforeFireTableDataChanged() {
        FlagsModel model = new FlagsModel(0, engine);
        // Before fireTableDataChanged, all values are 0
        for (int i = 0; i < 6; i++) {
            assertEquals(0, model.getValueAt(0, i));
        }
    }

    @Test
    public void testFlagsSet0_SignFlag() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_S;
        model.fireTableDataChanged();
        assertEquals(1, model.getValueAt(0, 0)); // S
        assertEquals(0, model.getValueAt(0, 1)); // Z
        assertEquals(0, model.getValueAt(0, 2)); // H
        assertEquals(0, model.getValueAt(0, 3)); // P/V
        assertEquals(0, model.getValueAt(0, 4)); // N
        assertEquals(0, model.getValueAt(0, 5)); // C
    }

    @Test
    public void testFlagsSet0_ZeroFlag() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_Z;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 0)); // S
        assertEquals(1, model.getValueAt(0, 1)); // Z
        assertEquals(0, model.getValueAt(0, 2)); // H
    }

    @Test
    public void testFlagsSet0_HalfCarryFlag() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_H;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 0)); // S
        assertEquals(1, model.getValueAt(0, 2)); // H
        assertEquals(0, model.getValueAt(0, 3)); // P/V
    }

    @Test
    public void testFlagsSet0_ParityOverflowFlag() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_PV;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 2)); // H
        assertEquals(1, model.getValueAt(0, 3)); // P/V
        assertEquals(0, model.getValueAt(0, 4)); // N
    }

    @Test
    public void testFlagsSet0_SubtractFlag() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_N;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 3)); // P/V
        assertEquals(1, model.getValueAt(0, 4)); // N
        assertEquals(0, model.getValueAt(0, 5)); // C
    }

    @Test
    public void testFlagsSet0_CarryFlag() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_C;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 4)); // N
        assertEquals(1, model.getValueAt(0, 5)); // C
    }

    @Test
    public void testFlagsSet0_AllFlags() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_H
                | EmulatorEngine.FLAG_PV | EmulatorEngine.FLAG_N | EmulatorEngine.FLAG_C;
        model.fireTableDataChanged();
        assertEquals(1, model.getValueAt(0, 0)); // S
        assertEquals(1, model.getValueAt(0, 1)); // Z
        assertEquals(1, model.getValueAt(0, 2)); // H
        assertEquals(1, model.getValueAt(0, 3)); // P/V
        assertEquals(1, model.getValueAt(0, 4)); // N
        assertEquals(1, model.getValueAt(0, 5)); // C
    }

    @Test
    public void testFlagsSet0_NoFlags() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = 0;
        model.fireTableDataChanged();
        for (int i = 0; i < 6; i++) {
            assertEquals(0, model.getValueAt(0, i));
        }
    }

    @Test
    public void testFlagsSet1_UsesFlags2() {
        FlagsModel model = new FlagsModel(1, engine);
        engine.flags = EmulatorEngine.FLAG_S; // primary flags - should be ignored for set 1
        engine.flags2 = EmulatorEngine.FLAG_C; // secondary flags
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 0)); // S - from flags2, not set
        assertEquals(1, model.getValueAt(0, 5)); // C - from flags2, set
    }

    @Test
    public void testFlagsSet1_AllFlags() {
        FlagsModel model = new FlagsModel(1, engine);
        engine.flags2 = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_H
                | EmulatorEngine.FLAG_PV | EmulatorEngine.FLAG_N | EmulatorEngine.FLAG_C;
        model.fireTableDataChanged();
        for (int i = 0; i < 6; i++) {
            assertEquals(1, model.getValueAt(0, i));
        }
    }

    @Test
    public void testFlagsSet1_NoFlags() {
        FlagsModel model = new FlagsModel(1, engine);
        engine.flags2 = 0;
        model.fireTableDataChanged();
        for (int i = 0; i < 6; i++) {
            assertEquals(0, model.getValueAt(0, i));
        }
    }

    @Test
    public void testFireTableDataChangedUpdatesValues() {
        FlagsModel model = new FlagsModel(0, engine);
        // Initially no flags
        engine.flags = 0;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 0));

        // Set sign flag
        engine.flags = EmulatorEngine.FLAG_S;
        model.fireTableDataChanged();
        assertEquals(1, model.getValueAt(0, 0));

        // Clear it again
        engine.flags = 0;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 0));
    }

    @Test
    public void testFlagsSet0_SignAndCarry() {
        FlagsModel model = new FlagsModel(0, engine);
        engine.flags = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C;
        model.fireTableDataChanged();
        assertEquals(1, model.getValueAt(0, 0)); // S
        assertEquals(0, model.getValueAt(0, 1)); // Z
        assertEquals(0, model.getValueAt(0, 2)); // H
        assertEquals(0, model.getValueAt(0, 3)); // P/V
        assertEquals(0, model.getValueAt(0, 4)); // N
        assertEquals(1, model.getValueAt(0, 5)); // C
    }

    @Test
    public void testFlagsSet1_ZeroAndHalfCarry() {
        FlagsModel model = new FlagsModel(1, engine);
        engine.flags2 = EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_H;
        model.fireTableDataChanged();
        assertEquals(0, model.getValueAt(0, 0)); // S
        assertEquals(1, model.getValueAt(0, 1)); // Z
        assertEquals(1, model.getValueAt(0, 2)); // H
        assertEquals(0, model.getValueAt(0, 3)); // P/V
        assertEquals(0, model.getValueAt(0, 4)); // N
        assertEquals(0, model.getValueAt(0, 5)); // C
    }
}

