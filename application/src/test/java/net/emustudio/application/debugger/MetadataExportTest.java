/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.*;

public class MetadataExportTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void exportWritesSelectedLogicalBankAndPortableAnnotationMetadata() throws Exception {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2, 9}, {3, 4, 9}}, 2);
        AnnotationIndex annotations = new AnnotationIndex(image);
        annotations.put(new AnnotationIndex.Entry(1, 0, "start", "entry point", true, "CODE"));
        Path directory = temporary.getRoot().toPath().resolve("export");

        MetadataExport.export(directory, image, annotations, "z80", 1, 4096);

        assertArrayEquals(new byte[] {3, 4, 9}, Files.readAllBytes(directory.resolve("memory-bank-1.bin")));
        Properties metadata = AtomicProperties.load(directory.resolve("memory-bank-1.properties"));
        assertEquals("z80", metadata.getProperty("processor"));
        assertEquals("4096", metadata.getProperty("base"));
        assertEquals("1", metadata.getProperty("bank"));
        assertEquals("3", metadata.getProperty("size"));
        assertEquals("2", metadata.getProperty("common"));
        assertEquals("memory-bank-1.bin", metadata.getProperty("binary"));
        AnnotationIndex imported = new AnnotationIndex(image);
        imported.importProperties(metadata);
        assertEquals("start", imported.get(1, 0).label);
        assertEquals("entry point", imported.get(1, 0).comment);
        assertTrue(imported.get(1, 0).bookmark);
    }

    @Test
    public void blankProcessorIsRejectedBeforeCreatingDirectory() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1}}, 1);
        Path directory = temporary.getRoot().toPath().resolve("export");

        assertThrows(IllegalArgumentException.class,
                () -> MetadataExport.export(directory, image, new AnnotationIndex(image), "  ", 0, 0));

        assertFalse(Files.exists(directory));
    }

    @Test
    public void invalidAddressRangeIsRejectedBeforeCreatingDirectory() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        Path directory = temporary.getRoot().toPath().resolve("export");

        assertThrows(IllegalArgumentException.class,
                () -> MetadataExport.export(directory, image, new AnnotationIndex(image), "z80", 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> MetadataExport.export(directory, image, new AnnotationIndex(image), "z80", 0, Integer.MAX_VALUE));

        assertFalse(Files.exists(directory));
    }

    @Test
    public void invalidBankIsRejectedBeforeCreatingDirectory() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1}}, 1);
        Path directory = temporary.getRoot().toPath().resolve("export");

        assertThrows(IllegalArgumentException.class,
                () -> MetadataExport.export(directory, image, new AnnotationIndex(image), "z80", 1, 0));

        assertFalse(Files.exists(directory));
    }
}
