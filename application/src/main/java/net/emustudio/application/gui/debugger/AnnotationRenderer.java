/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Keeps list navigation while giving annotation ranges and descriptions stable, scannable columns.
 */
final class AnnotationRenderer extends JPanel implements ListCellRenderer<MemoryAnnotations.Entry> {
    private final DefaultListCellRenderer standard = new DefaultListCellRenderer();
    private final JLabel location = new JLabel(), title = new JLabel(), detail = new JLabel();

    AnnotationRenderer() {
        super(new BorderLayout(10, 0));
        location.setPreferredSize(new Dimension(122, 20));
        location.setVerticalAlignment(SwingConstants.TOP);
        for (JLabel label : List.of(location, title, detail)) label.putClientProperty("html.disable", true);
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(detail, BorderLayout.SOUTH);
        add(location, BorderLayout.WEST);
        add(text);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MemoryAnnotations.Entry> list,
                                                  MemoryAnnotations.Entry entry,
                                                  int index, boolean selected, boolean focus) {
        standard.getListCellRendererComponent(list, entry, index, selected, focus);
        setBackground(standard.getBackground());
        setEnabled(list.isEnabled());
        setBorder(BorderFactory.createCompoundBorder(standard.getBorder(), BorderFactory.createEmptyBorder(3, 4, 3, 4)));
        for (JLabel label : List.of(location, title, detail)) {
            label.setForeground(standard.getForeground());
            label.setFont(list.getFont());
            label.setEnabled(list.isEnabled());
        }
        location.setFont(EmuStudioGui.monospaced(list));
        location.setText(DebuggerWidgets.addressRange(entry.start, entry.end));
        title.setText(entry.text.isBlank() ? "Unnamed annotation" : entry.text);
        detail.setText(entry.kind.equals("CODE") ? "Code" : "Data");
        setToolTipText(String.format("%04X %s", entry.start, entry.text));
        getAccessibleContext().setAccessibleName(location.getText() + ", " + title.getText() + ", " + detail.getText());
        return this;
    }
}
