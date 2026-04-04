/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck.gui;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MemoryTableModelTest {
    private Byte[] memory;
    private MemoryTableModel model;

    @Before
    public void setUp() {
        memory = new Byte[256];
        for (int i = 0; i < memory.length; i++) {
            memory[i] = (byte) i;
        }
        model = new MemoryTableModel(memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemory() {
        new MemoryTableModel(null);
    }

    @Test
    public void testGetRowCount() {
        assertEquals(1, model.getRowCount());
    }

    @Test
    public void testGetColumnCount() {
        assertEquals(5, model.getColumnCount());
    }

    @Test
    public void testGetColumnNameAtDefaultP() {
        // Default P = 0, columns: P-2, P-1, P, P+1, P+2
        // Column 0 => index = 0 + (0 - 2) = -2 => "N/A"
        assertEquals("N/A", model.getColumnName(0));
        // Column 1 => index = 0 + (1 - 2) = -1 => "N/A"
        assertEquals("N/A", model.getColumnName(1));
        // Column 2 => index = 0 + (2 - 2) = 0 => "00h"
        assertEquals("00h", model.getColumnName(2));
        // Column 3 => index = 0 + (3 - 2) = 1 => "01h"
        assertEquals("01h", model.getColumnName(3));
        // Column 4 => index = 0 + (4 - 2) = 2 => "02h"
        assertEquals("02h", model.getColumnName(4));
    }

    @Test
    public void testGetColumnNameAfterSetP() {
        model.setP(5);
        // Column 0 => index = 5 + (0 - 2) = 3 => "03h"
        assertEquals("03h", model.getColumnName(0));
        // Column 2 => index = 5 + (2 - 2) = 5 => "05h"
        assertEquals("05h", model.getColumnName(2));
        // Column 4 => index = 5 + (4 - 2) = 7 => "07h"
        assertEquals("07h", model.getColumnName(4));
    }

    @Test
    public void testGetColumnNameWhenPIs1() {
        model.setP(1);
        // Column 0 => index = 1 + (0 - 2) = -1 => "N/A"
        assertEquals("N/A", model.getColumnName(0));
        // Column 1 => index = 1 + (1 - 2) = 0 => "00h"
        assertEquals("00h", model.getColumnName(1));
        // Column 2 => index = 1 + (2 - 2) = 1 => "01h"
        assertEquals("01h", model.getColumnName(2));
    }

    @Test
    public void testGetValueAtDefaultP() {
        // Default P = 0
        // Column 0 => index = 0 + (0 - 2) = -2 => ""
        assertEquals("", model.getValueAt(0, 0));
        // Column 1 => index = 0 + (1 - 2) = -1 => ""
        assertEquals("", model.getValueAt(0, 1));
        // Column 2 => index = 0, memory[0] = 0 => "00h"
        assertEquals("00h", model.getValueAt(0, 2));
        // Column 3 => index = 1, memory[1] = 1 => "01h"
        assertEquals("01h", model.getValueAt(0, 3));
        // Column 4 => index = 2, memory[2] = 2 => "02h"
        assertEquals("02h", model.getValueAt(0, 4));
    }

    @Test
    public void testGetValueAtAfterSetP() {
        model.setP(10);
        // Column 2 => index = 10, memory[10] = 0x0A => "0Ah"
        assertEquals("0Ah", model.getValueAt(0, 2));
    }

    @Test
    public void testGetValueAtNegativeIndex() {
        // Default P = 0
        // Column 0 => index = -2, should return ""
        assertEquals("", model.getValueAt(0, 0));
    }

    @Test
    public void testSetPUpdatesModel() {
        model.setP(100);
        // Column 2 => index = 100, memory[100] = 0x64 => "64h"
        assertEquals("64h", model.getValueAt(0, 2));
    }

    @Test
    public void testGetValueAtHighAddress() {
        model.setP(0xFE);
        // Column 2 => index = 0xFE, memory[0xFE] = (byte)0xFE => "FEh"
        assertEquals("FEh", model.getValueAt(0, 2));
    }

    @Test
    public void testSetPToZero() {
        model.setP(10);
        model.setP(0);
        // Column 2 => index = 0 => "00h"
        assertEquals("00h", model.getValueAt(0, 2));
    }

    @Test
    public void testColumnNameFormatHex() {
        model.setP(0x10);
        // Column 2 => index = 0x10 => "10h"
        assertEquals("10h", model.getColumnName(2));
    }

    @Test
    public void testGetValueFormattedAsHex() {
        memory[5] = (byte) 0xAB;
        model.setP(5);
        // Column 2 => index = 5, memory[5] = 0xAB => "ABh"
        assertEquals("ABh", model.getValueAt(0, 2));
    }
}

