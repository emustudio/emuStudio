/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

final class AtomicProperties {
    private AtomicProperties() {}

    static Properties load(Path path) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            p.load(reader);
        }
        return p;
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
