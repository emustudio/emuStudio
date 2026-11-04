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

import static org.junit.Assert.*;

public class AnnotationIndexTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static ProgramIndex emptyLike(ProgramIndex index) {
        ProgramIndex.Layout layout = index.snapshot().layout();
        return ProgramIndex.create(layout.imageId(), layout.addressSpaceSize(), layout.bankCount(), layout.commonBoundary());
    }

    @Test
    public void metadataLoadDoesNotReplayAgainstNewMemoryRevision() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, 1, 2);
        AnnotationIndex live = new AnnotationIndex(emptyLike(image));
        AnnotationIndex captured = new AnnotationIndex(emptyLike(image));
        captured.put(new AnnotationIndex.Entry(0, 0, "stale", "", "CODE"));
        ProgramIndex.Snapshot base = live.programIndex().snapshot();
        live.programIndex().memoryChanged();
        assertEquals(ProgramIndex.CommitResult.STALE, live.installProperties(captured.properties(), base));
        assertTrue(live.entries().isEmpty());
    }

    @Test
    public void metadataLoadPreservesCompilerMappingsAndOtherOwners() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, 1, 2);
        AnnotationIndex live = new AnnotationIndex(emptyLike(image));
        AnnotationIndex captured = new AnnotationIndex(emptyLike(image));
        captured.put(new AnnotationIndex.Entry(0, 0, "user", "", "CODE"));
        ProgramIndex index = live.programIndex();
        try (ProgramIndex.ContentUpdate edit = index.updateContent(index.snapshot())) {
            edit.putMapping(2, 7, new ProgramIndex.Range(0, 0, 1), new ProgramIndex.SourceSpan("file:///test.asm", 0, 0, 1));
            edit.putAnnotation(9, 8, new ProgramIndex.Range(0, 1, 2), "custom:note", "keep");
            edit.commit();
        }
        assertEquals(ProgramIndex.CommitResult.PUBLISHED, live.installProperties(captured.properties(), index.snapshot()));
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
        ProgramIndex image = ProgramIndex.create("test", 2, 2, 1);
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image));
        Properties input = annotations.properties();
        input.setProperty("version", "1");
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
        ProgramIndex image = ProgramIndex.create("test", 2, 1, 2);
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image));
        annotations.put(new AnnotationIndex.Entry(0, 0, "user", "notes", "DATA"));
        ProgramIndex index = annotations.programIndex();
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.putMapping(2, 1, new ProgramIndex.Range(0, 0, 1), new ProgramIndex.SourceSpan("file:/test", 1, 0, 1));
            update.putAnnotation(2, 1, new ProgramIndex.Range(0, 0, 1), ProgramIndex.Kinds.LABEL, "compiler");
            update.putAnnotation(99, 1, new ProgramIndex.Range(0, 0, 2), "custom:region", "external");
            assertEquals(ProgramIndex.CommitResult.PUBLISHED, update.commit());
        }
        List<ProgramIndex.Annotation> labels = new ArrayList<>();
        index.snapshot().forEachAnnotation(a -> {
            if (a.kind().equals(ProgramIndex.Kinds.LABEL) && a.text().equals("user")) labels.add(a);
        });
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
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 2, 2, 1));
        annotations.put(new AnnotationIndex.Entry(1, 1, "shared", "system data", "DATA"));

        AnnotationIndex.Entry shared = annotations.get(0, 1);
        assertEquals(0, shared.bank);
        assertEquals("shared", shared.label);
        assertSame(shared, annotations.get(1, 1));

        annotations.remove(1, 1);
        assertNull(annotations.get(0, 1));
    }

    @Test
    public void bankedRegionKeepsIndependentAnnotationsAtSameAddress() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 2, 2, 1));
        annotations.put(new AnnotationIndex.Entry(0, 0, "first", "", "CODE"));
        annotations.put(new AnnotationIndex.Entry(1, 0, "second", "", "DATA"));

        assertEquals("first", annotations.get(0, 0).label);
        assertEquals("second", annotations.get(1, 0).label);
        assertEquals(2, annotations.entries().size());
    }

    @Test
    public void searchMatchesLabelsAndCommentsWithoutCaseSensitivity() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 3, 1, 3));
        annotations.put(new AnnotationIndex.Entry(0, 0, "MainLoop", "", "CODE"));
        annotations.put(new AnnotationIndex.Entry(0, 1, "buffer", "Used by MAINLOOP", "DATA"));
        annotations.put(new AnnotationIndex.Entry(0, 2, "other", "", "UNKNOWN"));

        List<AnnotationIndex.Entry> matches = annotations.search("mainloop");

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).address);
        assertEquals(1, matches.get(1).address);
    }

    @Test
    public void savedAnnotationsRestoreEveryUserField() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, 1, 2);
        AnnotationIndex original = new AnnotationIndex(emptyLike(image));
        original.put(new AnnotationIndex.Entry(0, 1, "Entrée", "first line\nsecond line", "CODE"));
        Path file = temporary.newFile("annotations.properties").toPath();

        original.save(file);
        assertFalse(original.properties().containsKey("entry.0.bookmark"));
        AnnotationIndex restored = new AnnotationIndex(emptyLike(image));
        restored.load(file);

        AnnotationIndex.Entry entry = restored.get(0, 1);
        assertEquals(0, entry.bank);
        assertEquals(1, entry.address);
        assertEquals("Entrée", entry.label);
        assertEquals("first line\nsecond line", entry.comment);
        assertEquals("CODE", entry.kind);
    }

    @Test
    public void importRejectsAnotherImageWithoutReplacingExistingEntries() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 2, 1, 2));
        annotations.put(new AnnotationIndex.Entry(0, 0, "keep", "", "CODE"));
        AnnotationIndex other = new AnnotationIndex(ProgramIndex.create("other", 2, 1, 2));

        assertThrows(IOException.class, () -> annotations.importProperties(other.properties()));

        assertEquals("keep", annotations.get(0, 0).label);
        assertEquals(1, annotations.entries().size());
    }

    @Test
    public void malformedLaterEntryDoesNotImportEarlierValidEntry() {
        ProgramIndex image = ProgramIndex.create("test", 2, 1, 2);
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image));
        annotations.put(new AnnotationIndex.Entry(0, 0, "keep", "", "CODE"));
        AnnotationIndex imported = new AnnotationIndex(emptyLike(image));
        imported.put(new AnnotationIndex.Entry(0, 1, "new", "", "DATA"));
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
        ProgramIndex image = ProgramIndex.create("test", 2, 1, 2);
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image));
        annotations.put(new AnnotationIndex.Entry(0, 0, "old", "", "CODE"));
        annotations.put(new AnnotationIndex.Entry(0, 1, "keep", "", "DATA"));
        AnnotationIndex imported = new AnnotationIndex(emptyLike(image));
        imported.put(new AnnotationIndex.Entry(0, 0, "replacement", "", "UNKNOWN"));

        Properties legacy = imported.properties();
        legacy.setProperty("version", "1");
        annotations.importProperties(legacy);

        assertEquals("replacement", annotations.get(0, 0).label);
        assertEquals("keep", annotations.get(0, 1).label);
    }

    @Test
    public void invalidKindAndOutOfRangeAddressLeaveIndexEmpty() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 1, 1, 1));

        assertThrows(IllegalArgumentException.class,
                () -> annotations.put(new AnnotationIndex.Entry(0, 0, "", "", "FUNCTION")));
        assertThrows(IllegalArgumentException.class,
                () -> annotations.put(new AnnotationIndex.Entry(0, 1, "", "", "CODE")));
        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void overlappingRangesRemainIndependentAfterSaveAndReload() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 16, 1, 16);
        AnnotationIndex annotations = new AnnotationIndex(image);
        AnnotationIndex.Entry first = annotations.add(new AnnotationIndex.Entry(-1, 0, 2, 8, "first", "", "UNKNOWN"));
        AnnotationIndex.Entry second = annotations.add(new AnnotationIndex.Entry(-1, 0, 2, 5, "second", "", "UNKNOWN"));
        Properties saved = annotations.properties();
        AnnotationIndex restored = new AnnotationIndex(emptyLike(image));

        assertEquals(ProgramIndex.CommitResult.PUBLISHED,
                restored.installProperties(saved, restored.programIndex().snapshot()));
        restored.update(new AnnotationIndex.Entry(first.id, 0, 3, 9, "changed", "", "UNKNOWN"));
        restored.remove(second);

        assertEquals(1, restored.entries().size());
        assertEquals(first.id, restored.entries().get(0).id);
        assertEquals(3, restored.entries().get(0).address);
        assertEquals(9, restored.entries().get(0).end);
        assertEquals("changed", restored.entries().get(0).label);
    }

    @Test
    public void rangesCrossingBankBoundaryAreRejectedWithoutChanges() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 8, 2, 4));

        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new AnnotationIndex.Entry(-1, 1, 3, 5, "crossing", "", "UNKNOWN")));

        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void bankZeroRangeCanCrossIntoCommonMemory() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 8, 2, 4));

        AnnotationIndex.Entry entry = annotations.add(new AnnotationIndex.Entry(-1, 0, 3, 5, "shared", "", "UNKNOWN"));

        List<ProgramIndex.Annotation> covering = new ArrayList<>();
        annotations.programIndex().snapshot().forEachAnnotationCovering(1, 4, covering::add);
        assertTrue(covering.stream().anyMatch(annotation -> annotation.entryId() == entry.id));
    }

    @Test
    public void importingVersionTwoAllocatesIndependentIdentities() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 8, 1, 8);
        AnnotationIndex annotations = new AnnotationIndex(image);
        AnnotationIndex.Entry original = annotations.add(new AnnotationIndex.Entry(-1, 0, 1, 4, "original", "", "UNKNOWN"));
        Properties imported = annotations.properties();
        imported.setProperty("entry.0.label", "imported");

        annotations.importProperties(imported);

        assertEquals(2, annotations.entries().size());
        assertEquals("original", annotations.entries().stream().filter(entry -> entry.id == original.id).findFirst().get().label);
        assertTrue(annotations.entries().stream().anyMatch(entry -> entry.id != original.id && entry.label.equals("imported")));
    }
}
