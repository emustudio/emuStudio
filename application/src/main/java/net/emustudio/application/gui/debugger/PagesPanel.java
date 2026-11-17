/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Objects;
import java.util.function.IntConsumer;


public class PagesPanel extends JPanel {

    private final Dialogs dialogs;
    private final DebugTableModel debugTableModel;
    private final EmuStudioGui gui;
    private final JToolBar tools;
    private int pageSeekLastValue = 10;

    private PagesPanel(DebugTableModel debugTableModel, Dialogs dialogs, EmuStudioGui gui) {
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);
        this.tools = gui.toolBar();
        tools.setLayout(new MigLayout("insets 0, hidemode 3, gapx 2", "", ""));
    }

    public static PagesPanel create(DebugTableModel debugTableModel, Dialogs dialogs, EmuStudioGui gui) {
        PagesPanel pagesPanel = new PagesPanel(debugTableModel, dialogs, gui);
        pagesPanel.initComponents();

        return pagesPanel;
    }

    public JToolBar toolbar() {
        return tools;
    }

    private void initComponents() {
        JButton btnFirst = gui.toolbarButton(evt -> debugTableModel.firstPage(), "/net/emustudio/application/gui/icons/page-first.png", "Go to the first page");
        JButton btnBackward = gui.toolbarButton(evt -> debugTableModel.previousPage(), "/net/emustudio/application/gui/icons/page-back.png", "Go to the previous page");
        JButton btnCurrentPage = gui.toolbarButton(evt -> debugTableModel.currentPage(), "/net/emustudio/application/gui/icons/page-current.png", "Go to the current page");
        JButton btnForward = gui.toolbarButton(evt -> debugTableModel.nextPage(), "/net/emustudio/application/gui/icons/run.png", "Go to the next page");
        JButton btnSeekBackward = gui.toolbarButton(evt -> seek("backward", debugTableModel::seekBackwardPage), "/net/emustudio/application/gui/icons/page-seek-backward.png", "Seek pages backward");
        JButton btnSeekForward = gui.toolbarButton(evt -> seek("forward", debugTableModel::seekForwardPage), "/net/emustudio/application/gui/icons/page-seek-forward.png", "Seek pages forward");
        JToggleButton fold = gui.toolbarToggleButton(evt -> {},
                "/net/emustudio/application/gui/icons/emulation/fold.png", "Fold identical repeats");
        fold.setSelected(debugTableModel.isFoldRepeats());
        fold.addActionListener(evt -> {
            debugTableModel.setFoldRepeats(fold.isSelected());
            fold.setSelected(debugTableModel.isFoldRepeats());
        });
        debugTableModel.addTableModelListener(evt -> fold.setSelected(debugTableModel.isFoldRepeats()));

        tools.add(btnFirst);
        tools.add(btnSeekBackward);
        tools.add(btnBackward);
        tools.add(btnCurrentPage);
        tools.add(btnForward);
        tools.add(btnSeekForward);
        tools.addSeparator();
        tools.add(fold);

        setLayout(new MigLayout("insets 0, fillx", "[grow]", "[]"));
        add(tools, "growx");
    }

    private void seek(String direction, IntConsumer seekPages) {
        dialogs.readInteger("Please enter number of pages to " + direction, "Seek", pageSeekLastValue)
                .ifPresent(value -> {
                    pageSeekLastValue = value;
                    seekPages.accept(value);
                });
    }
}
