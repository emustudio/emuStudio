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
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.ToolbarButton;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.gui.actions.DumpMemoryAction;
import net.emustudio.plugins.memory.ram.gui.actions.EraseMemoryAction;
import net.emustudio.plugins.memory.ram.gui.actions.LoadImageAction;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.KeyEvent;

import static net.emustudio.plugins.memory.ram.gui.Constants.DIALOG_PLAIN;
import static net.emustudio.plugins.memory.ram.gui.Constants.MONOSPACED_PLAIN;

public class MemoryGui extends JDialog {
    private final JTable table;

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final EraseMemoryAction eraseMemoryAction;

    public MemoryGui(JFrame parent, MemoryContextImpl memory, Dialogs dialogs) {
        super(parent, false);

        RamTableModel tableModel = new RamTableModel(memory);
        this.table = new JTable(tableModel);
        this.table.setFont(MONOSPACED_PLAIN);

        this.loadImageAction = new LoadImageAction(dialogs, memory, () -> {
            table.revalidate();
            table.repaint();
        });
        this.dumpMemoryAction = new DumpMemoryAction(dialogs, memory);
        this.eraseMemoryAction = new EraseMemoryAction(tableModel, memory);

        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JToolBar toolBar = new JToolBar();
        JPanel jPanel1 = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Program memory");

        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(new ToolbarButton(loadImageAction));
        toolBar.add(new ToolbarButton(dumpMemoryAction));
        toolBar.addSeparator();
        toolBar.add(new ToolbarButton(eraseMemoryAction));

        jPanel1.setBorder(BorderFactory.createTitledBorder(null, "Tape content", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, DIALOG_PLAIN));

        table.setGridColor(java.awt.SystemColor.control);
        jScrollPane1.setViewportView(table);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(toolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel1));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel1)));

        pack();
    }
}
