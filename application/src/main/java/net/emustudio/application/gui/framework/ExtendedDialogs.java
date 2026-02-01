/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.emulib.runtime.ui.Dialogs;

import java.awt.*;

public interface ExtendedDialogs extends Dialogs {

    default void setParent(Component component) {

    }
}
