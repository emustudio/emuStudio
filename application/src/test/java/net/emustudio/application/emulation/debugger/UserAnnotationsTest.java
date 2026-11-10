/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import org.junit.Test;

import java.util.List;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import static org.junit.Assert.*;

public class UserAnnotationsTest {

    @Test
    public void replacingCompilerAnnotationsPreservesUserMetadata() {
        MemoryAnnotations engine = new MemoryAnnotations();
        UserAnnotations annotations = new UserAnnotations(8, engine);
        annotations.add(new UserAnnotations.Entry(1, "user", "CODE"));
        engine.put(new MemoryAnnotations.Entry(10, 1, 1, 2, "source", "old", null));

        engine.replaceOwner(10, List.of(new MemoryAnnotations.Entry(10, 2, 2, 3, "source", "new", null)));

        assertEquals("user", annotations.get(1).label);
        assertNull(engine.get(10, 1));
        assertEquals("new", engine.get(10, 2).text);
    }

    @Test
    public void clearingUserMetadataPreservesCompilerAndOtherOwners() {
        int size = 2;
        MemoryAnnotations engine = new MemoryAnnotations();
        UserAnnotations annotations = new UserAnnotations(size, engine);
        annotations.add(new UserAnnotations.Entry(0, "user", "DATA"));
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
        UserAnnotations annotations = new UserAnnotations(2, new MemoryAnnotations());
        annotations.add(new UserAnnotations.Entry(1, "shared", "DATA"));

        UserAnnotations.Entry shared = annotations.get(1);
        assertEquals("shared", shared.label);
        assertEquals(shared.id, annotations.get(1).id);

        annotations.remove(shared);
        assertNull(annotations.get(1));
    }

    @Test
    public void updatingIdentityReplacesOnlyThatAnnotation() {
        UserAnnotations annotations = new UserAnnotations(2, new MemoryAnnotations());
        UserAnnotations.Entry first = annotations.add(new UserAnnotations.Entry(0, "first", "CODE"));
        UserAnnotations.Entry second = annotations.add(new UserAnnotations.Entry(0, "second", "DATA"));

        annotations.update(new UserAnnotations.Entry(first.id, 1, 2, "updated", "CODE"));

        assertEquals("updated", annotations.get(1).label);
        assertEquals(first.id, annotations.get(1).id);
        assertEquals(second.id, annotations.get(0).id);
        assertEquals("second", annotations.get(0).label);
        assertEquals(2, annotations.entries().size());
    }

    @Test
    public void searchMatchesLabelsWithoutCaseSensitivity() {
        UserAnnotations annotations = new UserAnnotations(3, new MemoryAnnotations());
        annotations.add(new UserAnnotations.Entry(0, "MainLoop", "CODE"));
        annotations.add(new UserAnnotations.Entry(1, "MainLoop buffer", "DATA"));
        annotations.add(new UserAnnotations.Entry(2, "other", "CODE"));

        List<UserAnnotations.Entry> matches = annotations.search("mainloop");

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).address);
        assertEquals(1, matches.get(1).address);
    }

    @Test
    public void invalidKindAndOutOfRangeAddressLeaveIndexEmpty() {
        UserAnnotations annotations = new UserAnnotations(1, new MemoryAnnotations());

        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new UserAnnotations.Entry(0, "", "FUNCTION")));
        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new UserAnnotations.Entry(0, "", "UNKNOWN")));
        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new UserAnnotations.Entry(1, "", "CODE")));
        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void rangesOutsideAddressSpaceAreRejectedWithoutChanges() {
        MemoryAnnotations engine = new MemoryAnnotations();
        UserAnnotations annotations = new UserAnnotations(8, engine);

        assertThrows(IllegalArgumentException.class,
                () -> annotations.add(new UserAnnotations.Entry(-1, 3, 9, "outside", "CODE")));

        assertTrue(annotations.entries().isEmpty());
    }

    @Test
    public void rangesCanCoverAnyContiguousCpuVisibleCells() {
        MemoryAnnotations engine = new MemoryAnnotations();
        UserAnnotations annotations = new UserAnnotations(8, engine);

        UserAnnotations.Entry entry = annotations.add(new UserAnnotations.Entry(-1, 3, 5, "shared", "CODE"));

        assertTrue(engine.at(4).stream().anyMatch(annotation -> annotation.ownerId == UserAnnotations.OWNER
                && annotation.entryId == entry.id));
        assertEquals("CODE", engine.get(UserAnnotations.OWNER, entry.id).kind);
    }

}
