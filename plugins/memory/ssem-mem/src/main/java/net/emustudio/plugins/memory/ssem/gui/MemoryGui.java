/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

import javax.swing.*;
import java.awt.event.KeyEvent;

public class MemoryGui extends JDialog {
    private final MemoryTableModel tableModel;

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

    public MemoryGui(JFrame parent, MemoryContext<Byte> memory) {
        super(parent);

        initComponents();
        setLocationRelativeTo(parent);

        this.tableModel = new MemoryTableModel(memory);
        MemoryTable memoryTable = new MemoryTable(tableModel, scrollPane);
        memoryTable.setup();
        scrollPane.setViewportView(memoryTable);

        memory.addMemoryListener(new MemoryListenerImpl());
    }

    private void initComponents() {
        scrollPane = new JScrollPane();
        JToolBar jToolBar1 = new JToolBar();
        JButton btnClear = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("SSEM Memory (Williams-Killburn Tube)");

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnClear.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/ssem/gui/clear.png")));
        btnClear.setFocusable(false);
        btnClear.setHorizontalTextPosition(SwingConstants.CENTER);
        btnClear.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnClear.addActionListener(this::btnClearActionPerformed);
        jToolBar1.add(btnClear);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 965, Short.MAX_VALUE)
                .addComponent(jToolBar1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addComponent(jToolBar1, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {
        tableModel.clear();
    }

    private JScrollPane scrollPane;
}
