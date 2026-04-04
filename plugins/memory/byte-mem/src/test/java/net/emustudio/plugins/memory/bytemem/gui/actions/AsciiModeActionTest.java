/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Before;
import org.junit.Test;
import javax.swing.*;
import java.awt.event.ActionEvent;
import static org.junit.Assert.*;
public class AsciiModeActionTest {
    private MemoryContextImpl context;
    private MemoryTableModel tableModel;
    @Before
    public void setUp() {
        context = TestMemoryContextFactory.create(256, 1, 0);
        tableModel = new MemoryTableModel(context);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        new AsciiModeAction(null, new JToggleButton());
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullButtonThrows() {
        new AsciiModeAction(tableModel, null);
    }
    @Test
    public void testActionPerformedTogglesAsciiModeOn() {
        JToggleButton btn = new JToggleButton();
        btn.setSelected(true);
        AsciiModeAction action = new AsciiModeAction(tableModel, btn);
        context.write(0, (byte) 65);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle"));
        assertEquals('A', tableModel.getValueAt(0, 0));
    }
    @Test
    public void testActionPerformedTogglesAsciiModeOff() {
        JToggleButton btn = new JToggleButton();
        btn.setSelected(false);
        AsciiModeAction action = new AsciiModeAction(tableModel, btn);
        context.write(0, (byte) 65);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle"));
        assertEquals("41", tableModel.getValueAt(0, 0));
    }
    @Test
    public void testToggleBackAndForth() {
        JToggleButton btn = new JToggleButton();
        AsciiModeAction action = new AsciiModeAction(tableModel, btn);
        context.write(0, (byte) 65);
        btn.setSelected(true);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle"));
        assertEquals('A', tableModel.getValueAt(0, 0));
        btn.setSelected(false);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle"));
        assertEquals("41", tableModel.getValueAt(0, 0));
    }
}
