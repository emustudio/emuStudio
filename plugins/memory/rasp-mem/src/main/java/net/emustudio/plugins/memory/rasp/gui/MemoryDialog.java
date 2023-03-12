/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
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

package net.emustudio.plugins.memory.rasp.gui;

import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.memory.rasp.MemoryContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MemoryDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryDialog.class);

    private final ApplicationApi api;
    private final Dialogs dialogs;

    private final MemoryContextImpl memory;
    private final RaspTableModel tableModel;
    private File recentOpenPath;
    private javax.swing.JTable memoryTable;

    public MemoryDialog(JFrame parent, MemoryContextImpl context, ApplicationApi api) {
        super(parent, false);

        this.api = Objects.requireNonNull(api);
        this.dialogs = Objects.requireNonNull(api.getDialogs());
        this.memory = Objects.requireNonNull(context);
        this.recentOpenPath = new File(System.getProperty("user.home"));

        initComponents();
        setLocationRelativeTo(parent);

        tableModel = new RaspTableModel(memory);
        updateTable();
        memory.addMemoryListener(new Memory.MemoryListener() {

            @Override
            public void memoryChanged(int position) {
                updateTable();
            }

            @Override
            public void memorySizeChanged() {
                updateTable();
            }
        });
    }

    private void updateTable() {
        tableModel.fireTableDataChanged();
        repaint();
    }

    private void initComponents() {
        JScrollPane jScrollPane1 = new JScrollPane();
        memoryTable = new javax.swing.JTable();
        JToolBar jToolBar1 = new JToolBar();
        JButton jButton1 = new JButton();
        JButton jButton2 = new JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("RASP Memory");

        memoryTable.setModel(new RaspTableModel(memory));
        memoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                memoryTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(memoryTable);
        if (memoryTable.getColumnModel().getColumnCount() > 0) {
            memoryTable.getColumnModel().getColumn(0).setResizable(false);
            memoryTable.getColumnModel().getColumn(1).setResizable(false);
        }

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/rasp/gui/document-open.png")));
        jButton1.setFocusable(false);
        jButton1.addActionListener(this::onOpenClick);
        jToolBar1.add(jButton1);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/rasp/gui/edit-delete.png")));
        jButton2.setFocusable(false);
        jButton2.addActionListener(this::onClearClick);
        jToolBar1.add(jButton2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE))
        );

        pack();
    }

    private void onOpenClick(java.awt.event.ActionEvent evt) {
        File currentDirectory = Objects.requireNonNullElse(recentOpenPath, new File(System.getProperty("user.dir")));
        dialogs.chooseFile(
                "Load compiled RASP program", "Load", currentDirectory.toPath(), false,
                new FileExtensionsFilter("RASP compiler file", "brasp")
        ).ifPresent(path -> {
            recentOpenPath = path.toFile().getParentFile();
            try {
                memory.deserialize(path.toString(), api);
                updateTable();
            } catch (IOException | ClassNotFoundException ex) {
                LOGGER.error("Could not read file: {}", path, ex);
                dialogs.showError("Could not open file " + path + ". Please see log file for details.");
            }
        });
    }

    private void onClearClick(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onClearClick
        memory.clear();
    }

    /**
     * Called when user double-clicks at a row; editor is displayed at the
     * "Numeric value" column. It is to make UI more user friendly as user does
     * not have to click at an editable cell, he/she just double-clicks the row.
     *
     * @param evt the click event
     */
    private void memoryTableMouseClicked(java.awt.event.MouseEvent evt) {
        int row = memoryTable.rowAtPoint(evt.getPoint());
        //check if double-click
        if (evt.getClickCount() == 2) {
            memoryTable.editCellAt(row, 1);
        }
    }
}
