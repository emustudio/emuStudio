/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.memory.rasp.MemoryContextImpl;
import net.emustudio.plugins.memory.rasp.gui.actions.DumpMemoryAction;
import net.emustudio.plugins.memory.rasp.gui.actions.EraseMemoryAction;
import net.emustudio.plugins.memory.rasp.gui.actions.LoadImageAction;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

public class MemoryGui extends DialogBase {
    private final JTable table;

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final EraseMemoryAction eraseMemoryAction;

    public MemoryGui(JFrame parent, MemoryContextImpl context, ApplicationApi api) {
        super(parent, "RASP Memory", false);

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

        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JScrollPane scrollPane = new JScrollPane();

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                memoryTableMouseClicked(evt);
            }
        });
        scrollPane.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(0).setResizable(false);
            table.getColumnModel().getColumn(1).setResizable(false);
        }
        scrollPane.setPreferredSize(new Dimension(265, 491));

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

    /**
     * Called when user double-clicks at a row; editor is displayed at the
     * "Numeric value" column. It is to make UI more user friendly as user does
     * not have to click at an editable cell, he/she just double-clicks the row.
     *
     * @param evt the click event
     */
    private void memoryTableMouseClicked(java.awt.event.MouseEvent evt) {
        int row = table.rowAtPoint(evt.getPoint());
        if (evt.getClickCount() == 2) {
            table.editCellAt(row, 1);
        }
    }
}
