/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;

/**
 * Toolbar toggle button - a JToggleButton ready to add to a toolbar.
 */
public class ToolbarToggleButton extends JToggleButton {

    public ToolbarToggleButton(Action action) {
        super(action);
        setHideActionText(true);
        setToolTipText(String.valueOf(action.getValue(SHORT_DESCRIPTION)));
        setFocusable(false);
        putClientProperty("JButton.buttonType", "toolBarButton");
    }

    public ToolbarToggleButton(Action action, String iconResource, String tooltipText, Class<?> callerClass) {
        super(action);
        action.putValue(SHORT_DESCRIPTION, tooltipText);
        action.putValue(SMALL_ICON, GUI.loadIcon(iconResource, callerClass));
        setHideActionText(true);
        setToolTipText(tooltipText);
        setFocusable(false);
        putClientProperty("JButton.buttonType", "toolBarButton");
    }

    public ToolbarToggleButton(Consumer<ActionEvent> action, Consumer<ItemEvent> itemAction,
                               String iconResource, String tooltipText, Class<?> callerClass) {
        this(action, iconResource, tooltipText, callerClass);
        addItemListener(itemAction::accept);
    }

    public ToolbarToggleButton(Consumer<ActionEvent> action, String iconResource, String tooltipText,
                               Class<?> callerClass) {
        this(new ActionFromEvent(action, iconResource, tooltipText, callerClass));
    }
}

