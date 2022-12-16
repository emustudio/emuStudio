/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import static net.emustudio.plugins.device.adm3a.Utils.loadFont;

public class DisplayFontJComboRenderer extends JLabel implements ListCellRenderer<String> {
    private final Font originalFont = loadFont(DisplayFont.FONT_ORIGINAL);
    private final Font modernFont = loadFont(DisplayFont.FONT_MODERN);

    public DisplayFontJComboRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        setText(value);
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        switch (index) {
            case 0:
                setFont(originalFont);
                break;
            case 1:
                setFont(modernFont);
                break;
            default:
        }
        return this;
    }
}
