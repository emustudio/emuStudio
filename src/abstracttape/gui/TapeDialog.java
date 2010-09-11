/**
 * TapeDialog.java
 * 
 *   KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package abstracttape.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractListModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;

import emuLib8.plugins.ISettingsHandler;

import emuLib8.runtime.StaticDialogs;

import abstracttape.gui.utils.NiceButton;
import abstracttape.impl.AbstractTape;
import abstracttape.impl.TapeContext;
import abstracttape.impl.TapeContext.TapeListener;

@SuppressWarnings("serial")
public class TapeDialog extends JDialog {

    private TapeContext tape;
    private TapeListModel listModel;

    public TapeDialog(AbstractTape atape, final TapeContext tape,
            ISettingsHandler settings, long pluginID) {
        super();
        this.tape = tape;
        this.listModel = new TapeListModel();
        initComponents();
        this.setTitle(atape.getTitle());
        lstTape.setModel(listModel);
        lstTape.setCellRenderer(new TapeCellRenderer());
        this.tape.setListener(new TapeListener() {

            @Override
            public void tapeChanged(EventObject evt) {
                listModel.fireChange();
                lstTape.ensureIndexIsVisible(tape.getPos());
            }
        });
        String s = settings.readSetting(pluginID, "alwaysOnTop");
        if (s == null || !s.toLowerCase().equals("true")) {
            this.setAlwaysOnTop(false);
        } else {
            this.setAlwaysOnTop(true);
        }
        changeEditable();
    }

    private class TapeListModel extends AbstractListModel {

        @Override
        public Object getElementAt(int index) {
            String s = tape.getSymbolAt(index);
            return (s == null || s.equals("")) ? "<empty>" : s;
        }

        @Override
        public int getSize() {
            return tape.getSize();
        }

        public void fireChange() {
            this.fireContentsChanged(this, 0, tape.getSize() - 1);
        }
    }

    public class TapeCellRenderer extends JLabel implements ListCellRenderer {

        private Font boldFont;
        private Font plainFont;

        public TapeCellRenderer() {
            super();
            setOpaque(true);
            boldFont = getFont().deriveFont(Font.BOLD);
            plainFont = getFont().deriveFont(Font.PLAIN);
        }

        @Override
        public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            if (tape.getPosVisible() && (tape.getPos() == index)) {
                this.setBackground(Color.BLUE);
                this.setForeground(Color.WHITE);
            } else {
                this.setBackground(Color.WHITE);

                String s = tape.getSymbolAt(index);
                if (s == null || s.equals("")) {
                    this.setForeground(Color.LIGHT_GRAY);
                } else {
                    this.setForeground(Color.BLACK);
                }
            }
            if (isSelected) {
                this.setFont(boldFont);
            } else {
                this.setFont(plainFont);
            }

            if (value != null) {
                setText(" " + value.toString());
            } else {
                setText("");
            }
            return this;
        }
    }

    public final void changeEditable() {
        boolean b = tape.getEditable();
        btnAddFirst.setEnabled(b && !tape.isBounded());
        btnAddLast.setEnabled(b);
        btnRemove.setEnabled(b);
        btnEdit.setEnabled(b);
        btnClear.setEnabled(b);
    }

    private void initComponents() {
        JScrollPane scrollTape = new JScrollPane();
        lstTape = new JList();
        btnAddFirst = new NiceButton("Add symbol");
        btnAddLast = new NiceButton("Add symbol");
        btnRemove = new NiceButton("Remove symbol");
        btnEdit = new NiceButton("Edit symbol");
        btnClear = new NiceButton("Clear tape");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //setLocationRelativeTo(null);
        scrollTape.setViewportView(lstTape);

        btnAddFirst.setIcon(new ImageIcon(getClass().getResource("/abstracttape/resources/go-up.png"))); // NOI18N
        btnAddFirst.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog("Enter symbol value:");
                tape.addSymbolFirst(s);
            }
        });

        btnAddLast.setIcon(new ImageIcon(getClass().getResource("/abstracttape/resources/go-down.png"))); // NOI18N
        btnAddLast.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog("Enter symbol value:");
                tape.addSymbolLast(s);
            }
        });

        btnEdit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int i = lstTape.getSelectedIndex();
                if (i == -1) {
                    StaticDialogs.showErrorMessage("A symbol must be selected !");
                    return;
                }
                String s = JOptionPane.showInputDialog("Enter symbol value:");
                tape.editSymbol(i, s);
            }
        });
        btnRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int i = lstTape.getSelectedIndex();
                if (i == -1) {
                    StaticDialogs.showErrorMessage("A symbol must be selected !");
                    return;
                }
                tape.removeSymbol(i);
            }
        });
        btnClear.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tape.clear();
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false).addComponent(btnEdit, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(btnClear, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(btnRemove, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(btnAddLast, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(btnAddFirst, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(scrollTape, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createSequentialGroup().addContainerGap().addComponent(btnAddFirst).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollTape, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnAddLast).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(btnRemove).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnEdit).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnClear).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        pack();
    }
    private NiceButton btnAddFirst;
    private NiceButton btnAddLast;
    private NiceButton btnRemove;
    private NiceButton btnEdit;
    private NiceButton btnClear;
    private JList lstTape;
}
