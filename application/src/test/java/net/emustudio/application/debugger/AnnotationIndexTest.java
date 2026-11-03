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
import net.emustudio.application.debugger.index.ProgramIndex;
import java.util.ArrayList;
import net.emustudio.application.debugger.index.ProgramIndex;

import static org.junit.Assert.*;

public class AnnotationIndexTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void capturedInstallDoesNotReplayAgainstNewMemoryRevision() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        AnnotationIndex live = new AnnotationIndex(image);
        AnnotationIndex captured = new AnnotationIndex(image);
        captured.put(new AnnotationIndex.Entry(0, 0, "stale", "", false, "CODE"));
        ProgramIndex.Snapshot base = live.programIndex().snapshot();
        live.programIndex().memoryChanged();
        assertEquals(ProgramIndex.CommitResult.STALE, live.installCaptured(captured, base));
        assertTrue(live.entries().isEmpty());
    }

    @Test
    public void capturedInstallPreservesCompilerMappingsAndOtherOwners() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        AnnotationIndex live = new AnnotationIndex(image);
        AnnotationIndex captured = new AnnotationIndex(image);
        captured.put(new AnnotationIndex.Entry(0, 0, "user", "", true, "CODE"));
        ProgramIndex index = live.programIndex();
        try (ProgramIndex.ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putMapping(2, 7, new ProgramIndex.Range(0, 0, 1), new ProgramIndex.SourceSpan("file:///test.asm", 0, 0, 1));
            edit.putAnnotation(9, 8, new ProgramIndex.Range(0, 1, 2), "custom:note", "keep");
            edit.commit();
        }
        assertEquals(ProgramIndex.CommitResult.PUBLISHED, live.installCaptured(captured, index.snapshot()));
        assertEquals("user", live.get(0, 0).label);
        java.util.ArrayList<ProgramIndex.Mapping> mappings = new java.util.ArrayList<>();
        index.snapshot().forEachMapping(mappings::add);
        assertEquals(1, mappings.size());
        java.util.ArrayList<ProgramIndex.Annotation> other = new java.util.ArrayList<>();
        index.snapshot().forEachAnnotation(annotation -> { if (annotation.ownerId() == 9) other.add(annotation); });
        assertEquals("keep", other.get(0).text());
    }

    @Test
    public void duplicateImportedPhysicalAddressesKeepOnlyLastValue() throws Exception {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}, {3, 2}}, 1);
        AnnotationIndex annotations = new AnnotationIndex(image);
        Properties input = annotations.properties();
        input.setProperty("entries", "2");
        for (int i = 0; i < 2; i++) {
            input.setProperty("entry." + i + ".bank", Integer.toString(i));
            input.setProperty("entry." + i + ".address", "1");
            input.setProperty("entry." + i + ".label", "value" + i);
        }
        annotations.importProperties(input);
        assertEquals(1, annotations.entries().size());
        assertEquals("value1", annotations.get(0, 1).label);
        assertSame(annotations.get(0, 1), annotations.get(1, 1));
    }

    @Test
    public void clearingUserMetadataPreservesCompilerAndOtherOwners() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        AnnotationIndex annotations = new AnnotationIndex(image);
        annotations.put(new AnnotationIndex.Entry(0, 0, "user", "notes", true, "DATA"));
        ProgramIndex index = annotations.programIndex();
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.putMapping(2, 1, new ProgramIndex.Range(0, 0, 1), new ProgramIndex.SourceSpan("file:/test", 1, 0, 1));
            update.putAnnotation(2, 1, new ProgramIndex.Range(0, 0, 1), ProgramIndex.Kinds.LABEL, "compiler");
            update.putAnnotation(99, 1, new ProgramIndex.Range(0, 0, 2), "custom:region", "external");
            assertEquals(ProgramIndex.CommitResult.PUBLISHED, update.commit());
        }
        List<ProgramIndex.Annotation> labels = new ArrayList<>();
        index.snapshot().forEachLabelNamed("user", labels::add);
        assertEquals(1, labels.size());
        annotations.clear();
        assertNull(annotations.get(0, 0));
        assertTrue(annotations.entries().isEmpty());
        labels.clear();
        index.snapshot().forEachAnnotation(labels::add);
        assertEquals(2, labels.size());
        List<ProgramIndex.Mapping> mappings = new ArrayList<>();
        index.snapshot().forEachMapping(mappings::add);
        assertEquals(1, mappings.size());
    }

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
