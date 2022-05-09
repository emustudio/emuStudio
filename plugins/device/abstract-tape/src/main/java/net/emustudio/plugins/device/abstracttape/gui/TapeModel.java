package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.plugins.device.abstracttape.AbstractTapeContextImpl;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import javax.swing.*;
import java.util.Objects;

public class TapeModel extends AbstractListModel<String> {
    private final AbstractTapeContext tapeContext;

    public TapeModel(AbstractTapeContextImpl tapeContext) {
        this.tapeContext = Objects.requireNonNull(tapeContext);
    }

    @Override
    public String getElementAt(int index) {
        String element = "";
        if (tapeContext.getShowPositions()) {
            element += String.format("%02d: ", index);
        }
        String symbolAtIndex = tapeContext.getSymbolAt(index).map(TapeSymbol::toString).orElse("<empty>");
        element += symbolAtIndex;

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
