/*
 * FindDialog.java
 *
 * Created on 1.4.2009, 11:26
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012, Peter Jakubčo
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
package emustudio.gui;

import emustudio.gui.utils.FindText;
import emustudio.gui.utils.NiceButton;
import emustudio.main.Main;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;
import javax.swing.event.ListDataListener;

/**
 * The find dialog. It searches for text within the source code editor.
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class FindDialog extends javax.swing.JDialog {
    private static final ArrayList<String> list = new ArrayList<>();
    private static final ArrayList<String> rlist = new ArrayList<>();
    private JTextPane textPane;

    private class CMBModel implements ComboBoxModel {

        private int in = -1;
        private final ArrayList<String> clist;

        public CMBModel(ArrayList<String> clist) {
            this.clist = clist;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            in = clist.indexOf(anItem);
        }

        @Override
        public Object getSelectedItem() {
            if (in != -1) {
                return clist.get(in);
            } else {
                return null;
            }
        }

        @Override
        public int getSize() {
            return clist.size();
        }

        @Override
        public Object getElementAt(int index) {
            return clist.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
        }
    }

    /**
     * Creates new find dialog instance.
     *
     * @param parent parent frame
     * @param modal whether this dialog should be modal
     * @param pane an object containing the all text, where the search process
     * will be executed.
     */
    public FindDialog(StudioFrame parent, boolean modal, JTextPane pane) {
        super(parent, modal);
        this.textPane = pane;
        initComponents();

        switch (FindText.getInstance().getDirection()) {
            case FindText.DIRECTION_TO_END:
                endRadio.setSelected(true);
                break;
            case FindText.DIRECTION_TO_START:
                startRadio.setSelected(true);
                break;
            case FindText.DIRECTION_ALL:
                allRadio.setSelected(true);
                break;
        }
        caseCheck.setSelected(FindText.getInstance().isCaseSensitive());
        wholeCheck.setSelected(FindText.getInstance().isWholeWords());

        cmbSearch.setModel(new CMBModel(list));
        cmbReplace.setModel(new CMBModel(rlist));

        String str = FindText.getInstance().getFindExpr();
        String rstr = FindText.getInstance().replacement;
        for (int i = 0; i < list.size(); i++) {
            if (((String) list.get(i)).equals(str)) {
                cmbSearch.setSelectedIndex(i);
                cmbSearch.getEditor().setItem(str);
                break;
            }
        }
        for (int i = 0; i < rlist.size(); i++) {
            if (((String) rlist.get(i)).equals(rstr)) {
                cmbReplace.setSelectedIndex(i);
                cmbReplace.getEditor().setItem(rstr);
                break;
            }
        }
        this.setLocationRelativeTo(parent);
    }

    private String saveGUI() {
        if (endRadio.isSelected()) {
            FindText.getInstance().setDirection(FindText.DIRECTION_TO_END);
        } else if (startRadio.isSelected()) {
            FindText.getInstance().setDirection(FindText.DIRECTION_TO_START);
        } else {
            FindText.getInstance().setDirection(FindText.DIRECTION_ALL);
        }

        byte checks = 0;
        if (caseCheck.isSelected()) {
            checks |= FindText.CASE_SENSITIVE;
        }
        if (wholeCheck.isSelected()) {
            checks |= FindText.WHOLE_WORDS;
        }
        FindText.getInstance().setParams(checks);

        String str = (String) cmbSearch.getEditor().getItem();
        if (!str.equals("") && !list.contains(str)) {
            list.add(str);
            cmbSearch.setModel(new CMBModel(list));
            cmbSearch.setSelectedIndex(list.indexOf(str));
            cmbSearch.getEditor().setItem(str);
        }
        String rstr = (String) cmbReplace.getEditor().getItem();
        FindText.getInstance().replacement = rstr;
        if (!rstr.equals("") && !rlist.contains(rstr)) {
            rlist.add(rstr);
            cmbReplace.setModel(new CMBModel(rlist));
            cmbReplace.setSelectedIndex(rlist.indexOf(rstr));
            cmbReplace.getEditor().setItem(rstr);
        }
        return str;
    }

    private void initComponents() {
        ButtonGroup buttonGroup1 = new ButtonGroup();
        JLabel lblSearchFor = new JLabel();
        cmbSearch = new JComboBox();
        JPanel panelOptions = new JPanel();
        caseCheck = new JCheckBox();
        wholeCheck = new JCheckBox();
        JPanel panelDirection = new JPanel();
        endRadio = new JRadioButton();
        startRadio = new JRadioButton();
        allRadio = new JRadioButton();
        NiceButton btnSearch = new NiceButton();
        JLabel lblReplaceWith = new JLabel();
        cmbReplace = new JComboBox();
        NiceButton btnReplace = new NiceButton();
        JButton btnReplaceAll = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Find/replace text");
        setAlwaysOnTop(true);
        setResizable(false);

        lblSearchFor.setText("Search for:");
        lblSearchFor.setFont(lblSearchFor.getFont().deriveFont(lblSearchFor.getFont().getStyle() & ~java.awt.Font.BOLD));

        cmbSearch.setEditable(true);
        cmbSearch.setFont(cmbSearch.getFont().deriveFont(cmbSearch.getFont().getStyle() & ~java.awt.Font.BOLD));

        panelOptions.setBorder(BorderFactory.createTitledBorder("Options"));

        caseCheck.setText("Case sensitive");
        caseCheck.setFont(caseCheck.getFont().deriveFont(caseCheck.getFont().getStyle() & ~java.awt.Font.BOLD));

        wholeCheck.setText("Whole words");
        wholeCheck.setFont(wholeCheck.getFont().deriveFont(wholeCheck.getFont().getStyle() & ~java.awt.Font.BOLD));

        GroupLayout panelOptionsLayout = new GroupLayout(panelOptions);
        panelOptions.setLayout(panelOptionsLayout);
        panelOptionsLayout.setHorizontalGroup(
                panelOptionsLayout.createSequentialGroup().addContainerGap().addGroup(panelOptionsLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(caseCheck).addComponent(wholeCheck)).addContainerGap());
        panelOptionsLayout.setVerticalGroup(
                panelOptionsLayout.createSequentialGroup().addContainerGap().addComponent(caseCheck).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(wholeCheck).addContainerGap());

        panelDirection.setBorder(BorderFactory.createTitledBorder("Direction"));

        buttonGroup1.add(endRadio);
        endRadio.setSelected(true);
        endRadio.setText("To end of document");
        endRadio.setFont(endRadio.getFont().deriveFont(endRadio.getFont().getStyle() & ~java.awt.Font.BOLD));

        buttonGroup1.add(startRadio);
        startRadio.setText("To start of document");
        startRadio.setFont(startRadio.getFont().deriveFont(startRadio.getFont().getStyle() & ~java.awt.Font.BOLD));

        buttonGroup1.add(allRadio);
        allRadio.setText("All document");
        allRadio.setFont(allRadio.getFont().deriveFont(allRadio.getFont().getStyle() & ~java.awt.Font.BOLD));

        GroupLayout panelDirectionLayout = new GroupLayout(panelDirection);
        panelDirection.setLayout(panelDirectionLayout);
        panelDirectionLayout.setHorizontalGroup(
                panelDirectionLayout.createSequentialGroup().addContainerGap().addGroup(panelDirectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(endRadio).addComponent(startRadio).addComponent(allRadio)).addContainerGap());
        panelDirectionLayout.setVerticalGroup(
                panelDirectionLayout.createSequentialGroup().addComponent(endRadio).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(startRadio).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(allRadio).addContainerGap());

        btnSearch.setText("Search");
        btnSearch.setFont(btnSearch.getFont().deriveFont(btnSearch.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnSearch.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        lblReplaceWith.setText("Replace with:");
        lblReplaceWith.setFont(lblReplaceWith.getFont().deriveFont(lblReplaceWith.getFont().getStyle() & ~java.awt.Font.BOLD));

        cmbReplace.setEditable(true);
        cmbReplace.setFont(cmbReplace.getFont().deriveFont(cmbReplace.getFont().getStyle() & ~java.awt.Font.BOLD));

        btnReplace.setText("Replace");
        btnReplace.setFont(btnReplace.getFont().deriveFont(btnReplace.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnReplace.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceButtonActionPerformed(evt);
            }
        });

        btnReplaceAll.setText("Replace all");
        btnReplaceAll.setFont(btnReplaceAll.getFont().deriveFont(btnReplaceAll.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnReplaceAll.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceAllButtonActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblSearchFor).addComponent(lblReplaceWith)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(cmbSearch).addComponent(cmbReplace)).addContainerGap()).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(panelOptions).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(panelDirection).addContainerGap()).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(btnReplaceAll).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnReplace).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnSearch).addContainerGap()));
        layout.setVerticalGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblSearchFor).addComponent(cmbSearch)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblReplaceWith).addComponent(cmbReplace)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(panelOptions, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(panelDirection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(btnReplaceAll).addComponent(btnReplace).addComponent(btnSearch)).addContainerGap());
        pack();
    }

    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
        String str = saveGUI();
        try {
            FindText.getInstance().createPattern(str);
            if (FindText.getInstance().findNext(textPane.getText(),
                    textPane.getCaretPosition(),
                    textPane.getDocument().getEndPosition().getOffset() - 1)) {
                textPane.select(FindText.getInstance().getMatchStart(),
                        FindText.getInstance().getMatchEnd());
                textPane.grabFocus();
                dispose();
            } else {
                Main.tryShowMessage("Expression was not found");
            }
        } catch (PatternSyntaxException e) {
            Main.tryShowErrorMessage("Pattern syntax error");
            cmbSearch.grabFocus();
        }
    }//GEN-LAST:event_searchButtonActionPerformed

    private void replaceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceButtonActionPerformed
        String str = saveGUI();
        try {
            FindText.getInstance().createPattern(str);
            FindText.getInstance().replacement = (String) cmbReplace.getEditor().getItem();
            if (FindText.getInstance().replaceNext(textPane)) {
                textPane.grabFocus();
                dispose();
            } else {
                Main.tryShowMessage("Expression was not found");
            }
        } catch (PatternSyntaxException e) {
            Main.tryShowErrorMessage("Pattern syntax error");
            cmbSearch.grabFocus();
        }
    }//GEN-LAST:event_replaceButtonActionPerformed

    private void replaceAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllButtonActionPerformed
        String str = saveGUI();
        try {
            FindText.getInstance().createPattern(str);
            FindText.getInstance().replacement = (String) cmbReplace.getEditor().getItem();
            if (FindText.getInstance().replaceAll(textPane)) {
                textPane.grabFocus();
                dispose();
            } else {
                Main.tryShowMessage("Expression was not found");
            }
        } catch (PatternSyntaxException e) {
            Main.tryShowErrorMessage("Pattern syntax error");
            cmbSearch.grabFocus();
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    JRadioButton allRadio;
    JCheckBox caseCheck;
    JRadioButton endRadio;
    JComboBox cmbReplace;
    JComboBox cmbSearch;
    JRadioButton startRadio;
    JCheckBox wholeCheck;
    // End of variables declaration//GEN-END:variables
}
