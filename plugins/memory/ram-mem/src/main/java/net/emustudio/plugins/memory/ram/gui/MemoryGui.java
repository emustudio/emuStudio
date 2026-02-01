/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.gui.actions.DumpMemoryAction;
import net.emustudio.plugins.memory.ram.gui.actions.EraseMemoryAction;
import net.emustudio.plugins.memory.ram.gui.actions.LoadImageAction;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.event.KeyEvent;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_COMMON;
import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

public class MemoryGui extends JDialog {
    private final JTable table;

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final EraseMemoryAction eraseMemoryAction;

    public MemoryGui(JFrame parent, MemoryContextImpl memory, Dialogs dialogs) {
        super(parent, "Program memory", false);

        RamTableModel tableModel = new RamTableModel(memory);
        this.table = new JTable(tableModel);
        this.table.setFont(FONT_MONOSPACED);

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
        JToolBar toolBar = GUI.toolBar();
        JPanel jPanel1 = new JPanel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        toolBar.add(GUI.toolbarButton(loadImageAction));
        toolBar.add(GUI.toolbarButton(dumpMemoryAction));
        toolBar.addSeparator();
        toolBar.add(GUI.toolbarButton(eraseMemoryAction));

        jPanel1.setBorder(BorderFactory.createTitledBorder(null, "Tape content", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, FONT_COMMON));

        table.setGridColor(java.awt.SystemColor.control);
        JScrollPane jScrollPane1 = GUI.scrollPane(table);

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
