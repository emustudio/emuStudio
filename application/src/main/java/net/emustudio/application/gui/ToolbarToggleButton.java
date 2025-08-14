/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class ToolbarToggleButton extends JToggleButton {

    public ToolbarToggleButton(Consumer<ActionEvent> action, Consumer<ItemEvent> itemAction, String iconResource,
                               String tooltipText) {

        super(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                action.accept(actionEvent);
            }
        });
        setIcon(loadIcon(iconResource));
        setToolTipText(tooltipText);

        setFocusable(false);
        addItemListener(itemAction::accept);
    }

    public ToolbarToggleButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        this(action, (ItemEvent itemAction) -> {
        }, iconResource, tooltipText);
    }
}
