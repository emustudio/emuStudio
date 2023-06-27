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
package net.emustudio.plugins.device.audiotape_player.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiConstants.*;

public class TapesListRenderer extends JLabel implements ListCellRenderer<String> {

    TapesListRenderer() {
        setFont(FONT_MONOSPACED);
        setDoubleBuffered(true);
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
            setForeground(UIManager.getColor("List.selectionForeground"));
        } else {
            setBackground((index % 2 == 0) ? TABLE_COLOR_ROW_ODD : TABLE_COLOR_ROW_EVEN);
            setForeground(Color.BLACK);
        }
        String val = Objects.requireNonNullElse(value, "");
        if (val.toLowerCase().endsWith(".tap") || val.toLowerCase().endsWith(".tzx")) {
            val = val.substring(0, val.length() - 4);
        }

        setText(val);
        return this;
    }
}

