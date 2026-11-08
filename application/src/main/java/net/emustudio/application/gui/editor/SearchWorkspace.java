/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Reads immutable snapshots and checks them again before applying reviewed replacements.
 */
final class SearchWorkspace {
    static final class Snapshot {
        final Path path;
        final REditor buffer;
        final String text;

        Snapshot(Path path, REditor buffer, String text) {
            this.path = path;
            this.buffer = buffer;
            this.text = text;
        }

        String title() {
            return path == null ? "Untitled" : path.toString();
        }

        /**
         * 1-based line number of the offset in this snapshot.
         */
        int line(int offset) {
            return 1 + (int) text.substring(0, offset).chars().filter(ch -> ch == '\n').count();
        }
    }

    static List<Snapshot> open(List<REditor> editors) {
        List<Snapshot> result = new ArrayList<>();
        for (REditor editor : editors)
            result.add(new Snapshot(editor.getCurrentFile().map(file -> file.toPath().toAbsolutePath().normalize()).orElse(null), editor, editor.textPane().getText()));
        return result;
    }

    static void apply(Map<Snapshot, List<EditorSearch.Match>> reviewed) throws IOException {
        Map<Path, Path> temporary = new LinkedHashMap<>();
        Map<Path, String> originals = new LinkedHashMap<>();
        try {
            // Validate the entire batch before changing any buffer or file.
            for (Map.Entry<Snapshot, List<EditorSearch.Match>> entry : reviewed.entrySet()) {
                Snapshot snapshot = entry.getKey();
                String current = snapshot.buffer == null ? Files.readString(snapshot.path) : snapshot.buffer.textPane().getText();
                if (!current.equals(snapshot.text))
                    throw new IOException("Source changed since preview: " + snapshot.title() + ". Search again.");
                String replacement = EditorSearch.apply(snapshot.text, entry.getValue());
                if (snapshot.buffer == null && !entry.getValue().isEmpty()) {
                    Path staged = Files.createTempFile(snapshot.path.getParent(), ".emustudio-edit-", ".tmp");
                    temporary.put(snapshot.path, staged);
                    originals.put(snapshot.path, snapshot.text);
                    Files.copy(snapshot.path, staged, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    Files.writeString(staged, replacement);
                }
            }
            List<Path> applied = new ArrayList<>();
            try {
                for (Map.Entry<Path, Path> entry : temporary.entrySet()) {
                    if (!Files.readString(entry.getKey()).equals(originals.get(entry.getKey())))
                        throw new IOException("Source changed during replacement: " + entry.getKey());
                    try {
                        Files.move(entry.getValue(), entry.getKey(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(entry.getValue(), entry.getKey(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    applied.add(entry.getKey());
                }
            } catch (IOException e) {
                for (Path path : applied) {
                    try {
                        Files.writeString(path, originals.get(path));
                    } catch (IOException rollback) {
                        e.addSuppressed(rollback);
                    }
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
