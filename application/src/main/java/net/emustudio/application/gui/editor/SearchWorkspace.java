/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Reads immutable snapshots and checks them again before applying reviewed replacements. */
final class SearchWorkspace {
    static final class Snapshot {
        final Path path;
        final REditor buffer;
        final String text;
        final List<EditorSearch.Match> matches = new ArrayList<>();
        Snapshot(Path path, REditor buffer, String text) { this.path = path; this.buffer = buffer; this.text = text; }
        String title() { return path == null ? "Untitled" : path.toString(); }
        /** 1-based line number of the offset in this snapshot. */
        int line(int offset) { return 1 + (int) text.substring(0, offset).chars().filter(ch -> ch == '\n').count(); }
    }
    static List<Snapshot> open(List<REditor> editors) {
        List<Snapshot> result = new ArrayList<>();
        for (REditor editor : editors) result.add(new Snapshot(editor.getCurrentFile().map(file -> file.toPath().toAbsolutePath().normalize()).orElse(null), editor, editor.textPane().getText()));
        return result;
    }
    static List<Snapshot> folder(Path root, String includes, String excludes, List<Snapshot> open) throws IOException {
        List<PathMatcher> include = matchers(includes), exclude = matchers(excludes);
        Map<Path, Snapshot> buffers = new HashMap<>();
        for (Snapshot snapshot : open) if (snapshot.path != null) buffers.put(snapshot.path, snapshot);
        List<Snapshot> result = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            Iterator<Path> iterator = paths.filter(Files::isRegularFile).filter(path -> !Files.isSymbolicLink(path)).sorted().iterator();
            while (iterator.hasNext()) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Search cancelled");
                Path path = iterator.next().toAbsolutePath().normalize(), relative = root.toAbsolutePath().normalize().relativize(path);
                if (relative.toString().contains(".git" + File.separator) || !include.isEmpty() && !matches(include, relative) || matches(exclude, relative)) continue;
                Snapshot buffer = buffers.get(path);
                if (buffer != null) result.add(buffer);
                else {
                    try { String text = Files.readString(path, StandardCharsets.UTF_8); if (text.indexOf('\0') < 0) result.add(new Snapshot(path, null, text)); }
                    catch (MalformedInputException e) { /* Not a UTF-8 text file. */ }
                }
            }
        }
        return result;
    }
    private static List<PathMatcher> matchers(String patterns) {
        List<PathMatcher> result = new ArrayList<>();
        for (String pattern : patterns.split(";")) if (!pattern.isBlank()) result.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern.trim()));
        return result;
    }
    private static boolean matches(List<PathMatcher> matchers, Path path) {
        return matchers.stream().anyMatch(matcher -> matcher.matches(path) || matcher.matches(path.getFileName()));
    }
    static void apply(Map<Snapshot, List<EditorSearch.Match>> reviewed) throws IOException {
        Map<Path, Path> temporary = new LinkedHashMap<>();
        Map<Path, String> originals = new LinkedHashMap<>();
        try {
            // Validate the entire batch before changing any buffer or file.
            for (Map.Entry<Snapshot, List<EditorSearch.Match>> entry : reviewed.entrySet()) {
                Snapshot snapshot = entry.getKey();
                String current = snapshot.buffer == null ? Files.readString(snapshot.path) : snapshot.buffer.textPane().getText();
                if (!current.equals(snapshot.text)) throw new IOException("Source changed since preview: " + snapshot.title() + ". Search again.");
                String replacement = EditorSearch.apply(snapshot.text, entry.getValue());
                if (snapshot.buffer == null && !entry.getValue().isEmpty()) {
                    Path staged = Files.createTempFile(snapshot.path.getParent(), ".emustudio-edit-", ".tmp");
                    temporary.put(snapshot.path, staged); originals.put(snapshot.path, snapshot.text);
                    Files.copy(snapshot.path, staged, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    Files.writeString(staged, replacement);
                }
            }
            List<Path> applied = new ArrayList<>();
            try {
                for (Map.Entry<Path, Path> entry : temporary.entrySet()) {
                    if (!Files.readString(entry.getKey()).equals(originals.get(entry.getKey()))) throw new IOException("Source changed during replacement: " + entry.getKey());
                    try { Files.move(entry.getValue(), entry.getKey(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                    catch (AtomicMoveNotSupportedException e) { Files.move(entry.getValue(), entry.getKey(), StandardCopyOption.REPLACE_EXISTING); }
                    applied.add(entry.getKey());
                }
            } catch (IOException e) {
                for (Path path : applied) {
                    try { Files.writeString(path, originals.get(path)); }
                    catch (IOException rollback) { e.addSuppressed(rollback); }
                }
                throw e;
            }
            for (Map.Entry<Snapshot, List<EditorSearch.Match>> entry : reviewed.entrySet()) {
                REditor buffer = entry.getKey().buffer;
                if (buffer == null || entry.getValue().isEmpty()) continue;
                buffer.commands().selections().clear();
                EditorSearch.apply(buffer.textPane(), entry.getValue());
            }
        } finally {
            for (Path staged : temporary.values()) Files.deleteIfExists(staged);
        }
    }
}
