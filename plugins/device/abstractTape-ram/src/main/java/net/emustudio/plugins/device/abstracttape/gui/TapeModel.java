package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.plugins.device.abstracttape.AbstractTapeContextImpl;

import javax.swing.*;
import java.util.Objects;

public class TapeModel extends AbstractListModel<String> {
    private final AbstractTapeContextImpl tapeContext;

    public TapeModel(AbstractTapeContextImpl tapeContext) {
        this.tapeContext = Objects.requireNonNull(tapeContext);
    }

    @Override
    public String getElementAt(int index) {
        String element = "";

        if (tapeContext.showPositions()) {
            element += String.format("%02d: ", index);
        }
        String symbolAtIndex = tapeContext.getSymbolAt(index);
        if (symbolAtIndex == null || symbolAtIndex.isEmpty()) {
            element += "<empty>";
        } else {
            element += symbolAtIndex;
        }

        return element;
    }

    @Override
    public int getSize() {
        return tapeContext.getSize();
    }

    public void fireChange() {
        this.fireContentsChanged(this, 0, tapeContext.getSize() - 1);
    }
}
