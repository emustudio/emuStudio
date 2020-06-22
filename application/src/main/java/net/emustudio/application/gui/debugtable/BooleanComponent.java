package net.emustudio.application.gui.debugtable;

import javax.swing.*;

public class BooleanComponent extends JLabel {
    public static final Icon BOOLEAN_ICON = new ImageIcon(BooleanCellRenderer.class.getResource("/net/emustudio/application/gui/dialogs/breakpoint.png"));

    private boolean value;

    public BooleanComponent(boolean value) {
        this.value = value;
        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        if (value) {
            setIcon(BOOLEAN_ICON);
        } else {
            setIcon(null);
        }
    }
}
