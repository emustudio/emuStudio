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
package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.plugins.device.abstracttape.AbstractTapeContextImpl;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TapeCellRenderer extends DefaultListCellRenderer {
    private final AbstractTapeContextImpl tapeContext;

    TapeCellRenderer(AbstractTapeContextImpl tapeContext) {
        this.tapeContext = Objects.requireNonNull(tapeContext);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (tapeContext.highlightCurrentPosition() && (tapeContext.getHeadPosition() == index)) {
            setBackground(Color.RED);
            setForeground(Color.WHITE);
        } else {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
            } else {
                setBackground(Color.WHITE);
            }

            String s = tapeContext.getSymbolAt(index).map(TapeSymbol::toString).orElse("");
            if (s.equals("")) {
                setForeground(Color.DARK_GRAY);
            } else {
                setForeground(Color.BLACK);
            }
        }

        return this;
    }
}
