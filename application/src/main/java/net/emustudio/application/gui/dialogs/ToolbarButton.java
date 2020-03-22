package net.emustudio.application.gui.dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ToolbarButton extends JButton {

    public ToolbarButton(Action action, String iconResource, String tooltipText) {
        super(action);
        setHideActionText(true);
        setIcon(new ImageIcon(getClass().getResource(iconResource)));
        setToolTipText(tooltipText);
        setFocusable(false);
        setBorderPainted(false);
    }

    public ToolbarButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        this(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                action.accept(actionEvent);
            }
        }, iconResource, tooltipText);
    }
}
