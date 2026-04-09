/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp;

import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class MemoryContextImplTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
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
    public void constructorRejectsNullAnnotations() {
        new MemoryContextImpl(null);
    }

    @Test
    public void metadataAndDefaults() {
        assertSame(annotations, context.annotations());
        assertEquals(Integer.class, context.getCellTypeClass());
        assertEquals(0, context.getSize());
        assertEquals(Integer.valueOf(0), context.read(0));
        assertEquals(Integer.valueOf(0), context.read(100));
        assertFalse(context.getLabel(99).isPresent());
    }

    @Test
    public void readWriteAndBulkAccess() {
        context.write(0, 100);
        context.write(1, 200);
        context.write(2, 300);
        context.write(5, 10);
        context.write(5, 20);
        context.write(10, 42);
        assertArrayEquals(new Integer[]{100, 200, 300}, context.read(0, 3));
        assertEquals(Integer.valueOf(20), context.read(5));
        assertEquals(Integer.valueOf(42), context.read(10));
        assertEquals(10, context.getSize());
        context.write(0, new Integer[]{10, 20, 30}, 2);
        assertArrayEquals(new Integer[]{10, 20, 300}, context.read(0, 3));
        context.write(5, new Integer[]{7, 8}, 2);
        assertArrayEquals(new Integer[]{7, 8, 0}, context.read(5, 3));
    }

    @Test
    public void clearResetsMemoryLabelsAndInputs() {
        context.write(0, 100);
        context.write(10, 200);
        context.setLabels(List.of(label(0, "START")));
        context.setInputs(List.of(1, 2, 3));
        context.clear();
        assertEquals(0, context.getSize());
        assertEquals(Integer.valueOf(0), context.read(0));
        assertEquals(Integer.valueOf(0), context.read(10));
        assertFalse(context.getLabel(0).isPresent());
        assertTrue(context.getSnapshot().inputs.isEmpty());
    }

    @Test
    public void labelsInputsAndSnapshotsAreCopied() {
        context.write(0, 100);
        context.setLabels(List.of(label(0, "FIRST")));
        context.setInputs(List.of(1, 2));
        RaspMemoryContext.RaspMemory snapshot = context.getSnapshot();
        context.write(0, 999);
        context.setLabels(List.of(label(1, "SECOND")));
        context.setInputs(List.of(3, 4, 5));
        assertEquals(Integer.valueOf(100), snapshot.programMemory.get(0));
        assertEquals("FIRST", snapshot.labels.get(0).getLabel());
        assertEquals(List.of(1, 2), snapshot.inputs);
        assertFalse(context.getLabel(0).isPresent());
        assertEquals("SECOND", context.getLabel(1).get().getLabel());
        assertEquals(List.of(3, 4, 5), context.getSnapshot().inputs);
    }

    @Test
    public void serializeRoundTripReplacesPreviousState() throws Exception {
        context.write(100, 888);
        context.setLabels(List.of(label(100, "OLD")));
        context.setInputs(List.of(9));
        MemoryContextImpl source = newContext();
        source.write(0, 1);
        source.write(1, 15);
        source.write(2, 100);
        source.setLabels(List.of(label(0, "START"), label(2, "DATA")));
        source.setInputs(List.of(42, 43));
        File file = tmp.newFile("memory.rasp");
        RaspMemoryContext.serialize(file.toPath(), 99, source.getSnapshot());
        source.destroy();
        AtomicInteger programLocation = new AtomicInteger(-1);
        context.deserialize(file.getAbsolutePath(), programLocation::set);
        assertEquals(99, programLocation.get());
        assertArrayEquals(new Integer[]{1, 15, 100}, context.read(0, 3));
        assertEquals("START", context.getLabel(0).get().getLabel());
        assertEquals("DATA", context.getLabel(2).get().getLabel());
        assertFalse(context.getLabel(100).isPresent());
        assertEquals(Integer.valueOf(0), context.read(100));
        assertEquals(List.of(42, 43), context.getSnapshot().inputs);
    }

    @Test(expected = FileNotFoundException.class)
    public void deserializeMissingFileFails() throws Exception {
        context.deserialize("/nonexistent/file.rasp", i -> {
        });
    }

    @Test
    public void instructionMetadataMatchesDisassembler() {
        assertTrue(context.isInstruction(1));
        assertTrue(context.isInstruction(15));
        assertTrue(context.isInstruction(18));
        assertFalse(context.isInstruction(0));
        assertFalse(context.isInstruction(19));
        assertFalse(context.isInstruction(-1));
        assertEquals(Optional.of("READ"), context.disassembleMnemo(1));
        assertEquals(Optional.of("JMP"), context.disassembleMnemo(15));
        assertEquals(Optional.of("HALT"), context.disassembleMnemo(18));
        assertEquals(Optional.empty(), context.disassembleMnemo(0));
        assertEquals(Optional.empty(), context.disassembleMnemo(100));
    }

    private MemoryContextImpl newContext() {
        MemoryContextAnnotations a = createNiceMock(MemoryContextAnnotations.class);
        replay(a);
        return new MemoryContextImpl(a);
    }

    private static RaspLabel label(int address, String name) {
        return new RaspLabel() {
            @Override
            public int getAddress() {
                return address;
            }

            @Override
            public String getLabel() {
                return name;
            }
        };
    }
}
