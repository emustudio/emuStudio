/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class AnnotationIndexTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void commonRegionAnnotationIsSharedAcrossBanksAndRemovedThroughEitherBank() {
        AnnotationIndex annotations = new AnnotationIndex(new MemoryAnalysis(new byte[][] {{1, 2}, {3, 2}}, 1));
        annotations.put(new AnnotationIndex.Entry(1, 1, "shared", "system data", true, "DATA"));

        AnnotationIndex.Entry shared = annotations.get(0, 1);
        assertEquals(0, shared.bank);
        assertEquals("shared", shared.label);
        assertSame(shared, annotations.get(1, 1));

        annotations.remove(1, 1);
        assertNull(annotations.get(0, 1));
    }

    @Test
    public void bankedRegionKeepsIndependentAnnotationsAtSameAddress() {
        AnnotationIndex annotations = new AnnotationIndex(new MemoryAnalysis(new byte[][] {{1, 2}, {3, 2}}, 1));
        annotations.put(new AnnotationIndex.Entry(0, 0, "first", "", false, "CODE"));
        annotations.put(new AnnotationIndex.Entry(1, 0, "second", "", false, "DATA"));

        assertEquals("first", annotations.get(0, 0).label);
        assertEquals("second", annotations.get(1, 0).label);
        assertEquals(2, annotations.entries().size());
    }

    @Test
    public void searchMatchesLabelsAndCommentsWithoutCaseSensitivity() {
        AnnotationIndex annotations = new AnnotationIndex(new MemoryAnalysis(new byte[][] {{1, 2, 3}}, 3));
        annotations.put(new AnnotationIndex.Entry(0, 0, "MainLoop", "", false, "CODE"));
        annotations.put(new AnnotationIndex.Entry(0, 1, "buffer", "Used by MAINLOOP", false, "DATA"));
        annotations.put(new AnnotationIndex.Entry(0, 2, "other", "", false, "UNKNOWN"));

        List<AnnotationIndex.Entry> matches = annotations.search("mainloop");

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).address);
        assertEquals(1, matches.get(1).address);
    }

    @Test
    public void savedAnnotationsRestoreEveryUserField() throws Exception {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        AnnotationIndex original = new AnnotationIndex(image);
        original.put(new AnnotationIndex.Entry(0, 1, "Entrée", "first line\nsecond line", true, "CODE"));
        Path file = temporary.newFile("annotations.properties").toPath();

        original.save(file);
        AnnotationIndex restored = new AnnotationIndex(image);
        restored.load(file);

        AnnotationIndex.Entry entry = restored.get(0, 1);
        assertEquals(0, entry.bank);
        assertEquals(1, entry.address);
        assertEquals("Entrée", entry.label);
        assertEquals("first line\nsecond line", entry.comment);
        assertTrue(entry.bookmark);
        assertEquals("CODE", entry.kind);
    }

    @Test
    public void importRejectsAnotherImageWithoutReplacingExistingEntries() {
        AnnotationIndex annotations = new AnnotationIndex(new MemoryAnalysis(new byte[][] {{1, 2}}, 2));
        annotations.put(new AnnotationIndex.Entry(0, 0, "keep", "", false, "CODE"));
        AnnotationIndex other = new AnnotationIndex(new MemoryAnalysis(new byte[][] {{9, 2}}, 2));

        assertThrows(IOException.class, () -> annotations.importProperties(other.properties()));

        assertEquals("keep", annotations.get(0, 0).label);
        assertEquals(1, annotations.entries().size());
    }

    @Test
    public void malformedLaterEntryDoesNotImportEarlierValidEntry() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        AnnotationIndex annotations = new AnnotationIndex(image);
        annotations.put(new AnnotationIndex.Entry(0, 0, "keep", "", false, "CODE"));
        AnnotationIndex imported = new AnnotationIndex(image);
        imported.put(new AnnotationIndex.Entry(0, 1, "new", "", false, "DATA"));
        Properties properties = imported.properties();
        properties.setProperty("entries", "2");
        properties.setProperty("entry.1.bank", "0");
        properties.setProperty("entry.1.address", "invalid");

        assertThrows(IOException.class, () -> annotations.importProperties(properties));

        assertNull(annotations.get(0, 1));
        assertEquals("keep", annotations.get(0, 0).label);
    }

    @Test
    public void importMergesEntriesAndReplacesMatchingAddress() throws Exception {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        AnnotationIndex annotations = new AnnotationIndex(image);
        annotations.put(new AnnotationIndex.Entry(0, 0, "old", "", false, "CODE"));
        annotations.put(new AnnotationIndex.Entry(0, 1, "keep", "", false, "DATA"));
        AnnotationIndex imported = new AnnotationIndex(image);
        imported.put(new AnnotationIndex.Entry(0, 0, "replacement", "", true, "UNKNOWN"));

        annotations.importProperties(imported.properties());

        assertEquals("replacement", annotations.get(0, 0).label);
        assertTrue(annotations.get(0, 0).bookmark);
        assertEquals("keep", annotations.get(0, 1).label);
    }

    @Test
    public void invalidKindAndOutOfRangeAddressLeaveIndexEmpty() {
        AnnotationIndex annotations = new AnnotationIndex(new MemoryAnalysis(new byte[][] {{1}}, 1));

        assertThrows(IllegalArgumentException.class,
                () -> annotations.put(new AnnotationIndex.Entry(0, 0, "", "", false, "FUNCTION")));
        assertThrows(IllegalArgumentException.class,
                () -> annotations.put(new AnnotationIndex.Entry(0, 1, "", "", false, "CODE")));
        assertTrue(annotations.entries().isEmpty());
    }
}
