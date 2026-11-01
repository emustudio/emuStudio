/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.debugger.BreakpointState;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/** A single-click, keyboard-operable three-state breakpoint button. */
final class BreakpointCell extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
    private final JToggleButton renderer = button();
    private final JToggleButton editor = button();
    private BreakpointState value;
    private static final Icon KEPT_ICON = new Icon() {
        public int getIconWidth() { return BooleanComponent.BOOLEAN_ICON.getIconWidth(); }
        public int getIconHeight() { return BooleanComponent.BOOLEAN_ICON.getIconHeight(); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            BooleanComponent.BOOLEAN_ICON.paintIcon(c, g, x, y);
            // Small lock badge distinguishes held inspection without relying on color.
            g.setColor(UIManager.getColor("ToggleButton.foreground"));
            g.fillRoundRect(x + 13, y + 13, 8, 7, 2, 2);
            g.drawRoundRect(x + 15, y + 9, 4, 7, 4, 4);
        }
    };

    BreakpointCell() {
        editor.addActionListener(e -> {
            value = value.next();
            fireEditingStopped();
        });
        editor.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "cycleBreakpoint");
        editor.getActionMap().put("cycleBreakpoint", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.doClick(0); }
        });
    }

    private static JToggleButton button() {
        JToggleButton button = new JToggleButton();
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusable(true);
        return button;
    }

    private Component configure(JToggleButton button, Object state) {
        BreakpointState bp = state instanceof BreakpointState ? (BreakpointState) state : BreakpointState.OFF;
        button.setBorderPainted(bp != BreakpointState.OFF);
        button.setContentAreaFilled(bp != BreakpointState.OFF);
        button.setOpaque(bp != BreakpointState.OFF);
        button.setSelected(bp != BreakpointState.OFF);
        button.setIcon(bp == BreakpointState.OFF ? null : bp == BreakpointState.ON ? BooleanComponent.BOOLEAN_ICON : KEPT_ICON);
        button.setToolTipText(bp.tooltip);
        button.getAccessibleContext().setAccessibleName(bp.tooltip);
        return button;
    }

    @Override public Component getTableCellRendererComponent(JTable table, Object state, boolean selected,
                                                            boolean focus, int row, int column) {
        if (state == null || state == BreakpointState.OFF) return table.getDefaultRenderer(Object.class)
                .getTableCellRendererComponent(table, "", selected, focus, row, column);
        renderer.setEnabled(table.isEnabled());
        return configure(renderer, state);
    }

    @Override public Component getTableCellEditorComponent(JTable table, Object state, boolean selected, int row, int column) {
        value = (BreakpointState) state;
        return configure(editor, value);
    }

    @Override public Object getCellEditorValue() { return value; }
}
