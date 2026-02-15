/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.abstracttape.AbstractTapeContextImpl;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import javax.swing.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;


public class TapeGui extends DialogBase {
    private static final String ICON_ADD_FIRST = "/net/emustudio/plugins/device/abstracttape/gui/go-up.png";
    private static final String ICON_ADD_LAST = "/net/emustudio/plugins/device/abstracttape/gui/go-down.png";

    private final Dialogs dialogs;
    private final AbstractTapeContextImpl tapeContext;
    private final TapeModel listModel;

    private JButton btnAddFirst;
    private JButton btnAddLast;
    private JButton btnRemove;
    private JButton btnEdit;
    private JButton btnClear;
    private JList<String> lstTape;

    public TapeGui(JFrame parent, String title, AbstractTapeContextImpl tapeContext, boolean alwaysOnTop, Dialogs dialogs) {
        super(parent, title, false);
        this.tapeContext = Objects.requireNonNull(tapeContext);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.listModel = new TapeModel(tapeContext);

        setAlwaysOnTop(alwaysOnTop);
        buildContent();

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

    @Override
    protected JComponent initializeComponents() {
        lstTape = new JList<>(listModel);
        lstTape.setFont(FONT_MONOSPACED);
        lstTape.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstTape.setCellRenderer(new TapeCellRenderer(tapeContext));

        btnAddFirst = new JButton("Add symbol", loadIcon(ICON_ADD_FIRST));
        btnAddLast = new JButton("Add symbol", loadIcon(ICON_ADD_LAST));
        btnRemove = new JButton("Remove symbol");
        btnEdit = new JButton("Edit symbol");
        btnClear = new JButton("Clear tape");

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

        JPanel content = GUI.panel("insets dialog", "[148!,grow]", "[][200:200:,grow][][][][][]");
        content.add(btnAddFirst, "growx, wrap");
        content.add(new JScrollPane(lstTape), "grow, wrap");
        content.add(btnAddLast, "growx, wrap");
        content.add(btnRemove, "growx, gaptop 10, wrap");
        content.add(btnEdit, "growx, wrap");
        content.add(btnClear, "growx, wrap");

        return content;
    }
}
