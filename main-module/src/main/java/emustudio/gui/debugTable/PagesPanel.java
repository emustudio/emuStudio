/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui.debugTable;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;

public class PagesPanel extends JPanel {
    private static final String PAGE_FIRST_PNG = "/emustudio/gui/page-first.png";
    private static final String PAGE_BACK_PNG = "/emustudio/gui/page-back.png";
    private static final String PAGE_CURRENT_PNG = "/emustudio/gui/page-current.png";
    private static final String PAGE_FORWARD_PNG = "/emustudio/gui/page-forward.png";
    private static final String PAGE_LAST_PNG = "/emustudio/gui/page-last.png";
    private static final String PAGE_SEEK_BACKWARD_PNG = "/emustudio/gui/page-seek-backward.png";
    private static final String PAGE_SEEK_FORWARD_PNG = "/emustudio/gui/page-seek-forward.png";

    private final DebugTableImpl debugTable;
    private int pageSeekLastValue = 10;

    private PagesPanel(DebugTableImpl debugTable) {
        this.debugTable = debugTable;
    }

    private void initComponents() {
        JButton btnFirst =  new JButton();
        JButton btnBackward = new JButton();
        JButton btnCurrentPage =  new JButton();
        JButton btnForward =  new JButton();
        JButton btnLast =  new JButton();
        JButton btnSeekBackward =  new JButton();
        JButton btnSeekForward =  new JButton();

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
        debugTable.getModel().firstPage();
    }

    private void gotoPreviousPage() {
        debugTable.getModel().previousPage();
    }

    private void gotoCurrentPage() {
        debugTable.getModel().currentPage();
    }

    private void gotoNextPage() {
        debugTable.getModel().nextPage();
    }

    private void gotoLastPage() {
        debugTable.getModel().lastPage();
    }

    private boolean gatherPageValue(String message) {
        String res = JOptionPane.showInputDialog(this, message, pageSeekLastValue);
        if (res != null && !res.isEmpty()) {
            pageSeekLastValue = Integer.decode(res);
            return true;
        }
        return false;
    }

    private void seekBackward() {
        if (gatherPageValue("Please enter number of pages to backward")) {
            debugTable.getModel().seekBackwardPage(pageSeekLastValue);
        }
    }

    private void seekForward() {
        if (gatherPageValue("Please enter number of pages to forward")) {
            debugTable.getModel().seekForwardPage(pageSeekLastValue);
        }
    }

    public static PagesPanel create(DebugTableImpl debugTable) {
        PagesPanel pagesPanel = new PagesPanel(debugTable);
        pagesPanel.initComponents();

        return pagesPanel;
    }
}
