/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp;

import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MemoryContextImplTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private MemoryContextImpl context;
    private MemoryContextAnnotations annotations;

    @Before
    public void setUp() {
        annotations = createNiceMock(MemoryContextAnnotations.class);
        replay(annotations);
        context = new MemoryContextImpl(annotations);
    }

    @After
    public void tearDown() {
        context.destroy();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullAnnotations() {
        new MemoryContextImpl(null);
    }

    @Test
    public void testAnnotations() {
        assertSame(annotations, context.annotations());
    }

    @Test
    public void testGetCellTypeClass() {
        assertEquals(Integer.class, context.getCellTypeClass());
    }

    // === Read/Write tests ===

    @Test
    public void testReadDefaultsToZero() {
        assertEquals(Integer.valueOf(0), context.read(0));
        assertEquals(Integer.valueOf(0), context.read(100));
    }

    @Test
    public void testWriteAndRead() {
        context.write(10, 42);
        assertEquals(Integer.valueOf(42), context.read(10));
    }

    @Test
    public void testWriteMultipleAddresses() {
        context.write(0, 100);
        context.write(1, 200);
        context.write(2, 300);

        assertEquals(Integer.valueOf(100), context.read(0));
        assertEquals(Integer.valueOf(200), context.read(1));
        assertEquals(Integer.valueOf(300), context.read(2));
    }

    @Test
    public void testWriteOverwrite() {
        context.write(5, 10);
        context.write(5, 20);
        assertEquals(Integer.valueOf(20), context.read(5));
    }

    @Test
    public void testReadArray() {
        context.write(0, 10);
        context.write(1, 20);
        context.write(2, 30);

        Integer[] result = context.read(0, 3);
        assertArrayEquals(new Integer[]{10, 20, 30}, result);
    }

    @Test
    public void testReadArrayWithGaps() {
        context.write(0, 10);
        context.write(2, 30);

        Integer[] result = context.read(0, 3);
        assertArrayEquals(new Integer[]{10, 0, 30}, result);
    }

    @Test
    public void testWriteArray() {
        Integer[] values = {100, 200, 300};
        context.write(5, values, 3);

        assertEquals(Integer.valueOf(100), context.read(5));
        assertEquals(Integer.valueOf(200), context.read(6));
        assertEquals(Integer.valueOf(300), context.read(7));
    }

    @Test
    public void testWriteArrayPartial() {
        Integer[] values = {100, 200, 300};
        context.write(0, values, 2);

        assertEquals(Integer.valueOf(100), context.read(0));
        assertEquals(Integer.valueOf(200), context.read(1));
        assertEquals(Integer.valueOf(0), context.read(2));
    }

    // === Size tests ===

    @Test
    public void testGetSizeEmpty() {
        assertEquals(0, context.getSize());
    }

    @Test
    public void testGetSizeAfterWrite() {
        context.write(10, 42);
        assertEquals(10, context.getSize());
    }

    @Test
    public void testGetSizeReturnsMaxAddress() {
        context.write(5, 1);
        context.write(20, 2);
        context.write(10, 3);
        assertEquals(20, context.getSize());
    }

    // === Clear tests ===

    @Test
    public void testClear() {
        context.write(0, 100);
        context.write(10, 200);
        context.clear();

        assertEquals(Integer.valueOf(0), context.read(0));
        assertEquals(Integer.valueOf(0), context.read(10));
        assertEquals(0, context.getSize());
    }

    @Test
    public void testClearAlsoClearsLabels() {
        List<RaspLabel> labels = List.of(createLabel(0, "START"));
        context.setLabels(labels);
        context.clear();

        assertFalse(context.getLabel(0).isPresent());
    }

    @Test
    public void testClearAlsoClearsInputs() {
        context.setInputs(List.of(1, 2, 3));
        context.clear();

        RaspMemoryContext.RaspMemory snapshot = context.getSnapshot();
        assertTrue(snapshot.inputs.isEmpty());
    }

    // === Labels tests ===

    @Test
    public void testSetAndGetLabel() {
        List<RaspLabel> labels = List.of(createLabel(5, "LOOP"));
        context.setLabels(labels);

        Optional<RaspLabel> result = context.getLabel(5);
        assertTrue(result.isPresent());
        assertEquals("LOOP", result.get().getLabel());
        assertEquals(5, result.get().getAddress());
    }

    @Test
    public void testGetLabelNotFound() {
        assertFalse(context.getLabel(99).isPresent());
    }

    @Test
    public void testSetLabelsReplacesExisting() {
        context.setLabels(List.of(createLabel(0, "FIRST")));
        context.setLabels(List.of(createLabel(1, "SECOND")));

        assertFalse(context.getLabel(0).isPresent());
        assertTrue(context.getLabel(1).isPresent());
    }

    // === Inputs tests ===

    @Test
    public void testSetInputs() {
        context.setInputs(List.of(10, 20, 30));

        RaspMemoryContext.RaspMemory snapshot = context.getSnapshot();
        assertEquals(List.of(10, 20, 30), snapshot.inputs);
    }

    @Test
    public void testSetInputsReplacesExisting() {
        context.setInputs(List.of(1, 2));
        context.setInputs(List.of(3, 4, 5));

        RaspMemoryContext.RaspMemory snapshot = context.getSnapshot();
        assertEquals(List.of(3, 4, 5), snapshot.inputs);
    }

    // === Snapshot tests ===

    @Test
    public void testGetSnapshot() {
        context.write(0, 100);
        context.write(1, 200);
        context.setLabels(List.of(createLabel(0, "START")));
        context.setInputs(List.of(10, 20));

        RaspMemoryContext.RaspMemory snapshot = context.getSnapshot();
        assertEquals(Integer.valueOf(100), snapshot.programMemory.get(0));
        assertEquals(Integer.valueOf(200), snapshot.programMemory.get(1));
        assertEquals(1, snapshot.labels.size());
        assertEquals("START", snapshot.labels.get(0).getLabel());
        assertEquals(List.of(10, 20), snapshot.inputs);
    }

    @Test
    public void testSnapshotIsImmutable() {
        context.write(0, 100);
        RaspMemoryContext.RaspMemory snapshot = context.getSnapshot();

        // Change original memory
        context.write(0, 999);

        // Snapshot should still have old value
        assertEquals(Integer.valueOf(100), snapshot.programMemory.get(0));
    }

    // === Serialize/Deserialize tests ===

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        context.write(0, 1);
        context.write(1, 15); // JMP opcode
        context.write(2, 100);
        context.setLabels(List.of(createLabel(0, "START"), createLabel(2, "DATA")));
        context.setInputs(List.of(42, 43));

        File file = tmpFolder.newFile("test.rasp");
        RaspMemoryContext.serialize(file.toPath(), 0, context.getSnapshot());

        // Create new context and deserialize
        MemoryContextAnnotations ann2 = createNiceMock(MemoryContextAnnotations.class);
        replay(ann2);
        MemoryContextImpl context2 = new MemoryContextImpl(ann2);

        AtomicInteger programLocation = new AtomicInteger(-1);
        context2.deserialize(file.getAbsolutePath(), programLocation::set);

        assertEquals(0, programLocation.get());
        assertEquals(Integer.valueOf(1), context2.read(0));
        assertEquals(Integer.valueOf(15), context2.read(1));
        assertEquals(Integer.valueOf(100), context2.read(2));

        assertTrue(context2.getLabel(0).isPresent());
        assertEquals("START", context2.getLabel(0).get().getLabel());
        assertTrue(context2.getLabel(2).isPresent());
        assertEquals("DATA", context2.getLabel(2).get().getLabel());

        RaspMemoryContext.RaspMemory snapshot = context2.getSnapshot();
        assertEquals(List.of(42, 43), snapshot.inputs);

        context2.destroy();
    }

    @Test(expected = FileNotFoundException.class)
    public void testDeserializeNonExistentFileThrows() throws Exception {
        context.deserialize("/nonexistent/file.rasp", i -> {});
    }

    // === Destroy tests ===

    @Test
    public void testDestroy() {
        context.write(0, 100);
        context.setLabels(List.of(createLabel(0, "X")));
        context.setInputs(List.of(1));
        context.destroy();

        assertEquals(Integer.valueOf(0), context.read(0));
        assertFalse(context.getLabel(0).isPresent());
        assertTrue(context.getSnapshot().inputs.isEmpty());
    }

    // === Helper methods ===

    private RaspLabel createLabel(int address, String label) {
        return new RaspLabel() {
            @Override
            public int getAddress() {
                return address;
            }

            @Override
            public String getLabel() {
                return label;
            }
        };
    }
}

