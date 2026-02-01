/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.ui.Constants.*;

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

