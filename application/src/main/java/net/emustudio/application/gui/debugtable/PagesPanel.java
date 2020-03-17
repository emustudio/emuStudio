/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class PagesPanel extends JPanel {
    private static final String PAGE_FIRST_PNG = "/net/emustudio/application/gui/dialogs/page-first.png";
    private static final String PAGE_BACK_PNG = "/net/emustudio/application/gui/dialogs/page-back.png";
    private static final String PAGE_CURRENT_PNG = "/net/emustudio/application/gui/dialogs/page-current.png";
    private static final String PAGE_FORWARD_PNG = "/net/emustudio/application/gui/dialogs/page-forward.png";
    private static final String PAGE_LAST_PNG = "/net/emustudio/application/gui/dialogs/page-last.png";
    private static final String PAGE_SEEK_BACKWARD_PNG = "/net/emustudio/application/gui/dialogs/page-seek-backward.png";
    private static final String PAGE_SEEK_FORWARD_PNG = "/net/emustudio/application/gui/dialogs/page-seek-forward.png";

    private final Dialogs dialogs;
    private final DebugTableModel debugTableModel;
    private int pageSeekLastValue = 10;

    private PagesPanel(DebugTableModel debugTableModel, Dialogs dialogs) {
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    private void initComponents() {
        JButton btnFirst = new JButton();
        JButton btnBackward = new JButton();
        JButton btnCurrentPage = new JButton();
        JButton btnForward = new JButton();
        JButton btnLast = new JButton();
        JButton btnSeekBackward = new JButton();
        JButton btnSeekForward = new JButton();

        btnFirst.setIcon(new ImageIcon(getClass().getResource(PAGE_FIRST_PNG)));
        btnFirst.setToolTipText("Go to the first page");

        btnBackward.setIcon(new ImageIcon(getClass().getResource(PAGE_BACK_PNG)));
        btnBackward.setToolTipText("Go to the previous page");

        btnCurrentPage.setIcon(new ImageIcon(getClass().getResource(PAGE_CURRENT_PNG)));
        btnCurrentPage.setToolTipText("Go to the current page");

        btnForward.setIcon(new ImageIcon(getClass().getResource(PAGE_FORWARD_PNG)));
        btnForward.setToolTipText("Go to the next page");

        btnLast.setIcon(new ImageIcon(getClass().getResource(PAGE_LAST_PNG)));
        btnLast.setToolTipText("Go to the last page");

        btnSeekBackward.setIcon(new ImageIcon(getClass().getResource(PAGE_SEEK_BACKWARD_PNG)));
        btnSeekBackward.setToolTipText("Go to the current page");

        btnSeekForward.setIcon(new ImageIcon(getClass().getResource(PAGE_SEEK_FORWARD_PNG)));
        btnSeekForward.setToolTipText("Go to the current page");

        btnFirst.addActionListener(evt -> gotoFirstPage());
        btnBackward.addActionListener(evt -> gotoPreviousPage());
        btnForward.addActionListener(evt -> gotoNextPage());
        btnCurrentPage.addActionListener(evt -> gotoCurrentPage());
        btnLast.addActionListener(evt -> gotoLastPage());
        btnSeekBackward.addActionListener(evt -> seekBackward());
        btnSeekForward.addActionListener(evt -> seekForward());

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
                    .addComponent(btnSeekForward)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnLast))
        );
        pagesLayout.setVerticalGroup(
            pagesLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addGroup(pagesLayout.createSequentialGroup()
                    .addGroup(pagesLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(btnLast)
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

    private void gotoLastPage() {
        debugTableModel.lastPage();
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

    public static PagesPanel create(DebugTableModel debugTableModel, Dialogs dialogs) {
        PagesPanel pagesPanel = new PagesPanel(debugTableModel, dialogs);
        pagesPanel.initComponents();

        return pagesPanel;
    }
}
