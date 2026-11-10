/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Objects;
import java.util.function.IntConsumer;

import static net.emustudio.emulib.ui.StandardIcons.*;

public class PagesPanel extends JPanel {

    private final Dialogs dialogs;
    private final DebugTableModel debugTableModel;
    private final EmuStudioGui gui;
    private int pageSeekLastValue = 10;

    private PagesPanel(DebugTableModel debugTableModel, Dialogs dialogs, EmuStudioGui gui) {
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);
    }

    public static PagesPanel create(DebugTableModel debugTableModel, Dialogs dialogs, EmuStudioGui gui) {
        PagesPanel pagesPanel = new PagesPanel(debugTableModel, dialogs, gui);
        pagesPanel.initComponents();

        return pagesPanel;
    }

    private void initComponents() {
        JButton btnFirst = gui.toolbarButton(evt -> debugTableModel.firstPage(), ICON_PAGE_FIRST, "Go to the first page");
        JButton btnBackward = gui.toolbarButton(evt -> debugTableModel.previousPage(), ICON_PAGE_BACK, "Go to the previous page");
        JButton btnCurrentPage = gui.toolbarButton(evt -> debugTableModel.currentPage(), ICON_PAGE_CURRENT, "Go to the current page");
        JButton btnForward = gui.toolbarButton(evt -> debugTableModel.nextPage(), ICON_PAGE_FORWARD, "Go to the next page");
        JButton btnSeekBackward = gui.toolbarButton(evt -> seek("backward", debugTableModel::seekBackwardPage), ICON_PAGE_SEEK_BACKWARD, "Seek pages backward");
        JButton btnSeekForward = gui.toolbarButton(evt -> seek("forward", debugTableModel::seekForwardPage), ICON_PAGE_SEEK_FORWARD, "Seek pages forward");

        JPanel buttonPanel = gui.panel("insets 0, center", "[]0[]0[]0[]0[]0[]", "[]");
        buttonPanel.add(btnFirst);
        buttonPanel.add(btnSeekBackward);
        buttonPanel.add(btnBackward);
        buttonPanel.add(btnCurrentPage);
        buttonPanel.add(btnForward);
        buttonPanel.add(btnSeekForward);

        setLayout(new MigLayout("insets 0, fillx", "[grow]", "[]"));
        add(buttonPanel, "center");
    }

    private void seek(String direction, IntConsumer seekPages) {
        dialogs.readInteger("Please enter number of pages to " + direction, "Seek", pageSeekLastValue)
                .ifPresent(value -> {
                    pageSeekLastValue = value;
                    seekPages.accept(value);
                });
    }
}
