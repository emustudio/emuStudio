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
package net.emustudio.plugins.memory.brainduck.gui;

import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class MemoryGui extends JDialog {
    private final MemoryTableModel tableModel;
    private final MemoryContext<Byte> memory;

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
        super(parent, false);
        setLocationRelativeTo(parent);
        initComponents();

        this.memory = Objects.requireNonNull(memory);
        this.tableModel = new MemoryTableModel(memory);
        MemoryTable table = new MemoryTable(tableModel, scrollPane);
        scrollPane.setViewportView(table);

        lblPageCount.setText(String.valueOf(tableModel.getPageCount()));

        memory.addMemoryListener(new MemoryListenerImpl());
    }


    private void initComponents() {
        scrollPane = new JScrollPane();
        JPanel jPanel1 = new JPanel();
        JButton btnPageDown = new JButton();
        JButton btnPageUp = new JButton();
        JLabel jLabel1 = new JLabel();
        JLabel jLabel2 = new JLabel();
        lblPageCount = new JLabel();
        txtPage = new JTextField();
        JButton btnClear = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("BrainDuck Memory");

        btnPageDown.setText("Page Down");
        btnPageDown.addActionListener(this::btnPageDownActionPerformed);

        btnPageUp.setText("Page Up");
        btnPageUp.setToolTipText("");
        btnPageUp.addActionListener(this::btnPageUpActionPerformed);

        jLabel1.setFont(jLabel1.getFont());
        jLabel1.setText("Page:");

        jLabel2.setText("/");

        lblPageCount.setText("0");

        txtPage.setText("0");
        txtPage.addActionListener(this::txtPageActionPerformed);

        btnClear.setText("Clear");
        btnClear.addActionListener(this::btnClearActionPerformed);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel1)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(txtPage, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel2)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(lblPageCount)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 332, Short.MAX_VALUE)
                    .addComponent(btnClear)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnPageUp)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnPageDown))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addGap(0, 12, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnPageDown)
                        .addComponent(btnPageUp)
                        .addComponent(jLabel1)
                        .addComponent(jLabel2)
                        .addComponent(lblPageCount)
                        .addComponent(txtPage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnClear)))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane)
                .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnPageUpActionPerformed(java.awt.event.ActionEvent evt) {
        int page = tableModel.getPage() - 1;
        if (page >= 0) {
            tableModel.setPage(page);
        }

        txtPage.setText(String.valueOf(tableModel.getPage()));
    }

    private void btnPageDownActionPerformed(java.awt.event.ActionEvent evt) {
        int page = tableModel.getPage() + 1;
        if (page < tableModel.getPageCount()) {
            tableModel.setPage(page);
        }

        txtPage.setText(String.valueOf(tableModel.getPage()));
    }

    private void txtPageActionPerformed(java.awt.event.ActionEvent evt) {
        int page = Integer.decode(evt.getActionCommand());
        if (page >= 0 && page < tableModel.getPageCount()) {
            tableModel.setPage(page);
        }

        txtPage.setText(String.valueOf(tableModel.getPage()));
    }

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {
        memory.clear();
        tableModel.fireTableDataChanged();
    }

    private JLabel lblPageCount;
    private JScrollPane scrollPane;
    private JTextField txtPage;
}
