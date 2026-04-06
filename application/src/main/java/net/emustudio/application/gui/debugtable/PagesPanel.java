/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioGui.*;

public class PagesPanel extends JPanel {

    private final Dialogs dialogs;
    private final DebugTableModel debugTableModel;
    private final GUI gui;
    private int pageSeekLastValue = 10;

    private PagesPanel(DebugTableModel debugTableModel, Dialogs dialogs, GUI gui) {
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);
    }

    public static PagesPanel create(DebugTableModel debugTableModel, Dialogs dialogs, GUI gui) {
        PagesPanel pagesPanel = new PagesPanel(debugTableModel, dialogs, gui);
        pagesPanel.initComponents();

        return pagesPanel;
    }

    private void initComponents() {
        JButton btnFirst = gui.toolbarButton(evt -> debugTableModel.firstPage(), ICON_PAGE_FIRST, "Go to the first page");
        JButton btnBackward = gui.toolbarButton(evt -> debugTableModel.previousPage(), ICON_PAGE_BACK, "Go to the previous page");
        JButton btnCurrentPage = gui.toolbarButton(evt -> debugTableModel.currentPage(), ICON_PAGE_CURRENT, "Go to the current page");
        JButton btnForward = gui.toolbarButton(evt -> debugTableModel.nextPage(), ICON_PAGE_FORWARD, "Go to the next page");
        JButton btnSeekBackward = gui.toolbarButton(evt -> seekBackward(), ICON_PAGE_SEEK_BACKWARD, "Go to the current page");
        JButton btnSeekForward = gui.toolbarButton(evt -> seekForward(), ICON_PAGE_SEEK_FORWARD, "Go to the current page");

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

    private void seekBackward() {
        dialogs.readInteger("Please enter number of pages to backward", "Seek", pageSeekLastValue)
                .ifPresent(value -> {
                    pageSeekLastValue = value;
                    debugTableModel.seekBackwardPage(value);
                });
    }

    private void seekForward() {
        dialogs.readInteger("Please enter number of pages to forward", "Seek", pageSeekLastValue)
                .ifPresent(value -> {
                    pageSeekLastValue = value;
                    debugTableModel.seekForwardPage(value);
                });
    }
}
