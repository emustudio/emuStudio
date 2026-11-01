/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.application.gui.framework.EmuStudioGui;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;

/**
 * Toolbar button - a JButton ready to add to a toolbar.
 */
public class ToolbarButton extends JButton {

    public ToolbarButton(Action action) {
        super(action);
        setHideActionText(true);
        setToolTipText((String) action.getValue(SHORT_DESCRIPTION));
        setFocusable(false);
        putClientProperty("JButton.buttonType", "toolBarButton");
    }

    public ToolbarButton(Action action, String iconResource, String tooltipText, Class<?> callerClass) {
        super(action);
        action.putValue(SHORT_DESCRIPTION, tooltipText);
        action.putValue(SMALL_ICON, EmuStudioGui.loadIcon(iconResource, callerClass));
        setHideActionText(true);
        setToolTipText(tooltipText);
        setFocusable(false);
        putClientProperty("JButton.buttonType", "toolBarButton");
    }

    public ToolbarButton(Consumer<ActionEvent> action, String iconResource, String tooltipText,
                         Class<?> callerClass) {
        this(new ActionFromEvent(action, iconResource, tooltipText, callerClass));
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        Action action = getAction();
        putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
                text != null ? text : action == null ? null : action.getValue(Action.NAME));
    }

    @Override
    protected void actionPropertyChanged(Action action, String propertyName) {
        super.actionPropertyChanged(action, propertyName);
        if (Action.NAME.equals(propertyName) && getToolTipText() == null) {
            putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, action.getValue(Action.NAME));
        }
    }
}
