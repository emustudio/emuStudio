/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.Icon;
import javax.swing.UIManager;

public final class Icons {
    public static final int SIZE = 22;
    private static final String CUSTOM_PREFIX = "emuStudio.icon.";

    private Icons() {
    }

    public static Icon loadIcon(String resource) {
        return loadIcon(resource, EmuStudioGui.class);
    }

    public static Icon loadIcon(String resource, Class<?> callerClass) {
        String customKey = customKey(resource);
        Icon customIcon = UIManager.getIcon(customKey);
        if (customIcon != null) {
            return customIcon;
        }

        Icon fallback = GUI.loadIcon(resource, callerClass);
        if (fallback != null) {
            UIManager.put(customKey, fallback);
        }
        return fallback;
    }

    public static String customKey(String resource) {
        return CUSTOM_PREFIX + resource;
    }
}
