/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 * An action created from an ActionEvent consumer.
 */
public class ActionFromEvent extends AbstractAction {
    private final Consumer<ActionEvent> action;

    public ActionFromEvent(Consumer<ActionEvent> action, String name, String iconResource, String tooltipText,
                           Class<?> callerClass) {
        super(name, GUI.loadIcon(iconResource, callerClass));
        putValue(SHORT_DESCRIPTION, tooltipText);
        this.action = action;
    }

    public ActionFromEvent(Consumer<ActionEvent> action, String iconResource, String tooltipText,
                           Class<?> callerClass) {
        this(action, null, iconResource, tooltipText, callerClass);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        action.accept(actionEvent);
    }
}

