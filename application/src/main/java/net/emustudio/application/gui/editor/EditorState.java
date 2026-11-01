/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.application.settings.AppSettings;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/** Editor preferences and per-file bookmarks use the existing application settings store. */
final class EditorState {
    private final AppSettings settings;
    private final Properties bookmarks = new Properties();
    EditorState(AppSettings settings) {
        this.settings = settings;
        if (settings != null) {
            try { bookmarks.load(new StringReader(settings.getString("editorBookmarks", ""))); }
            catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }
    boolean flag(String name, boolean fallback) { return settings == null ? fallback : settings.getBoolean("editor" + name, fallback); }
    void setFlag(String name, boolean value) { if (settings != null) settings.setBoolean("editor" + name, value); }
    int number(String name, int fallback) { return settings == null ? fallback : settings.getInt("editor" + name, fallback); }
    void setNumber(String name, int value) { if (settings != null) settings.setInt("editor" + name, value); }
    String text(String name, String fallback) { return settings == null ? fallback : settings.getString("editor" + name, fallback); }
    void setText(String name, String value) { if (settings != null) settings.setString("editor" + name, value); }
    private String key(Path path) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(path.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
    }
    String bookmarks(Path path) { return bookmarks.getProperty(key(path), ""); }
    void bookmarks(Path path, String value) {
        bookmarks.setProperty(key(path), value);
        if (settings != null) {
            try {
                StringWriter writer = new StringWriter(); bookmarks.store(writer, null);
                settings.setString("editorBookmarks", writer.toString());
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }
}
