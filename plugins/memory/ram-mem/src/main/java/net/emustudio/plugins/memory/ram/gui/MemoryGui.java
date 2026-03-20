/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.gui.actions.DumpMemoryAction;
import net.emustudio.plugins.memory.ram.gui.actions.EraseMemoryAction;
import net.emustudio.plugins.memory.ram.gui.actions.LoadImageAction;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;

public class MemoryGui extends DialogBase {
    private final GUI gui;
    private final JTable table;

    private final LoadImageAction loadImageAction;
    private final DumpMemoryAction dumpMemoryAction;
    private final EraseMemoryAction eraseMemoryAction;

    public MemoryGui(JFrame parent, MemoryContextImpl memory, Dialogs dialogs, GUI gui) {
        super(parent, "Program memory", false);
        this.gui = gui;

        RamTableModel tableModel = new RamTableModel(memory);
        this.table = new JTable(tableModel);
        this.table.setFont(FONT_MONOSPACED);

        this.loadImageAction = new LoadImageAction(dialogs, memory, () -> {
            table.revalidate();
            table.repaint();
        });
        this.dumpMemoryAction = new DumpMemoryAction(dialogs, memory);
        this.eraseMemoryAction = new EraseMemoryAction(tableModel, memory);

        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JToolBar toolBar = gui.toolBar();
        toolBar.add(gui.toolbarButton(loadImageAction));
        toolBar.add(gui.toolbarButton(dumpMemoryAction));
        toolBar.addSeparator();
        toolBar.add(gui.toolbarButton(eraseMemoryAction));

        table.setGridColor(SystemColor.control);
        JScrollPane scrollPane = gui.scrollPane(table);
        scrollPane.setPreferredSize(new Dimension(439, 456));

        JPanel panelContent = gui.section("Tape content", "insets dialog", "[grow]", "[grow]");
        panelContent.add(scrollPane, "grow");

        JPanel content = gui.panel("insets 0", "[grow]", "[]6[grow]");
        content.add(toolBar, "growx, wrap");
        content.add(panelContent, "grow");
        return content;
    }
}
