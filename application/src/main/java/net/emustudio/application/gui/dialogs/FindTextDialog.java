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
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.ConstantSizeButton;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.gui.editor.FindText;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

import static net.emustudio.application.gui.Components.addKeyListenerRecursively;

/**
 * The find dialog. It searches for text within the source code editor.
 */
class FindTextDialog extends javax.swing.JDialog {
    private static final List<String> list = new ArrayList<>();
    private static final List<String> rlist = new ArrayList<>();
    private final Editor editor;
    private final FindText finder;
    private final DialogKeyListener dialogKeyListener = new DialogKeyListener();
    private final Dialogs dialogs;

    private FindTextDialog(JFrame parent, FindText finder, Editor pane, Dialogs dialogs) {
        super(parent, false);

        this.editor = Objects.requireNonNull(pane);
        this.finder = Objects.requireNonNull(finder);
        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();

        switch (finder.getDirection()) {
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
        caseCheck.setSelected(finder.isCaseSensitive());
        wholeCheck.setSelected(finder.isWholeWords());

        cmbSearch.setModel(new CMBModel(list));
        cmbReplace.setModel(new CMBModel(rlist));

        String str = finder.getFindExpr();
        String rstr = finder.replacement;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(str)) {
                cmbSearch.setSelectedIndex(i);
                cmbSearch.getEditor().setItem(str);
                break;
            }
        }
        for (int i = 0; i < rlist.size(); i++) {
            if (rlist.get(i).equals(rstr)) {
                cmbReplace.setSelectedIndex(i);
                cmbReplace.getEditor().setItem(rstr);
                break;
            }
        }
        this.setLocationRelativeTo(parent);
    }

    static FindTextDialog create(JFrame parent, FindText finder, Editor pane, Dialogs dialogs) {
        FindTextDialog dialog = new FindTextDialog(parent, finder, pane, dialogs);
        dialog.initialize();
        return dialog;
    }

    private void initialize() {
        addKeyListenerRecursively(this, dialogKeyListener);
    }

    private String saveGUI() {
        if (endRadio.isSelected()) {
            finder.setDirection(FindText.DIRECTION_TO_END);
        } else if (startRadio.isSelected()) {
            finder.setDirection(FindText.DIRECTION_TO_START);
        } else {
            finder.setDirection(FindText.DIRECTION_ALL);
        }

        byte checks = 0;
        if (caseCheck.isSelected()) {
            checks |= FindText.CASE_SENSITIVE;
        }
        if (wholeCheck.isSelected()) {
            checks |= FindText.WHOLE_WORDS;
        }
        finder.setParams(checks);

        String str = (String) cmbSearch.getEditor().getItem();
        fillComboWithPreviousSearches(str, list);
        String rstr = (String) cmbReplace.getEditor().getItem();
        finder.replacement = rstr;
        fillComboWithPreviousSearches(rstr, rlist);
        return str;
    }

    private void fillComboWithPreviousSearches(String str, List<String> previousSearches) {
        if (!str.equals("") && !previousSearches.contains(str)) {
            previousSearches.add(str);
            cmbSearch.setModel(new CMBModel(previousSearches));
            cmbSearch.setSelectedIndex(previousSearches.indexOf(str));
            cmbSearch.getEditor().setItem(str);
        }
    }

    private void initComponents() {
        ButtonGroup buttonGroup1 = new ButtonGroup();
        JLabel lblSearchFor = new JLabel();
        cmbSearch = new JComboBox<>();
        JPanel panelOptions = new JPanel();
        caseCheck = new JCheckBox();
        wholeCheck = new JCheckBox();
        JPanel panelDirection = new JPanel();
        endRadio = new JRadioButton();
        startRadio = new JRadioButton();
        allRadio = new JRadioButton();
        ConstantSizeButton btnSearch = new ConstantSizeButton();
        JLabel lblReplaceWith = new JLabel();
        cmbReplace = new JComboBox<>();
        ConstantSizeButton btnReplace = new ConstantSizeButton();
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
        btnSearch.addActionListener(this::searchButtonActionPerformed);

        lblReplaceWith.setText("Replace with:");
        lblReplaceWith.setFont(lblReplaceWith.getFont().deriveFont(lblReplaceWith.getFont().getStyle() & ~java.awt.Font.BOLD));

        cmbReplace.setEditable(true);
        cmbReplace.setFont(cmbReplace.getFont().deriveFont(cmbReplace.getFont().getStyle() & ~java.awt.Font.BOLD));

        btnReplace.setText("Replace");
        btnReplace.setFont(btnReplace.getFont().deriveFont(btnReplace.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnReplace.addActionListener(this::replaceButtonActionPerformed);

        btnReplaceAll.setText("Replace all");
        btnReplaceAll.setFont(btnReplaceAll.getFont().deriveFont(btnReplaceAll.getFont().getStyle() & ~java.awt.Font.BOLD));
        btnReplaceAll.addActionListener(this::replaceAllButtonActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblSearchFor).addComponent(lblReplaceWith)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(cmbSearch).addComponent(cmbReplace)).addContainerGap()).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(panelOptions).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(panelDirection).addContainerGap()).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(btnReplaceAll).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnReplace).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnSearch).addContainerGap()));
        layout.setVerticalGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblSearchFor).addComponent(cmbSearch)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblReplaceWith).addComponent(cmbReplace)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(panelOptions, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(panelDirection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(btnReplaceAll).addComponent(btnReplace).addComponent(btnSearch)).addContainerGap());
        pack();
    }

    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
        String str = saveGUI();
        try {
            finder.createPattern(str);
            if (finder.findNext(editor.getText(),
                editor.getCaretPosition(),
                editor.getDocument().getEndPosition().getOffset() - 1)) {
                editor.select(finder.getMatchStart(), finder.getMatchEnd());
                editor.grabFocus();
                dispose();
            } else {
                dialogs.showInfo("Text was not found", "Find text");
            }
        } catch (PatternSyntaxException e) {
            dialogs.showError("Regular expression syntax error", "Find text");
            cmbSearch.grabFocus();
        }
    }//GEN-LAST:event_searchButtonActionPerformed

    private void replaceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceButtonActionPerformed
        String str = saveGUI();
        try {
            finder.createPattern(str);
            finder.replacement = (String) cmbReplace.getEditor().getItem();
            if (finder.replaceNext(editor)) {
                editor.grabFocus();
                dispose();
            } else {
                dialogs.showInfo("Text was not found", "Replace text");
            }
        } catch (PatternSyntaxException e) {
            dialogs.showError("Regular expression syntax error", "Replace text");
            cmbSearch.grabFocus();
        }
    }//GEN-LAST:event_replaceButtonActionPerformed

    private void replaceAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllButtonActionPerformed
        String str = saveGUI();
        try {
            finder.createPattern(str);
            finder.replacement = (String) cmbReplace.getEditor().getItem();
            if (finder.replaceAll(editor)) {
                editor.grabFocus();
                dispose();
            } else {
                dialogs.showInfo("Text was not found", "Replace all");
            }
        } catch (PatternSyntaxException e) {
            dialogs.showError("Regular expression syntax error", "Replace all");
            cmbSearch.grabFocus();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JRadioButton allRadio;
    private JCheckBox caseCheck;
    private JRadioButton endRadio;
    private JComboBox<String> cmbReplace;
    private JComboBox<String> cmbSearch;
    private JRadioButton startRadio;
    private JCheckBox wholeCheck;
    // End of variables declaration//GEN-END:variables

    private static class CMBModel implements ComboBoxModel<String> {

        private int in = -1;
        private final List<String> clist;

        CMBModel(List<String> clist) {
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
        public String getElementAt(int index) {
            return clist.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
        }
    }

    private class DialogKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                FindTextDialog.this.dispose();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}
