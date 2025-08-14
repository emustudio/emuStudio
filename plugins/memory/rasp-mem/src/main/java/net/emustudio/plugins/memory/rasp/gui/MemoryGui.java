/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.interaction.ToolbarButton;
import net.emustudio.plugins.memory.rasp.MemoryContextImpl;
import net.emustudio.plugins.memory.rasp.gui.actions.DumpMemoryAction;
import net.emustudio.plugins.memory.rasp.gui.actions.EraseMemoryAction;
import net.emustudio.plugins.memory.rasp.gui.actions.LoadImageAction;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiConstants.FONT_MONOSPACED;

public class MemoryGui extends JDialog {
    private final JTable table;

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final EraseMemoryAction eraseMemoryAction;

    public MemoryGui(JFrame parent, MemoryContextImpl context, ApplicationApi api) {
        super(parent, false);

        MemoryContextImpl memory = Objects.requireNonNull(context);
        RaspTableModel tableModel = new RaspTableModel(memory);
        this.table = new JTable(tableModel);
        this.table.setFont(FONT_MONOSPACED);

        this.loadImageAction = new LoadImageAction(api.getDialogs(), context, () -> {
            table.revalidate();
            table.repaint();
        }, api::setProgramLocation);
        this.dumpMemoryAction = new DumpMemoryAction(api.getDialogs(), context, api::getProgramLocation);
        this.eraseMemoryAction = new EraseMemoryAction(tableModel, context);

        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JScrollPane jScrollPane1 = new JScrollPane();
        JToolBar toolBar = new JToolBar();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("RASP Memory");

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                memoryTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(0).setResizable(false);
            table.getColumnModel().getColumn(1).setResizable(false);
        }

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
                        .addComponent(toolBar, GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 491, Short.MAX_VALUE))
        );

        pack();
    }

    /**
     * Called when user double-clicks at a row; editor is displayed at the
     * "Numeric value" column. It is to make UI more user friendly as user does
     * not have to click at an editable cell, he/she just double-clicks the row.
     *
     * @param evt the click event
     */
    private void memoryTableMouseClicked(java.awt.event.MouseEvent evt) {
        int row = table.rowAtPoint(evt.getPoint());
        //check if double-click
        if (evt.getClickCount() == 2) {
            table.editCellAt(row, 1);
        }
    }
}
