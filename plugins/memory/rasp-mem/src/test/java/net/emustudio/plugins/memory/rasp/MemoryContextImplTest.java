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

    // === RaspMemoryContext default method tests ===

    @Test
    public void testIsInstructionValidOpcode() {
        assertTrue(context.isInstruction(1));  // READ
        assertTrue(context.isInstruction(15)); // JMP
        assertTrue(context.isInstruction(18)); // HALT
    }

    @Test
    public void testIsInstructionInvalidOpcode() {
        assertFalse(context.isInstruction(0));
        assertFalse(context.isInstruction(19));
        assertFalse(context.isInstruction(-1));
    }

    @Test
    public void testDisassembleMnemoValid() {
        assertEquals(Optional.of("READ"), context.disassembleMnemo(1));
        assertEquals(Optional.of("JMP"), context.disassembleMnemo(15));
        assertEquals(Optional.of("HALT"), context.disassembleMnemo(18));
    }

    @Test
    public void testDisassembleMnemoInvalid() {
        assertEquals(Optional.empty(), context.disassembleMnemo(0));
        assertEquals(Optional.empty(), context.disassembleMnemo(100));
    }

    // === RaspMemory tests ===

    @Test
    public void testRaspMemoryEmptyCollections() {
        RaspMemoryContext.RaspMemory memory = new RaspMemoryContext.RaspMemory(
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyList()
        );
        assertTrue(memory.labels.isEmpty());
        assertTrue(memory.programMemory.isEmpty());
        assertTrue(memory.inputs.isEmpty());
    }

    @Test
    public void testRaspMemoryIsImmutableLabels() {
        RaspMemoryContext.RaspMemory memory = new RaspMemoryContext.RaspMemory(
                List.of(createLabel(0, "X")), Map.of(0, 1), List.of(10)
        );
        try {
            memory.labels.add(createLabel(1, "Y"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testRaspMemoryIsImmutableProgramMemory() {
        RaspMemoryContext.RaspMemory memory = new RaspMemoryContext.RaspMemory(
                Collections.emptyList(), Map.of(0, 1), Collections.emptyList()
        );
        try {
            memory.programMemory.put(1, 2);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testRaspMemoryIsImmutableInputs() {
        RaspMemoryContext.RaspMemory memory = new RaspMemoryContext.RaspMemory(
                Collections.emptyList(), Collections.emptyMap(), List.of(10)
        );
        try {
            memory.inputs.add(20);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    // === Serialize edge cases ===

    @Test
    public void testSerializeEmptyMemory() throws Exception {
        File file = tmpFolder.newFile("empty.rasp");
        RaspMemoryContext.RaspMemory snapshot = context.getSnapshot();
        RaspMemoryContext.serialize(file.toPath(), 0, snapshot);

        // Deserialize and verify
        MemoryContextAnnotations ann2 = createNiceMock(MemoryContextAnnotations.class);
        replay(ann2);
        MemoryContextImpl context2 = new MemoryContextImpl(ann2);
        AtomicInteger programLocation = new AtomicInteger(-1);
        context2.deserialize(file.getAbsolutePath(), programLocation::set);

        assertEquals(0, programLocation.get());
        assertEquals(0, context2.getSize());
        assertTrue(context2.getSnapshot().labels.isEmpty());
        assertTrue(context2.getSnapshot().inputs.isEmpty());

        context2.destroy();
    }

    @Test
    public void testSerializeWithNonZeroProgramLocation() throws Exception {
        context.write(0, 42);
        File file = tmpFolder.newFile("nonzero.rasp");
        RaspMemoryContext.serialize(file.toPath(), 99, context.getSnapshot());

        MemoryContextAnnotations ann2 = createNiceMock(MemoryContextAnnotations.class);
        replay(ann2);
        MemoryContextImpl context2 = new MemoryContextImpl(ann2);
        AtomicInteger programLocation = new AtomicInteger(-1);
        context2.deserialize(file.getAbsolutePath(), programLocation::set);

        assertEquals(99, programLocation.get());
        context2.destroy();
    }

    // === Write array overwriting existing addresses ===

    @Test
    public void testWriteArrayOverwriteExisting() {
        context.write(5, 1);
        context.write(6, 2);

        Integer[] values = {10, 20};
        context.write(5, values, 2);

        assertEquals(Integer.valueOf(10), context.read(5));
        assertEquals(Integer.valueOf(20), context.read(6));
    }

    // === Multiple labels ===

    @Test
    public void testSetMultipleLabels() {
        context.setLabels(List.of(
                createLabel(0, "A"),
                createLabel(5, "B"),
                createLabel(10, "C")
        ));

        assertEquals("A", context.getLabel(0).get().getLabel());
        assertEquals("B", context.getLabel(5).get().getLabel());
        assertEquals("C", context.getLabel(10).get().getLabel());
        assertFalse(context.getLabel(3).isPresent());
    }

    // === Deserialize clears previous state ===

    @Test
    public void testDeserializeClearsPreviousState() throws Exception {
        // Set up initial state
        context.write(0, 999);
        context.write(100, 888);
        context.setLabels(List.of(createLabel(100, "OLD")));
        context.setInputs(List.of(1, 2, 3, 4, 5));

        // Create a new image with different data
        MemoryContextAnnotations ann2 = createNiceMock(MemoryContextAnnotations.class);
        replay(ann2);
        MemoryContextImpl tempContext = new MemoryContextImpl(ann2);
        tempContext.write(0, 42);
        tempContext.setLabels(List.of(createLabel(0, "NEW")));
        tempContext.setInputs(List.of(10));

        File file = tmpFolder.newFile("replace.rasp");
        RaspMemoryContext.serialize(file.toPath(), 3, tempContext.getSnapshot());
        tempContext.destroy();

        // Deserialize into original context
        AtomicInteger programLocation = new AtomicInteger(-1);
        context.deserialize(file.getAbsolutePath(), programLocation::set);

        assertEquals(3, programLocation.get());
        assertEquals(Integer.valueOf(42), context.read(0));
        // Old data at address 100 should be gone
        assertEquals(Integer.valueOf(0), context.read(100));
        assertFalse(context.getLabel(100).isPresent());
        assertTrue(context.getLabel(0).isPresent());
        assertEquals("NEW", context.getLabel(0).get().getLabel());
        assertEquals(List.of(10), context.getSnapshot().inputs);
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
