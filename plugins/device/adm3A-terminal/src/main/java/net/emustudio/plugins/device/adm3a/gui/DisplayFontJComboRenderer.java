/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.*;
import java.awt.*;


public class DisplayFontJComboRenderer extends JLabel implements ListCellRenderer<Integer> {
    private final Font originalFont = GUI.loadFontResource(
            DisplayFont.FONT_ORIGINAL.path, DisplayFontJComboRenderer.class, DisplayFont.FONT_ORIGINAL.fontSize);
    private final Font modernFont = GUI.loadFontResource(
            DisplayFont.FONT_MODERN.path, DisplayFontJComboRenderer.class, DisplayFont.FONT_MODERN.fontSize);

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
