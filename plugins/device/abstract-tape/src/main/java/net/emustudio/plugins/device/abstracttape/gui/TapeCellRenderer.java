package net.emustudio.plugins.device.abstracttape.gui;

import net.emustudio.plugins.device.abstracttape.AbstractTapeContextImpl;

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

            String s = tapeContext.getSymbolAt(index);
            if (s == null || s.equals("")) {
                setForeground(Color.DARK_GRAY);
            } else {
                setForeground(Color.BLACK);
            }
        }

        return this;
    }
}
