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
package net.emustudio.plugins.memory.bytemem.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static javax.swing.Action.SHORT_DESCRIPTION;

public class ToolbarButton extends JButton {

    public ToolbarButton(Action action, String iconResource) {
        super(action);
        setHideActionText(true);
        setIcon(new ImageIcon(getClass().getResource(iconResource)));
        setToolTipText(String.valueOf(action.getValue(SHORT_DESCRIPTION)));
        setFocusable(false);
    }

    public ToolbarButton(Action action, String iconResource, String tooltipText) {
        super(action);
        setHideActionText(true);
        setIcon(new ImageIcon(getClass().getResource(iconResource)));
        setToolTipText(tooltipText);
        setFocusable(false);
    }

    public ToolbarButton(Action action) {
        super(action);
        setHideActionText(true);
        setToolTipText(String.valueOf(action.getValue(SHORT_DESCRIPTION)));
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
