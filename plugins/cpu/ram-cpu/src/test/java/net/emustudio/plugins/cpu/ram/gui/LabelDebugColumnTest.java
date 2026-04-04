/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class LabelDebugColumnTest {
    private RamMemoryContext memory;
    private LabelDebugColumn column;

    @Before
    public void setUp() {
        memory = createNiceMock(RamMemoryContext.class);
    }

    @Test
    public void testGetClassType() {
        replay(memory);
        column = new LabelDebugColumn(memory);
        assertEquals(String.class, column.getClassType());
    }

    @Test
    public void testGetTitle() {
        replay(memory);
        column = new LabelDebugColumn(memory);
        assertEquals("label", column.getTitle());
    }

    @Test
    public void testIsEditable() {
        replay(memory);
        column = new LabelDebugColumn(memory);
        assertFalse(column.isEditable());
    }

    @Test
    public void testGetValueWithLabel() {
        RamLabel label = createNiceMock(RamLabel.class);
        expect(label.getLabel()).andReturn("START").anyTimes();
        replay(label);

        expect(memory.getLabel(5)).andReturn(Optional.of(label)).anyTimes();
        replay(memory);
        column = new LabelDebugColumn(memory);

        assertEquals("START", column.getValue(5));
    }

    @Test
    public void testGetValueWithoutLabel() {
        expect(memory.getLabel(5)).andReturn(Optional.empty()).anyTimes();
        replay(memory);
        column = new LabelDebugColumn(memory);

        assertEquals("", column.getValue(5));
    }

    @Test
    public void testSetValueDoesNotThrow() {
        replay(memory);
        column = new LabelDebugColumn(memory);
        column.setValue(0, "anything");
    }
}

