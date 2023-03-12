/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ssem.gui;

import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.ToolbarButton;
import net.emustudio.plugins.memory.ssem.gui.actions.DumpMemoryAction;
import net.emustudio.plugins.memory.ssem.gui.actions.EraseMemoryAction;
import net.emustudio.plugins.memory.ssem.gui.actions.LoadImageAction;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class MemoryGui extends JDialog {
    private final MemoryTableModel tableModel;
    private final JScrollPane scrollPane = new JScrollPane();

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final EraseMemoryAction eraseMemoryAction;

    public MemoryGui(JFrame parent, MemoryContext<Byte> memory, Dialogs dialogs) {
        super(parent);

        this.tableModel = new MemoryTableModel(memory);
        MemoryTable table = new MemoryTable(tableModel, scrollPane);

        this.loadImageAction = new LoadImageAction(dialogs, memory, () -> {
            table.revalidate();
            table.repaint();
        });
        this.dumpMemoryAction = new DumpMemoryAction(dialogs, memory);
        this.eraseMemoryAction = new EraseMemoryAction(tableModel, memory);

        initComponents();
        setLocationRelativeTo(parent);

        table.setup();
        scrollPane.setViewportView(table);

        memory.addMemoryListener(new MemoryListenerImpl());
    }

    private void initComponents() {
        JToolBar toolBar = new JToolBar();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("SSEM Memory (Williams–Kilburn Tube)");

        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(new ToolbarButton(loadImageAction));
        toolBar.add(new ToolbarButton(dumpMemoryAction));
        toolBar.addSeparator();
        toolBar.add(new ToolbarButton(eraseMemoryAction));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 965, Short.MAX_VALUE)
                        .addComponent(toolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE))
        );

        pack();
    }

    private class MemoryListenerImpl implements Memory.MemoryListener {

        @Override
        public void memoryChanged(int memoryPosition) {
            tableModel.dataChangedAt(memoryPosition);
        }

        @Override
        public void memorySizeChanged() {
            tableModel.fireTableDataChanged();
        }
    }
}
