/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.util.Map;

public final class Icons {
    private static final String CUSTOM_PREFIX = "emuStudio.icon.";
    private static final Map<String, String> SYSTEM_KEYS = Map.of(
            "document-new.png", "FileView.fileIcon",
            "document-open.png", "Tree.openIcon",
            "document-save.png", "FileView.floppyDriveIcon",
            "computer.png", "FileView.computerIcon",
            "list-remove.png", "InternalFrame.closeIcon",
            "edit-delete.png", "InternalFrame.closeIcon"
    );

    private Icons() {
    }

    public static Icon loadIcon(String resource) {
        return loadIcon(resource, EmuStudioGui.class);
    }

    public static Icon loadIcon(String resource, Class<?> callerClass) {
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
        String systemKey = SYSTEM_KEYS.get(fileName);
        if (systemKey != null) {
            Icon systemIcon = UIManager.getIcon(systemKey);
            if (systemIcon != null) {
                return systemIcon;
            }
        }

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
