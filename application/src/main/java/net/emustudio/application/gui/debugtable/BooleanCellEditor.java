/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

import static net.emustudio.emulib.runtime.ui.Constants.TABLE_COLOR_ROW_EVEN;
import static net.emustudio.emulib.runtime.ui.Constants.TABLE_COLOR_ROW_ODD;

public class BooleanCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final BooleanComponent component;
    private boolean isMouseEvent;

    public BooleanCellEditor() {
        component = new BooleanComponent(false);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fireEditingStopped();
            }
        });

        InputMap im = component.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = component.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "click");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "click");
        am.put("click", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                component.setValue(!component.getValue());
            }
        });
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        isMouseEvent = e instanceof MouseEvent;
        return true;
    }

    @Override
    public Object getCellEditorValue() {
        return component.getValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        boolean state = isMouseEvent != (boolean) value;
        component.setValue(state);
        component.setOpaque(isSelected);
        component.setBackground((row % 2 == 0) ? TABLE_COLOR_ROW_ODD : TABLE_COLOR_ROW_EVEN);
        return component;
    }
}
