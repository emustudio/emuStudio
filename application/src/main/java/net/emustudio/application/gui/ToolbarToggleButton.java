/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui;

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
        setIcon(new ImageIcon(ClassLoader.getSystemResource(iconResource)));
        setToolTipText(tooltipText);

        setFocusable(false);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        addItemListener(itemAction::accept);
    }

    public ToolbarToggleButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        this(action, (ItemEvent itemAction) -> {
        }, iconResource, tooltipText);
    }
}
