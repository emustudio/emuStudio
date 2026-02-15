/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.ToolbarButton;

import javax.swing.*;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioUI.*;

public class PagesPanel extends JPanel {

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
        ToolbarButton btnFirst = GUI.toolbarButton(evt -> debugTableModel.firstPage(), ICON_PAGE_FIRST, "Go to the first page");
        ToolbarButton btnBackward = GUI.toolbarButton(evt -> debugTableModel.previousPage(), ICON_PAGE_BACK, "Go to the previous page");
        ToolbarButton btnCurrentPage = GUI.toolbarButton(evt -> debugTableModel.currentPage(), ICON_PAGE_CURRENT, "Go to the current page");
        ToolbarButton btnForward = GUI.toolbarButton(evt -> debugTableModel.nextPage(), ICON_PAGE_FORWARD, "Go to the next page");
        ToolbarButton btnSeekBackward = GUI.toolbarButton(evt -> seekBackward(), ICON_PAGE_SEEK_BACKWARD, "Go to the current page");
        ToolbarButton btnSeekForward = GUI.toolbarButton(evt -> seekForward(), ICON_PAGE_SEEK_FORWARD, "Go to the current page");

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
