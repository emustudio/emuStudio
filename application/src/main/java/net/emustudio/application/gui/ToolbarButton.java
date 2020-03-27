package net.emustudio.application.gui;

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
    }

    public ToolbarButton(Action action, String tooltipText) {
        super(action);
        setHideActionText(true);
        setToolTipText(tooltipText);
        setFocusable(false);
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
