/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.abstracttape.AbstractTapeContextImpl;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Objects;

public class TapeGui extends JDialog {
    public static final Font FONT_MONOSPACED = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final String ICON_ADD_FIRST = "/net/emustudio/plugins/device/abstracttape/gui/go-up.png";
    private static final String ICON_ADD_LAST = "/net/emustudio/plugins/device/abstracttape/gui/go-down.png";

    private final Dialogs dialogs;
    private final AbstractTapeContextImpl tapeContext;
    private final TapeModel listModel;

    private NiceButton btnAddFirst;
    private NiceButton btnAddLast;
    private NiceButton btnRemove;
    private NiceButton btnEdit;
    private NiceButton btnClear;
    private JList<String> lstTape;

    public TapeGui(JFrame parent, String title, AbstractTapeContextImpl tapeContext, boolean alwaysOnTop, Dialogs dialogs) {
        super(parent);
        this.tapeContext = Objects.requireNonNull(tapeContext);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.listModel = new TapeModel(tapeContext);

        initComponents();
        setTitle(title);
        setAlwaysOnTop(alwaysOnTop);
        setLocationRelativeTo(parent);

        tapeContext.setListener(() -> {
            listModel.fireChange();
            lstTape.ensureIndexIsVisible(tapeContext.getHeadPosition());
        });

        changeEditable();
    }


    private void changeEditable() {
        boolean b = tapeContext.getEditable();
        btnAddFirst.setEnabled(b && !tapeContext.isLeftBounded());
        btnAddLast.setEnabled(b);
        btnRemove.setEnabled(b);
        btnEdit.setEnabled(b);
        btnClear.setEnabled(b);
    }

    private void initComponents() {
        JScrollPane scrollTape = new JScrollPane();
        lstTape = new JList<>(listModel);
        btnAddFirst = new NiceButton("Add symbol", loadIcon(ICON_ADD_FIRST));
        btnAddLast = new NiceButton("Add symbol", loadIcon(ICON_ADD_LAST));
        btnRemove = new NiceButton("Remove symbol");
        btnEdit = new NiceButton("Edit symbol");
        btnClear = new NiceButton("Clear tape");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        lstTape.setFont(FONT_MONOSPACED);
        lstTape.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstTape.setCellRenderer(new TapeCellRenderer(tapeContext));
        scrollTape.setViewportView(lstTape);

        btnAddFirst.addActionListener(e -> dialogs
                .readString("Symbol value:", "Add symbol (on top)")
                .map(TapeSymbol::guess)
                .ifPresent(s -> {
                    try {
                        tapeContext.addFirst(s);
                    } catch (IllegalArgumentException ignored) {
                        dialogs.showError("Unexpected symbol type. Supported types: " + tapeContext.getAcceptedTypes());
                    }
                }));

        btnAddLast.addActionListener(e -> dialogs
                .readString("Symbol value:", "Add symbol (on bottom)")
                .map(TapeSymbol::guess)
                .ifPresent(s -> {
                    try {
                        tapeContext.addLast(s);
                    } catch (IllegalArgumentException ignored) {
                        dialogs.showError("Unexpected symbol type. Supported types: " + tapeContext.getAcceptedTypes());
                    }
                }));

        btnEdit.addActionListener(e -> {
            int symbolIndex = lstTape.getSelectedIndex();
            if (symbolIndex == -1) {
                dialogs.showError("A symbol must be selected");
            } else {
                dialogs
                        .readString("Enter symbol value:", "Edit symbol", tapeContext.getSymbolAt(symbolIndex).toString())
                        .map(TapeSymbol::guess)
                        .ifPresent(symbol -> tapeContext.setSymbolAt(symbolIndex, symbol));
            }
        });
        btnRemove.addActionListener(e -> {
            int symbolIndex = lstTape.getSelectedIndex();
            if (symbolIndex == -1) {
                dialogs.showError("A symbol must be selected");
                return;
            }
            tapeContext.removeSymbolAt(symbolIndex);
        });
        btnClear.addActionListener(e -> tapeContext.clear());

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(btnEdit, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnClear, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnRemove, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnAddLast, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnAddFirst, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(scrollTape, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                        ).addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnAddFirst)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scrollTape, GroupLayout.PREFERRED_SIZE, 200, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAddLast)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnRemove)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEdit)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClear)
                        .addContainerGap());
        pack();
    }

    private ImageIcon loadIcon(String resource) {
        URL url = getClass().getResource(resource);
        return url == null ? null : new ImageIcon(url);
    }
}
