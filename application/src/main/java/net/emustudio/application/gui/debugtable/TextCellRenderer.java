/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.Objects;

class TextCellRenderer extends DefaultTableCellRenderer {

    private final DebugTableModel model;

    TextCellRenderer(DebugTableModel model) {
        this.model = Objects.requireNonNull(model);
        setDoubleBuffered(true);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        boolean current = model.isRowAtCurrentInstruction(table.convertRowIndexToModel(row));
        boolean address = false;
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (DebugTableModel.TITLE_ADDRESS.equals(model.getColumnAt(col).getTitle())
                    && model.getColumnName(col).equals(table.getColumnName(column))) {
                address = true;
                break;
            }
        }
        setIcon(address ? (current ? CURRENT_INSTRUCTION : EMPTY_MARKER) : null);
        setIconTextGap(4);
        Font tableFont = table.getFont();
        setFont(new Font(Font.MONOSPACED, tableFont.getStyle(), tableFont.getSize()).deriveFont(tableFont.getSize2D()));
        setToolTipText(current ? "Current instruction (PC)" : null);
        getAccessibleContext().setAccessibleDescription(current ? "Current instruction (PC)" : null);
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (!getFont().isBold() || !isEnabled() || getText() == null || getText().isEmpty()) return;
        // Most monospaced fonts map extra-heavy weights to the same bold face.
        // Add a small outline to make breakpoint text visibly heavier at the same size.
        FontMetrics metrics = getFontMetrics(getFont());
        Insets insets = getInsets();
        Rectangle view = new Rectangle(insets.left, insets.top,
                getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
        Rectangle icon = new Rectangle(), text = new Rectangle();
        String visible = SwingUtilities.layoutCompoundLabel(this, metrics, getText(), getIcon(),
                getVerticalAlignment(), getHorizontalAlignment(), getVerticalTextPosition(),
                getHorizontalTextPosition(), view, icon, text, getIconTextGap());
        if (visible.isEmpty()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.clip(view);
            g.setColor(getForeground());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(.6f));
            TextLayout layout = new TextLayout(visible, getFont(), g.getFontRenderContext());
            g.draw(layout.getOutline(AffineTransform.getTranslateInstance(text.x, text.y + metrics.getAscent())));
        } finally {
            g.dispose();
        }
    }

    private static final Icon EMPTY_MARKER = new Marker(false);
    private static final Icon CURRENT_INSTRUCTION = new Marker(true);

    /** PC remains visible independently of the selected row and its keyboard focus border. */
    private static final class Marker implements Icon {
        private final boolean current;
        private Marker(boolean current) { this.current = current; }
        @Override public int getIconWidth() { return 10; }
        @Override public int getIconHeight() { return 10; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (current) {
                graphics.setColor(component.getForeground());
                graphics.fillPolygon(new int[] {x + 1, x + 8, x + 1}, new int[] {y + 1, y + 5, y + 9}, 3);
            }
        }
    }
}
