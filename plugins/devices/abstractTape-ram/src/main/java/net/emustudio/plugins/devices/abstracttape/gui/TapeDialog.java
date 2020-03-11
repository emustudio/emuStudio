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
package net.emustudio.plugins.devices.abstracttape.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.devices.abstracttape.AbstractTapeContextImpl;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TapeDialog extends JDialog {
    private final Dialogs dialogs;
    private final AbstractTapeContextImpl tapeContext;
    private final TapeListModel listModel = new TapeListModel();

    public TapeDialog(String title, AbstractTapeContextImpl tapeContext, boolean alwaysOnTop, Dialogs dialogs) {
        this.tapeContext = Objects.requireNonNull(tapeContext);
        this.dialogs = Objects.requireNonNull(dialogs);

        initComponents();
        setTitle(title);
        setAlwaysOnTop(alwaysOnTop);

        lstTape.setModel(listModel);
        lstTape.setCellRenderer(new TapeCellRenderer());
        this.tapeContext.setListener(() -> {
            listModel.fireChange();
            lstTape.ensureIndexIsVisible(tapeContext.getHeadPosition());
        });

        changeEditable();
    }

    private class TapeListModel extends AbstractListModel<String> {

        @Override
        public String getElementAt(int index) {
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

    private class TapeCellRenderer extends JLabel implements ListCellRenderer<String> {

        private Font boldFont;
        private Font plainFont;

        TapeCellRenderer() {
            setOpaque(true);
            boldFont = getFont().deriveFont(Font.BOLD);
            plainFont = getFont().deriveFont(Font.PLAIN);
        }

        @Override
        public Component getListCellRendererComponent(JList list, String value, int index, boolean isSelected,
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
                setText(" " + value);
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
        lstTape = new JList<>();
        btnAddFirst = new NiceButton("Add symbol");
        btnAddLast = new NiceButton("Add symbol");
        btnRemove = new NiceButton("Remove symbol");
        btnEdit = new NiceButton("Edit symbol");
        btnClear = new NiceButton("Clear tape");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //setLocationRelativeTo(null);
        scrollTape.setViewportView(lstTape);

        btnAddFirst.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/devices/abstracttape/gui/go-up.png"))); // NOI18N
        btnAddFirst.addActionListener(e -> {
            String s = JOptionPane.showInputDialog(this, "Enter symbol value:");
            if (s != null) {
                tapeContext.addSymbolFirst(s);
            }
        });

        btnAddLast.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/plugins/devices/abstracttape/gui/go-down.png"))); // NOI18N
        btnAddLast.addActionListener(e -> {
            String s = JOptionPane.showInputDialog(this, "Enter symbol value:");
            if (s != null) {
                tapeContext.addSymbolLast(s);
            }
        });

        btnEdit.addActionListener(e -> {
            int symbolIndex = lstTape.getSelectedIndex();
            if (symbolIndex == -1) {
                dialogs.showError("A symbol must be selected");
            } else {
                dialogs
                    .readString("Enter symbol value:", "Edit symbol", tapeContext.getSymbolAt(symbolIndex))
                    .ifPresent(symbol -> tapeContext.editSymbol(symbolIndex, symbol));
            }
        });
        btnRemove.addActionListener(e -> {
            int symbolIndex = lstTape.getSelectedIndex();
            if (symbolIndex == -1) {
                dialogs.showError("A symbol must be selected");
                return;
            }
            tapeContext.removeSymbol(symbolIndex);
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
    private JList<String> lstTape;
}
