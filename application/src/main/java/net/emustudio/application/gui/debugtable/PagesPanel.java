/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.ToolbarButton;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class PagesPanel extends JPanel {
    private static final String PAGE_FIRST_PNG = "/net/emustudio/application/gui/dialogs/page-first.png";
    private static final String PAGE_BACK_PNG = "/net/emustudio/application/gui/dialogs/page-back.png";
    private static final String PAGE_CURRENT_PNG = "/net/emustudio/application/gui/dialogs/page-current.png";
    private static final String PAGE_FORWARD_PNG = "/net/emustudio/application/gui/dialogs/page-forward.png";
    private static final String PAGE_SEEK_BACKWARD_PNG = "/net/emustudio/application/gui/dialogs/page-seek-backward.png";
    private static final String PAGE_SEEK_FORWARD_PNG = "/net/emustudio/application/gui/dialogs/page-seek-forward.png";

    private final Dialogs dialogs;
    private final DebugTableModel debugTableModel;
    private int pageSeekLastValue = 10;

    private PagesPanel(DebugTableModel debugTableModel, Dialogs dialogs) {
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    public static PagesPanel create(DebugTableModel debugTableModel, Dialogs dialogs) {
        PagesPanel pagesPanel = new PagesPanel(debugTableModel, dialogs);
        pagesPanel.initComponents();

        return pagesPanel;
    }

    private void initComponents() {
        ToolbarButton btnFirst = GUI.toolbarButton(evt -> gotoFirstPage(), PAGE_FIRST_PNG, "Go to the first page");
        ToolbarButton btnBackward = GUI.toolbarButton(evt -> gotoPreviousPage(), PAGE_BACK_PNG, "Go to the previous page");
        ToolbarButton btnCurrentPage = GUI.toolbarButton(evt -> gotoCurrentPage(), PAGE_CURRENT_PNG, "Go to the current page");
        ToolbarButton btnForward = GUI.toolbarButton(evt -> gotoNextPage(), PAGE_FORWARD_PNG, "Go to the next page");
        ToolbarButton btnSeekBackward = GUI.toolbarButton(evt -> seekBackward(), PAGE_SEEK_BACKWARD_PNG, "Go to the current page");
        ToolbarButton btnSeekForward = GUI.toolbarButton(evt -> seekForward(), PAGE_SEEK_FORWARD_PNG, "Go to the current page");

        GroupLayout pagesLayout = new GroupLayout(this);
        setLayout(pagesLayout);
        pagesLayout.setHorizontalGroup(
                pagesLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(pagesLayout.createSequentialGroup()
                                .addComponent(btnFirst)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSeekBackward)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnBackward)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnCurrentPage)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnForward)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSeekForward))
        );
        pagesLayout.setVerticalGroup(
                pagesLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(pagesLayout.createSequentialGroup()
                                .addGroup(pagesLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(btnSeekBackward)
                                        .addComponent(btnBackward)
                                        .addComponent(btnFirst)
                                        .addComponent(btnCurrentPage)
                                        .addComponent(btnSeekForward)
                                        .addComponent(btnForward)))
        );
    }

    private void gotoFirstPage() {
        debugTableModel.firstPage();
    }

    private void gotoPreviousPage() {
        debugTableModel.previousPage();
    }

    private void gotoCurrentPage() {
        debugTableModel.currentPage();
    }

    private void gotoNextPage() {
        debugTableModel.nextPage();
    }

    private boolean gatherPageValue(String message) {
        Optional<Integer> result = dialogs.readInteger(message, "Seek", pageSeekLastValue);
        if (result.isPresent()) {
            pageSeekLastValue = result.get();
            return true;
        }
        return false;
    }

    private void seekBackward() {
        if (gatherPageValue("Please enter number of pages to backward")) {
            debugTableModel.seekBackwardPage(pageSeekLastValue);
        }
    }

    private void seekForward() {
        if (gatherPageValue("Please enter number of pages to forward")) {
            debugTableModel.seekForwardPage(pageSeekLastValue);
        }
    }
}
