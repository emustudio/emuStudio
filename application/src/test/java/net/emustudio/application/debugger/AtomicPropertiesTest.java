/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class AtomicPropertiesTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void saveReplacesPreviousFileAndPreservesEscapedUnicodeValues() throws Exception {
        Path file = temporary.newFile("state.properties").toPath();
        Files.writeString(file, "obsolete=old\n");
        Properties properties = new Properties();
        properties.setProperty("label", "žluťoučký = debugger\nsecond line");

        AtomicProperties.save(file, properties);

        Properties restored = AtomicProperties.load(file);
        assertEquals("žluťoučký = debugger\nsecond line", restored.getProperty("label"));
        assertNull(restored.getProperty("obsolete"));
        try (Stream<Path> files = Files.list(temporary.getRoot().toPath())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void failedReplacementCleansTemporaryFileAndPreservesTarget() throws Exception {
        Path target = temporary.newFolder("state").toPath();
        Path existing = target.resolve("keep.txt");
        Files.writeString(existing, "keep");

        assertThrows(IOException.class, () -> AtomicProperties.save(target, new Properties()));

        assertEquals("keep", Files.readString(existing));
        try (Stream<Path> files = Files.list(temporary.getRoot().toPath())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void missingFileReportsIoFailure() {
        Path missing = temporary.getRoot().toPath().resolve("missing.properties");

        assertThrows(IOException.class, () -> AtomicProperties.load(missing));
    }
}
