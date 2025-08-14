/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;

import java.awt.*;

public interface ExtendedDialogs extends Dialogs {

    default void setParent(Component component) {

    }
}
