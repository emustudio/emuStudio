/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static net.emustudio.plugins.memory.ram.gui.Constants.DIALOG_PLAIN;

public class MemoryDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryDialog.class);

    private final Dialogs dialogs;

    private final MemoryContextImpl memory;
    private final RAMTableModel tableModel;
    private File lastOpenedFile;
    private JTable tableProgram;

    public MemoryDialog(JFrame parent, MemoryContextImpl memory, Dialogs dialogs) {
        super(parent, false);

        this.dialogs = Objects.requireNonNull(dialogs);
        this.memory = Objects.requireNonNull(memory);

        initComponents();
        setLocationRelativeTo(parent);

        tableModel = new RAMTableModel(memory);
        tableProgram.setModel(tableModel);
    }

    private void openRAM() {
        File currentDirectory = Objects.requireNonNullElse(lastOpenedFile, new File(System.getProperty("user.dir")));
        dialogs.chooseFile("Load compiled RAM program", "Load", currentDirectory.toPath(), false, new FileExtensionsFilter("RAM compiler file", "ro")).ifPresent(path -> {
            lastOpenedFile = path.toFile();
            try {
                memory.deserialize(lastOpenedFile.getAbsolutePath());
                tableModel.fireTableDataChanged();
            } catch (IOException | ClassNotFoundException e) {
                dialogs.showError("Cannot open file " + lastOpenedFile.getPath() + ". Please see log file for details.");
                LOGGER.error("Could not open file {}", lastOpenedFile, e);
            }
        });
    }

    private void initComponents() {
        JToolBar jToolBar1 = new JToolBar();
        JButton btnOpen = new JButton();
        JButton btnClear = new JButton();
        JPanel jPanel1 = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        tableProgram = new JTable();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Program memory");

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnOpen.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/ram/gui/document-open.png")));
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnOpen.addActionListener(this::btnOpenActionPerformed);
        jToolBar1.add(btnOpen);

        btnClear.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/ram/gui/edit-delete.png")));
        btnClear.setFocusable(false);
        btnClear.setHorizontalTextPosition(SwingConstants.CENTER);
        btnClear.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnClear.addActionListener(this::btnClearActionPerformed);
        jToolBar1.add(btnClear);

        jPanel1.setBorder(BorderFactory.createTitledBorder(null, "Tape content", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, DIALOG_PLAIN));

        tableProgram.setGridColor(java.awt.SystemColor.control);
        jScrollPane1.setViewportView(tableProgram);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jToolBar1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel1));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jToolBar1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel1)));

        pack();
    }

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {
        openRAM();
    }

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {
        memory.clear();
    }
}
