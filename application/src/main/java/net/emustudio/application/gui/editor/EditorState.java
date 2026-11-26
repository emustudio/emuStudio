/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.settings.AppSettings;

/** Editor preferences use the existing application settings store. */
final class EditorState {
    private final AppSettings settings;
    EditorState(AppSettings settings) {
        this.settings = settings;
    }
    boolean flag(String name, boolean fallback) { return settings == null ? fallback : settings.getBoolean("editor" + name).orElse(fallback); }
    void setFlag(String name, boolean value) { if (settings != null) settings.set("editor" + name, value); }
    int number(String name, int fallback) { return settings == null ? fallback : settings.getInt("editor" + name).orElse(fallback); }
    void setNumber(String name, int value) { if (settings != null) settings.set("editor" + name, value); }
}
