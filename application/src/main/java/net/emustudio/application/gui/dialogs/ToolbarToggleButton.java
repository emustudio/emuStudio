package net.emustudio.application.gui.dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

public class ToolbarToggleButton extends JToggleButton {

    public ToolbarToggleButton(Consumer<ActionEvent> action, Consumer<ItemEvent> itemAction, String iconResource,
                               String tooltipText) {

        super(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                action.accept(actionEvent);
            }
        });
        setIcon(new ImageIcon(getClass().getResource(iconResource)));
        setToolTipText(tooltipText);

        setFocusable(false);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        addItemListener(itemAction::accept);
    }

    public ToolbarToggleButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        this(action, (ItemEvent itemAction) -> {}, iconResource, tooltipText);
    }
}
