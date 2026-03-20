/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import net.emustudio.emulib.runtime.ui.GUI;

/**
 * Provides the application-level GUI instance.
 * <p>
 * This is used internally by the application module to avoid threading the GUI instance through every constructor.
 * Plugins should use {@link net.emustudio.emulib.runtime.ApplicationApi#getGUI()} instead.
 */
public final class GUIProvider {
    private static final GUI INSTANCE = new GUIImpl();

    private GUIProvider() {
    }

    /**
     * Gets the application GUI instance.
     *
     * @return GUI implementation
     */
    public static GUI getGUI() {
        return INSTANCE;
    }
}

