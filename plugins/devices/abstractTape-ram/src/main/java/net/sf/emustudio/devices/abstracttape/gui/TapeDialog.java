/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.devices.abstracttape.gui;

import emulib.emustudio.SettingsManager;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.devices.abstracttape.impl.AbstractTape;
import net.sf.emustudio.devices.abstracttape.impl.AbstractTapeContextImpl;

import javax.swing.*;
import java.awt.*;

public class TapeDialog extends JDialog {
    private final AbstractTapeContextImpl tapeContext;
    private TapeListModel listModel;

    public TapeDialog(AbstractTape tape, final AbstractTapeContextImpl tapeContext,
                      SettingsManager settings, long pluginID) {
        super();
        this.tapeContext = tapeContext;
        this.listModel = new TapeListModel();
        initComponents();
        this.setTitle(tape.getTitle());
        lstTape.setModel(listModel);
        lstTape.setCellRenderer(new TapeCellRenderer());
        this.tapeContext.setListener(() -> {
            listModel.fireChange();
            lstTape.ensureIndexIsVisible(tapeContext.getHeadPosition());
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
            String element = "";
            
            if (tapeContext.showPositions()) {
                element += String.format("%02d: ", index);
            }
            String symbolAtIndex = tapeContext.getSymbolAt(index);
            if (symbolAtIndex == null || symbolAtIndex.isEmpty()) {
                element += "<empty>";
            } else {
                element += symbolAtIndex;
            }
            
            return element;
        }

        @Override
        public int getSize() {
            return tapeContext.getSize();
        }

        public void fireChange() {
            this.fireContentsChanged(this, 0, tapeContext.getSize() - 1);
        }
    }

    private class TapeCellRenderer extends JLabel implements ListCellRenderer {

        private Font boldFont;
        private Font plainFont;

        TapeCellRenderer() {
            super();
            setOpaque(true);
            boldFont = getFont().deriveFont(Font.BOLD);
            plainFont = getFont().deriveFont(Font.PLAIN);
        }

        @Override
        public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            if (tapeContext.highlightCurrentPosition() && (tapeContext.getHeadPosition() == index)) {
                this.setBackground(Color.BLUE);
                this.setForeground(Color.WHITE);
            } else {
                this.setBackground(Color.WHITE);

                String s = tapeContext.getSymbolAt(index);
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

    private void changeEditable() {
        boolean b = tapeContext.getEditable();
        btnAddFirst.setEnabled(b && !tapeContext.isBounded());
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

        btnAddFirst.setIcon(new ImageIcon(getClass().getResource("/net/sf/emustudio/devices/abstracttape/gui/go-up.png"))); // NOI18N
        btnAddFirst.addActionListener(e -> {
            String s = JOptionPane.showInputDialog("Enter symbol value:");
            tapeContext.addSymbolFirst(s);
        });

        btnAddLast.setIcon(new ImageIcon(getClass().getResource("/net/sf/emustudio/devices/abstracttape/gui/go-down.png"))); // NOI18N
        btnAddLast.addActionListener(e -> {
            String s = JOptionPane.showInputDialog("Enter symbol value:");
            tapeContext.addSymbolLast(s);
        });

        btnEdit.addActionListener(e -> {
            int i = lstTape.getSelectedIndex();
            if (i == -1) {
                StaticDialogs.showErrorMessage("A symbol must be selected !");
                return;
            }
            String s = JOptionPane.showInputDialog("Enter symbol value:");
            tapeContext.editSymbol(i, s);
        });
        btnRemove.addActionListener(e -> {
            int i = lstTape.getSelectedIndex();
            if (i == -1) {
                StaticDialogs.showErrorMessage("A symbol must be selected !");
                return;
            }
            tapeContext.removeSymbol(i);
        });
        btnClear.addActionListener(e -> tapeContext.clear());

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
