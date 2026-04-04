/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui;

import net.emustudio.plugins.memory.rasp.MemoryContextImpl;
import net.emustudio.plugins.memory.rasp.MemoryContextImplFactory;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RaspTableModelTest {
    private MemoryContextImpl context;
    private RaspTableModel model;

    @Before
    public void setUp() {
        context = MemoryContextImplFactory.create();
        model = new RaspTableModel(context);
    }

    @After
    public void tearDown() {
        context.destroy();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMemoryThrows() {
        new RaspTableModel(null);
    }

    @Test
    public void testGetColumnCount() {
        assertEquals(2, model.getColumnCount());
    }

    @Test
    public void testGetRowCountEmpty() {
        assertEquals(0, model.getRowCount());
    }

    @Test
    public void testGetRowCountAfterWrite() {
        context.write(5, 42);
        assertEquals(5, model.getRowCount());
    }

    @Test
    public void testIsCellEditableColumn0() {
        assertFalse(model.isCellEditable(0, 0));
    }

    @Test
    public void testIsCellEditableColumn1() {
        assertTrue(model.isCellEditable(0, 1));
    }

    @Test
    public void testIsCellEditableColumn2() {
        assertFalse(model.isCellEditable(0, 2));
    }

    @Test
    public void testGetColumnNameAddress() {
        assertEquals("Address", model.getColumnName(0));
    }

    @Test
    public void testGetColumnNameNumericValue() {
        assertEquals("Numeric Value", model.getColumnName(1));
    }

    @Test
    public void testGetColumnNameDefault() {
        assertNull(model.getColumnName(2));
        assertNull(model.getColumnName(99));
    }

    @Test
    public void testGetValueAtAddressColumnWithoutLabel() {
        context.write(0, 100);
        Object value = model.getValueAt(0, 0);
        assertEquals("0", value);
    }

    @Test
    public void testGetValueAtAddressColumnWithLabel() {
        context.write(5, 100);
        context.setLabels(List.of(new RaspLabel() {
            @Override
            public int getAddress() {
                return 5;
            }

            @Override
            public String getLabel() {
                return "LOOP";
            }
        }));

        // Row 5 should show "5 LOOP"
        Object value = model.getValueAt(5, 0);
        assertEquals("5 LOOP", value);
    }

    @Test
    public void testGetValueAtNumericValueColumn() {
        context.write(3, 42);
        Object value = model.getValueAt(3, 1);
        assertEquals("42", value);
    }

    @Test
    public void testGetValueAtNumericValueColumnDefaultZero() {
        // Reading an unset address should return "0"
        context.write(5, 1); // ensure size >= 1
        Object value = model.getValueAt(0, 1);
        assertEquals("0", value);
    }

    @Test
    public void testGetValueAtInvalidColumnReturnsEmpty() {
        context.write(0, 100);
        Object value = model.getValueAt(0, 5);
        assertEquals("", value);
    }

    @Test
    public void testSetValueAtWritesToMemory() {
        context.write(0, 0); // ensure address exists
        model.setValueAt("42", 0, 1);
        assertEquals(Integer.valueOf(42), context.read(0));
    }

    @Test
    public void testSetValueAtHexValue() {
        context.write(0, 0);
        model.setValueAt("0xFF", 0, 1);
        assertEquals(Integer.valueOf(255), context.read(0));
    }

    @Test
    public void testSetValueAtInvalidValueDoesNothing() {
        context.write(0, 100);
        model.setValueAt("not_a_number", 0, 1);
        // value should remain unchanged
        assertEquals(Integer.valueOf(100), context.read(0));
    }

    @Test
    public void testSetValueAtEmptyStringDoesNothing() {
        context.write(0, 50);
        model.setValueAt("", 0, 1);
        // value should remain unchanged
        assertEquals(Integer.valueOf(50), context.read(0));
    }

    @Test
    public void testGetValueAtAddressColumnNonZeroRow() {
        context.write(0, 10);
        context.write(1, 20);
        context.write(2, 30);

        assertEquals("0", model.getValueAt(0, 0));
        assertEquals("1", model.getValueAt(1, 0));
        assertEquals("2", model.getValueAt(2, 0));
    }

    @Test
    public void testGetValueAtNumericValueColumnMultipleRows() {
        context.write(0, 10);
        context.write(1, 20);
        context.write(2, 30);

        assertEquals("10", model.getValueAt(0, 1));
        assertEquals("20", model.getValueAt(1, 1));
        assertEquals("30", model.getValueAt(2, 1));
    }

    @Test
    public void testSetValueAtNegativeValue() {
        context.write(0, 50);
        model.setValueAt("-5", 0, 1);
        // RadixUtils.parseRadix doesn't support negative numbers, so value remains unchanged
        assertEquals(Integer.valueOf(50), context.read(0));
    }
}

