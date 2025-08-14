/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.plugins.device.adm3a.gui.GuiUtilsAdm3A.loadFont;

public class DisplayFontJComboRenderer extends JLabel implements ListCellRenderer<Integer> {
    private final Font originalFont = loadFont(DisplayFont.FONT_ORIGINAL);
    private final Font modernFont = loadFont(DisplayFont.FONT_MODERN);

    public DisplayFontJComboRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Integer value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        switch (value) {
            case 0:
                setFont(originalFont);
                setText("Original");
                break;
            case 1:
                setFont(modernFont);
                setText("Modern");
                break;
            default:
        }
        return this;
    }
}
