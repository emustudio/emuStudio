/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.table;

import net.emustudio.plugins.memory.bytemem.TestExecutionState;

import net.emustudio.emulib.runtime.ExecutionState;
import net.emustudio.emulib.plugins.cpu.CPU;
import java.util.concurrent.CompletableFuture;
import net.emustudio.emulib.runtime.annotations.AnnotationEngine;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.Color;

import static org.junit.Assert.*;

public class MemoryTableTest {
    private MemoryTableModel tableModel;
    private final AnnotationEngine metadata = new AnnotationEngine();
    private final ExecutionState execution = new ExecutionState() {
        public int currentInstruction() { return 3; }
        public CPU.RunState runState() { return CPU.RunState.STATE_STOPPED_BREAK; }
        public boolean isBusy() { return false; }
        public CompletableFuture<Void> mutate(Runnable operation) { return TestExecutionState.create().mutate(operation); }
        public void addCPUListener(CPU.CPUListener listener) { }
        public void removeCPUListener(CPU.CPUListener listener) { }
        public void addListener(Runnable listener) { }
        public void removeListener(Runnable listener) { }
    };

    @Before
    public void setUp() {
        tableModel = new MemoryTableModel(TestMemoryContextFactory.create(65536, 2, 4), TestExecutionState.create());
        metadata.put(new AnnotationEngine.Entry(1, 1, 1, 2, "label", "<start>", null));
        metadata.put(new AnnotationEngine.Entry(1, 2, 2, 3, "emuStudio.user/label", "Label & details", null));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        new MemoryTable(null, new JScrollPane(), metadata, execution);
    }

    @Test
    public void testGetTableModel() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        assertSame(tableModel, table.getTableModel());
    }

    @Test
    public void testTableHasCorrectColumnCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        assertEquals(16, table.getColumnCount());
    }

    @Test
    public void testTableHasCorrectRowCount() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        assertEquals(16, table.getRowCount());
    }

    @Test
    public void testCellSelectionEnabled() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        assertTrue(table.getCellSelectionEnabled());
    }

    @Test
    public void testSelectionModeIsSingle() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
    }

    @Test
    public void testTableIsOpaque() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        assertTrue(table.isOpaque());
    }

    @Test
    public void metadataColorsAndTooltipsFollowEachAddress() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        tableModel.setCurrentBank(1);
        JLabel initial = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);
        assertFalse(initial.getToolTipText().contains("Label:"));
        tableModel.setCurrentBank(0);

        JLabel label = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);
        assertEquals(new Color(230, 230, 230), label.getBackground());
        assertTrue(label.getToolTipText().contains("&lt;start&gt;"));
        JLabel annotation = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 2), 0, 2);
        assertEquals(new Color(230, 230, 230), annotation.getBackground());
        assertTrue(annotation.getToolTipText().contains("Label &amp; details"));
        JLabel instruction = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 3), 0, 3);
        assertEquals(new Color(255, 204, 204), instruction.getBackground());
        assertTrue(instruction.getToolTipText().contains("Current instruction"));
        JLabel empty = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 4), 0, 4);
        assertNull(empty.getToolTipText());
    }

    @Test
    public void unavailableMetadataLeavesMemoryUsable() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), null, null);

        JLabel cell = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);

        assertEquals("00", cell.getText());
    }

    @Test
    public void rendererUsesCurrentLabels() {
        MemoryTable table = new MemoryTable(tableModel, new JScrollPane(), metadata, execution);
        metadata.replaceOwner(1, java.util.List.of(
                new AnnotationEngine.Entry(1, 3, 1, 2, "label", "new label", null)));

        JLabel label = (JLabel) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);

        assertTrue(label.getToolTipText().contains("new label"));
        assertFalse(label.getToolTipText().contains("start"));
    }
}
