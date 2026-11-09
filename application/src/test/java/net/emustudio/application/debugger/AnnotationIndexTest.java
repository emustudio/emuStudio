/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AnnotationIndexTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void replacingCompilerAnnotationsPreservesUserMetadata() {
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex annotations = new AnnotationIndex(8, engine);
        annotations.add(new AnnotationIndex.Entry(1, "user", "CODE"));
        engine.put(new MemoryAnnotations.Entry(10, 1, 1, 2, "source", "old", null));

        engine.replaceOwner(10, List.of(new MemoryAnnotations.Entry(10, 2, 2, 3, "source", "new", null)));

        assertEquals("user", annotations.get(1).label);
        assertNull(engine.get(10, 1));
        assertEquals("new", engine.get(10, 2).text);
    }

    @Test
    public void importRetriesConcurrentEditsWithoutLosingLiveValuesOrOtherOwners() throws Exception {
        AnnotationIndex saved = new AnnotationIndex(8, new MemoryAnnotations());
        saved.add(new AnnotationIndex.Entry(1, "saved", "DATA"));
        saved.add(new AnnotationIndex.Entry(2, "retained", "CODE"));
        MemoryAnnotations engine = spy(new MemoryAnnotations());
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.getAndSet(false)) {
                engine.put(new MemoryAnnotations.Entry(AnnotationIndex.OWNER, 1L << 40, 3, 4,
                        AnnotationIndex.USER_KIND + "DATA", "live edit", null));
                engine.put(new MemoryAnnotations.Entry(42, 1, 0, 1, "custom", "changed", null));
            }
            return call.callRealMethod();
        }).when(engine).compareAndReplaceOwner(anyLong(), eq(AnnotationIndex.OWNER), anyCollection());
        AnnotationIndex live = new AnnotationIndex(8, engine);

        assertEquals(0, live.importProperties(saved.properties()));

        assertEquals("saved", live.get(1).label);
        assertEquals("retained", live.get(2).label);
        assertEquals("live edit", live.get(3).label);
        assertEquals("changed", engine.get(42, 1).text);
    }

    @Test
    public void malformedImportEntriesAreSkippedAndCounted() {
        int size = 2;
        AnnotationIndex live = new AnnotationIndex(size, new MemoryAnnotations());
        live.add(new AnnotationIndex.Entry(0, "keep", "CODE"));
        AnnotationIndex saved = new AnnotationIndex(size, new MemoryAnnotations());
        saved.add(new AnnotationIndex.Entry(1, "saved", "DATA"));
        Properties input = saved.properties();
        input.setProperty("entry.1.address", "invalid");
        input.setProperty("entry.2.address", "0");
        input.setProperty("entry.2.end", "1");
        input.setProperty("entry.2.kind", "SOURCE");

        assertEquals(2, live.importProperties(input));

        assertEquals(2, live.entries().size());
        assertEquals("keep", live.get(0).label);
        assertEquals("saved", live.get(1).label);
    }

    @Test
    public void metadataLoadPreservesCompilerMappingsAndOtherOwners() throws Exception {
        int size = 2;
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex live = new AnnotationIndex(size, engine);
        AnnotationIndex captured = new AnnotationIndex(size, new MemoryAnnotations());
        captured.add(new AnnotationIndex.Entry(0, "user", "CODE"));
        engine.put(new MemoryAnnotations.Entry(9, 8, 1, 2, "custom:note", "keep", null));

        live.importProperties(captured.properties());

        assertEquals("user", live.get(0).label);
        assertEquals("keep", engine.get(9, 8).text);
    }

    @Test
    public void importIgnoresLegacyCountAndVersionKeys() {
        int size = 4;
        AnnotationIndex annotations = new AnnotationIndex(size, new MemoryAnnotations());
        Properties input = new Properties();
        input.setProperty("version", "1");
        input.setProperty("entries", "1");
        for (int i : new int[] {0, 7}) {
            input.setProperty("entry." + i + ".address", Integer.toString(i == 0 ? 1 : 2));
            input.setProperty("entry." + i + ".end", Integer.toString(i == 0 ? 2 : 3));
            input.setProperty("entry." + i + ".label", "value" + i);
            input.setProperty("entry." + i + ".kind", "DATA");
        }

        assertEquals(0, annotations.importProperties(input));

        assertEquals("value0", annotations.get(1).label);
        assertEquals("value7", annotations.get(2).label);
    }

    @Test
    public void repeatedImportDoesNotDuplicateAnnotations() {
        AnnotationIndex annotations = new AnnotationIndex(4, new MemoryAnnotations());
        annotations.add(new AnnotationIndex.Entry(-1, 1, 3, "buffer", "DATA"));
        Properties saved = annotations.properties();

        annotations.importProperties(saved);
        annotations.importProperties(saved);

        assertEquals(1, annotations.entries().size());
    }

    @Test
    public void savedAnnotationsContainOnlyRangesLabelsAndKinds() {
        AnnotationIndex annotations = new AnnotationIndex(4, new MemoryAnnotations());
        annotations.add(new AnnotationIndex.Entry(-1, 1, 3, "buffer", "DATA"));
        Properties expected = new Properties();
        expected.setProperty("entry.0.address", "1");
        expected.setProperty("entry.0.end", "3");
        expected.setProperty("entry.0.label", "buffer");
        expected.setProperty("entry.0.kind", "DATA");

        assertEquals(expected, annotations.properties());
    }

    @Test
    public void clearingUserMetadataPreservesCompilerAndOtherOwners() {
        int size = 2;
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex annotations = new AnnotationIndex(size, engine);
        annotations.add(new AnnotationIndex.Entry(0, "user", "DATA"));
        engine.put(new MemoryAnnotations.Entry(2, 1, 0, 1, "label", "compiler", null));
        engine.put(new MemoryAnnotations.Entry(99, 1, 0, 2, "custom:region", "external", null));

        annotations.replace(List.of());

        assertNull(annotations.get(0));
        assertTrue(annotations.entries().isEmpty());
        assertEquals("compiler", engine.get(2, 1).text);
        assertEquals("external", engine.get(99, 1).text);
    }

    @Test
    public void annotationIsFoundAndRemovedAtItsCpuVisibleAddress() {
        AnnotationIndex annotations = new AnnotationIndex(2, new MemoryAnnotations());
        annotations.add(new AnnotationIndex.Entry(1, "shared", "DATA"));

        AnnotationIndex.Entry shared = annotations.get(1);
        assertEquals("shared", shared.label);
        assertEquals(shared.id, annotations.get(1).id);

        annotations.remove(shared);
        assertNull(annotations.get(1));
    }

    @Test
    public void updatingIdentityReplacesOnlyThatAnnotation() {
        AnnotationIndex annotations = new AnnotationIndex(2, new MemoryAnnotations());
        AnnotationIndex.Entry first = annotations.add(new AnnotationIndex.Entry(0, "first", "CODE"));
        AnnotationIndex.Entry second = annotations.add(new AnnotationIndex.Entry(0, "second", "DATA"));

        annotations.update(new AnnotationIndex.Entry(first.id, 1, 2, "updated", "UNKNOWN"));

        assertEquals("updated", annotations.get(1).label);
        assertEquals(first.id, annotations.get(1).id);
        assertEquals(second.id, annotations.get(0).id);
        assertEquals("second", annotations.get(0).label);
        assertEquals(2, annotations.entries().size());
    }

    @Test
    public void searchMatchesLabelsWithoutCaseSensitivity() {
        AnnotationIndex annotations = new AnnotationIndex(3, new MemoryAnnotations());
        annotations.add(new AnnotationIndex.Entry(0, "MainLoop", "CODE"));
        annotations.add(new AnnotationIndex.Entry(1, "MainLoop buffer", "DATA"));
        annotations.add(new AnnotationIndex.Entry(2, "other", "UNKNOWN"));

        List<AnnotationIndex.Entry> matches = annotations.search("mainloop");

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).address);
        assertEquals(1, matches.get(1).address);
    }

    @Test
    public void savedAnnotationsRestoreEveryUserField() throws Exception {
        int size = 2;
        AnnotationIndex original = new AnnotationIndex(size, new MemoryAnnotations());
        original.add(new AnnotationIndex.Entry(1, "Entrée\nsecond line", "CODE"));
        Path file = temporary.newFile("annotations.properties").toPath();

        original.save(file);
        assertFalse(original.properties().containsKey("entry.0.bookmark"));
        AnnotationIndex restored = new AnnotationIndex(size, new MemoryAnnotations());
        restored.load(file);

        AnnotationIndex.Entry entry = restored.get(1);
        assertEquals(1, entry.address);
        assertEquals("Entrée\nsecond line", entry.label);
        assertEquals("CODE", entry.kind);
    }

    @Test
    public void importKeepsIndependentAnnotationsAtTheSameAddress() throws Exception {
        int size = 2;
        AnnotationIndex annotations = new AnnotationIndex(size, new MemoryAnnotations());
        annotations.add(new AnnotationIndex.Entry(0, "old", "CODE"));
        annotations.add(new AnnotationIndex.Entry(1, "keep", "DATA"));
        AnnotationIndex imported = new AnnotationIndex(size, new MemoryAnnotations());
        imported.add(new AnnotationIndex.Entry(0, "replacement", "UNKNOWN"));

        annotations.importProperties(imported.properties());

        assertEquals(3, annotations.entries().size());
        assertTrue(annotations.entries().stream().anyMatch(entry -> entry.label.equals("replacement")));
        assertTrue(annotations.entries().stream().anyMatch(entry -> entry.label.equals("old")));
        assertEquals("keep", annotations.get(1).label);
    }

    @Test
    public void invalidKindAndOutOfRangeAddressLeaveIndexEmpty() {
        AnnotationIndex annotations = new AnnotationIndex(1, new MemoryAnnotations());

        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new AnnotationIndex.Entry(0, "", "FUNCTION")));
        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new AnnotationIndex.Entry(1, "", "CODE")));
        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void overlappingRangesRemainIndependentAfterSaveAndReload() throws Exception {
        int size = 16;
        AnnotationIndex annotations = new AnnotationIndex(size, new MemoryAnnotations());
        annotations.add(new AnnotationIndex.Entry(-1, 2, 8, "first", "UNKNOWN"));
        annotations.add(new AnnotationIndex.Entry(-1, 2, 5, "second", "UNKNOWN"));
        Properties saved = annotations.properties();
        AnnotationIndex restored = new AnnotationIndex(size, new MemoryAnnotations());

        restored.importProperties(saved);
        AnnotationIndex.Entry first = restored.entries().stream().filter(entry -> entry.label.equals("first")).findFirst().orElseThrow();
        AnnotationIndex.Entry second = restored.entries().stream().filter(entry -> entry.label.equals("second")).findFirst().orElseThrow();
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
        AnnotationIndex annotations = new AnnotationIndex(8, engine);

        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new AnnotationIndex.Entry(-1, 3, 9, "outside", "UNKNOWN")));

        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void rangesCanCoverAnyContiguousCpuVisibleCells() {
        MemoryAnnotations engine = new MemoryAnnotations();
        AnnotationIndex annotations = new AnnotationIndex(8, engine);

        AnnotationIndex.Entry entry = annotations.add(new AnnotationIndex.Entry(-1, 3, 5, "shared", "UNKNOWN"));

        assertTrue(engine.at(4).stream().anyMatch(annotation -> annotation.ownerId == AnnotationIndex.OWNER
                && annotation.entryId == entry.id));
    }

    @Test
    public void importingAllocatesIndependentIdentities() throws Exception {
        int size = 8;
        AnnotationIndex annotations = new AnnotationIndex(size, new MemoryAnnotations());
        AnnotationIndex.Entry original = annotations.add(new AnnotationIndex.Entry(-1, 1, 4, "original", "UNKNOWN"));
        Properties imported = annotations.properties();
        imported.setProperty("entry.0.label", "imported");

        annotations.importProperties(imported);

        assertEquals(2, annotations.entries().size());
        assertEquals("original", annotations.entries().stream().filter(entry -> entry.id == original.id).findFirst().get().label);
        assertTrue(annotations.entries().stream().anyMatch(entry -> entry.id != original.id && entry.label.equals("imported")));
    }
}
