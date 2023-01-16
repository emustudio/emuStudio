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
package net.emustudio.plugins.device.adm3a.gui;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.plugins.device.adm3a.gui.GuiUtils.loadFont;

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
