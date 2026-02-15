/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.memory.ssem.gui.actions.DumpMemoryAction;
import net.emustudio.plugins.memory.ssem.gui.actions.EraseMemoryAction;
import net.emustudio.plugins.memory.ssem.gui.actions.LoadImageAction;
import net.emustudio.plugins.memory.ssem.gui.table.MemoryTable;
import net.emustudio.plugins.memory.ssem.gui.table.MemoryTableModel;

import javax.swing.*;
import java.awt.*;

public class MemoryGui extends DialogBase {
    private final MemoryTableModel tableModel;
    private final JScrollPane scrollPane = new JScrollPane();

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final EraseMemoryAction eraseMemoryAction;

    public MemoryGui(JFrame parent, MemoryContext<Byte> memory, ApplicationApi api) {
        super(parent, "SSEM Memory (Williams–Kilburn Tube)", false);

        this.tableModel = new MemoryTableModel(memory);
        MemoryTable table = new MemoryTable(tableModel, scrollPane);

        this.loadImageAction = new LoadImageAction(api, memory, () -> {
            table.revalidate();
            table.repaint();
        });
        this.dumpMemoryAction = new DumpMemoryAction(api, memory);
        this.eraseMemoryAction = new EraseMemoryAction(tableModel, memory);

        scrollPane.setViewportView(table);
        scrollPane.setPreferredSize(new Dimension(965, 455));
        memory.addMemoryListener(new MemoryListenerImpl());
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(GUI.toolbarButton(loadImageAction));
        toolBar.add(GUI.toolbarButton(dumpMemoryAction));
        toolBar.addSeparator();
        toolBar.add(GUI.toolbarButton(eraseMemoryAction));

        JPanel content = GUI.panel("insets 0", "[grow]", "[]6[grow]");
        content.add(toolBar, "growx, wrap");
        content.add(scrollPane, "grow");
        return content;
    }

    private class MemoryListenerImpl implements MemoryContext.MemoryListener {
        @Override
        public void memoryContentChanged(int fromLocation, int toLocation) {
            tableModel.dataChangedAt(fromLocation, toLocation);
        }

        @Override
        public void memorySizeChanged() {
            tableModel.fireTableDataChanged();
        }
    }
}
