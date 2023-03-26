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
package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.plugins.device.abstracttape.AbstractTapeContextImpl;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;

public class TapeModel extends DefaultListModel<String> {
    private final AbstractTapeContext tapeContext;
    private volatile int currentSize;

    public TapeModel(AbstractTapeContextImpl tapeContext) {
        this.tapeContext = Objects.requireNonNull(tapeContext);
        this.currentSize = tapeContext.getSize();
    }

    @Override
    public String getElementAt(int index) {
        Map.Entry<Integer, TapeSymbol> symbol = tapeContext.getSymbolAtIndex(index);

        String element = "";
        if (tapeContext.getShowPositions()) {
            element = String.format("%02d: ", symbol.getKey());
        }
        element += symbol.getValue().toString();
        return element;
    }

    @Override
    public int getSize() {
        return currentSize;
    }

    public void fireChange() {
        int newSize = tapeContext.getSize();
        currentSize = newSize;
        this.fireContentsChanged(this, 0, newSize - 1);
    }
}
