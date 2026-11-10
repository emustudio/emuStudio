/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

final class AtomicProperties {
    private AtomicProperties() {}

    static Properties load(Path path) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            p.load(reader);
        }
        return p;
    }

    /** Ascending indices i of present keys prefix + i + suffix. */
    static List<Integer> indices(Properties properties, String prefix, String suffix) {
        Set<Integer> result = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(prefix) || !key.endsWith(suffix) || key.length() <= prefix.length() + suffix.length()) continue;
            String index = key.substring(prefix.length(), key.length() - suffix.length());
            if (!index.chars().allMatch(c -> c >= '0' && c <= '9') || index.length() > 9) continue;
            result.add(Integer.parseInt(index));
        }
        return new ArrayList<>(result);
    }

    static void save(Path path, Properties properties) throws IOException {
        Path target = path.toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), ".emustudio-debug-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp)) {
                properties.store(writer, "emuStudio debugger metadata");
            }
            try {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
