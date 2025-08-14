/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
