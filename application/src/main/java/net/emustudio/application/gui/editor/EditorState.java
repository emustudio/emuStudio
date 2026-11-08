/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.settings.AppSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

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
    String text(String name, String fallback) { return settings == null ? fallback : settings.getString("editor" + name).orElse(fallback); }
    void setText(String name, String value) { if (settings != null) settings.set("editor" + name, value); }
    private String key(Path path) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(path.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
    }
    String syntax(Path path) { return text("Syntax" + key(path), SourceFoldParser.COMPILER_STYLE); }
    void syntax(Path path, String style) { setText("Syntax" + key(path), style); }
}
