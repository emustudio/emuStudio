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
