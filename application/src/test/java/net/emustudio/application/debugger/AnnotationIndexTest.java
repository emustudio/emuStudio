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
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class AnnotationIndexTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static ProgramIndex emptyLike(ProgramIndex index) {
        ProgramIndex.Layout layout = index.snapshot().layout();
        return ProgramIndex.create(layout.imageId(), layout.addressSpaceSize(), new MemoryAnnotations());
    }

    @Test
    public void replacingCompilerAnnotationsPreservesUserMetadata() {
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 8, new MemoryAnnotations()), engine);
        annotations.put(new AnnotationIndex.Entry(1, "user", "CODE"));
        engine.put(new MemoryAnnotations.Entry(10, 1, 1, 2, "source", "old", null));

        engine.replaceOwner(10, List.of(new MemoryAnnotations.Entry(10, 2, 2, 3, "source", "new", null)));

        assertEquals("user", annotations.get(1).label);
        assertNull(engine.get(10, 1));
        assertEquals("new", engine.get(10, 2).text);
    }

    @Test
    public void loadingMetadataRejectsConcurrentEngineChange() throws Exception {
        MemoryAnnotations engine = new MemoryAnnotations();
        ProgramIndex index = ProgramIndex.create("test", 8, new MemoryAnnotations());
        AnnotationIndex live = new AnnotationIndex(index, engine);
        AnnotationIndex saved = new AnnotationIndex(ProgramIndex.create("test", 8, new MemoryAnnotations()));
        saved.put(new AnnotationIndex.Entry(1, "saved", "DATA"));
        long revision = engine.revision();
        ProgramIndex.Snapshot base = index.snapshot();
        engine.put(new MemoryAnnotations.Entry(42, 1, 0, 1, "custom", "changed", null));

        ProgramIndex.CommitResult result = live.installProperties(saved.properties(), base, revision);

        assertEquals(ProgramIndex.CommitResult.STALE, result);
        assertNull(live.get(1));
        assertEquals("changed", engine.get(42, 1).text);
    }

    @Test
    public void metadataLoadDoesNotReplayAgainstNewMemoryRevision() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, new MemoryAnnotations());
        AnnotationIndex live = new AnnotationIndex(emptyLike(image));
        AnnotationIndex captured = new AnnotationIndex(emptyLike(image));
        captured.put(new AnnotationIndex.Entry(0, "stale", "CODE"));
        ProgramIndex.Snapshot base = live.programIndex().snapshot();
        live.programIndex().memoryChanged();
        assertEquals(ProgramIndex.CommitResult.STALE, live.installProperties(captured.properties(), base));
        assertTrue(live.entries().isEmpty());
    }

    @Test
    public void metadataLoadPreservesCompilerMappingsAndOtherOwners() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, new MemoryAnnotations());
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex live = new AnnotationIndex(emptyLike(image), engine);
        AnnotationIndex captured = new AnnotationIndex(emptyLike(image));
        captured.put(new AnnotationIndex.Entry(0, "user", "CODE"));
        ProgramIndex index = live.programIndex();
        engine.put(new MemoryAnnotations.Entry(9, 8, 1, 2, "custom:note", "keep", null));
        assertEquals(ProgramIndex.CommitResult.PUBLISHED, live.installProperties(captured.properties(), index.snapshot()));
        assertEquals("user", live.get(0).label);
        assertEquals("keep", engine.get(9, 8).text);
    }

    @Test
    public void importRejectsObsoleteFormatWithoutChangingMetadata() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, new MemoryAnnotations());
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image));
        Properties input = annotations.properties();
        input.setProperty("version", "1");
        input.setProperty("entries", "2");
        for (int i = 0; i < 2; i++) {
            input.setProperty("entry." + i + ".address", "1");
            input.setProperty("entry." + i + ".label", "value" + i);
        }
        assertThrows(IOException.class, () -> annotations.importProperties(input));
        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void clearingUserMetadataPreservesCompilerAndOtherOwners() {
        ProgramIndex image = ProgramIndex.create("test", 2, new MemoryAnnotations());
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image), engine);
        annotations.put(new AnnotationIndex.Entry(0, "user", "DATA"));
        engine.put(new MemoryAnnotations.Entry(2, 1, 0, 1, "label", "compiler", null));
        engine.put(new MemoryAnnotations.Entry(99, 1, 0, 2, "custom:region", "external", null));

        annotations.clear();

        assertNull(annotations.get(0));
        assertTrue(annotations.entries().isEmpty());
        assertEquals("compiler", engine.get(2, 1).text);
        assertEquals("external", engine.get(99, 1).text);
    }

    @Test
    public void annotationIsFoundAndRemovedAtItsCpuVisibleAddress() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 2, new MemoryAnnotations()));
        annotations.put(new AnnotationIndex.Entry(1, "shared", "DATA"));

        AnnotationIndex.Entry shared = annotations.get(1);
        assertEquals("shared", shared.label);
        assertEquals(shared.id, annotations.get(1).id);

        annotations.remove(1);
        assertNull(annotations.get(1));
    }

    @Test
    public void puttingAtSameAddressReplacesExistingAnnotation() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 2, new MemoryAnnotations()));
        annotations.put(new AnnotationIndex.Entry(0, "first", "CODE"));
        annotations.put(new AnnotationIndex.Entry(0, "second", "DATA"));

        assertEquals("second", annotations.get(0).label);
        assertEquals(1, annotations.entries().size());
    }

    @Test
    public void searchMatchesLabelsWithoutCaseSensitivity() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 3, new MemoryAnnotations()));
        annotations.put(new AnnotationIndex.Entry(0, "MainLoop", "CODE"));
        annotations.put(new AnnotationIndex.Entry(1, "MainLoop buffer", "DATA"));
        annotations.put(new AnnotationIndex.Entry(2, "other", "UNKNOWN"));

        List<AnnotationIndex.Entry> matches = annotations.search("mainloop");

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).address);
        assertEquals(1, matches.get(1).address);
    }

    @Test
    public void savedAnnotationsRestoreEveryUserField() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, new MemoryAnnotations());
        AnnotationIndex original = new AnnotationIndex(emptyLike(image));
        original.put(new AnnotationIndex.Entry(1, "Entrée\nsecond line", "CODE"));
        Path file = temporary.newFile("annotations.properties").toPath();

        original.save(file);
        assertFalse(original.properties().containsKey("entry.0.bookmark"));
        AnnotationIndex restored = new AnnotationIndex(emptyLike(image));
        restored.load(file);

        AnnotationIndex.Entry entry = restored.get(1);
        assertEquals(1, entry.address);
        assertEquals("Entrée\nsecond line", entry.label);
        assertEquals("CODE", entry.kind);
    }

    @Test
    public void importRejectsAnotherImageWithoutReplacingExistingEntries() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 2, new MemoryAnnotations()));
        annotations.put(new AnnotationIndex.Entry(0, "keep", "CODE"));
        AnnotationIndex other = new AnnotationIndex(ProgramIndex.create("other", 2, new MemoryAnnotations()));

        assertThrows(IOException.class, () -> annotations.importProperties(other.properties()));

        assertEquals("keep", annotations.get(0).label);
        assertEquals(1, annotations.entries().size());
    }

    @Test
    public void malformedLaterEntryDoesNotImportEarlierValidEntry() {
        ProgramIndex image = ProgramIndex.create("test", 2, new MemoryAnnotations());
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image));
        annotations.put(new AnnotationIndex.Entry(0, "keep", "CODE"));
        AnnotationIndex imported = new AnnotationIndex(emptyLike(image));
        imported.put(new AnnotationIndex.Entry(1, "new", "DATA"));
        Properties properties = imported.properties();
        properties.setProperty("entries", "2");
        properties.setProperty("entry.1.address", "invalid");

        assertThrows(IOException.class, () -> annotations.importProperties(properties));

        assertNull(annotations.get(1));
        assertEquals("keep", annotations.get(0).label);
    }

    @Test
    public void importKeepsIndependentAnnotationsAtTheSameAddress() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 2, new MemoryAnnotations());
        AnnotationIndex annotations = new AnnotationIndex(emptyLike(image));
        annotations.put(new AnnotationIndex.Entry(0, "old", "CODE"));
        annotations.put(new AnnotationIndex.Entry(1, "keep", "DATA"));
        AnnotationIndex imported = new AnnotationIndex(emptyLike(image));
        imported.put(new AnnotationIndex.Entry(0, "replacement", "UNKNOWN"));

        annotations.importProperties(imported.properties());

        assertEquals(3, annotations.entries().size());
        assertTrue(annotations.entries().stream().anyMatch(entry -> entry.label.equals("replacement")));
        assertTrue(annotations.entries().stream().anyMatch(entry -> entry.label.equals("old")));
        assertEquals("keep", annotations.get(1).label);
    }

    @Test
    public void invalidKindAndOutOfRangeAddressLeaveIndexEmpty() {
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 1, new MemoryAnnotations()));

        assertThrows(IllegalArgumentException.class,
                () -> annotations.put(new AnnotationIndex.Entry(0, "", "FUNCTION")));
        assertThrows(IllegalArgumentException.class,
                () -> annotations.put(new AnnotationIndex.Entry(1, "", "CODE")));
        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void overlappingRangesRemainIndependentAfterSaveAndReload() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 16, new MemoryAnnotations());
        AnnotationIndex annotations = new AnnotationIndex(image);
        AnnotationIndex.Entry first = annotations.add(new AnnotationIndex.Entry(-1, 2, 8, "first", "UNKNOWN"));
        AnnotationIndex.Entry second = annotations.add(new AnnotationIndex.Entry(-1, 2, 5, "second", "UNKNOWN"));
        Properties saved = annotations.properties();
        AnnotationIndex restored = new AnnotationIndex(emptyLike(image));

        assertEquals(ProgramIndex.CommitResult.PUBLISHED,
                restored.installProperties(saved, restored.programIndex().snapshot()));
        restored.update(new AnnotationIndex.Entry(first.id, 3, 9, "changed", "UNKNOWN"));
        restored.remove(second);

        assertEquals(1, restored.entries().size());
        assertEquals(first.id, restored.entries().get(0).id);
        assertEquals(3, restored.entries().get(0).address);
        assertEquals(9, restored.entries().get(0).end);
        assertEquals("changed", restored.entries().get(0).label);
    }

    @Test
    public void rangesOutsideAddressSpaceAreRejectedWithoutChanges() {
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 8, new MemoryAnnotations()), engine);

        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new AnnotationIndex.Entry(-1, 3, 9, "outside", "UNKNOWN")));

        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void rangesCanCoverAnyContiguousCpuVisibleCells() {
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex annotations = new AnnotationIndex(ProgramIndex.create("test", 8, new MemoryAnnotations()), engine);

        AnnotationIndex.Entry entry = annotations.add(new AnnotationIndex.Entry(-1, 3, 5, "shared", "UNKNOWN"));

        assertTrue(engine.at(4).stream().anyMatch(annotation -> annotation.ownerId == AnnotationIndex.OWNER
                && annotation.entryId == entry.id));
    }

    @Test
    public void importingVersionTwoAllocatesIndependentIdentities() throws Exception {
        ProgramIndex image = ProgramIndex.create("test", 8, new MemoryAnnotations());
        AnnotationIndex annotations = new AnnotationIndex(image);
        AnnotationIndex.Entry original = annotations.add(new AnnotationIndex.Entry(-1, 1, 4, "original", "UNKNOWN"));
        Properties imported = annotations.properties();
        imported.setProperty("entry.0.label", "imported");

        annotations.importProperties(imported);

        assertEquals(2, annotations.entries().size());
        assertEquals("original", annotations.entries().stream().filter(entry -> entry.id == original.id).findFirst().get().label);
        assertTrue(annotations.entries().stream().anyMatch(entry -> entry.id != original.id && entry.label.equals("imported")));
    }
}
