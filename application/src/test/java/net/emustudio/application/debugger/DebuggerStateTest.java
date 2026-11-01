/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.cpu.*;
import net.emustudio.emulib.plugins.cpu.debug.*;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.*;
import java.util.*;

public class DebuggerStateTest {
    private CPU cpu(DebugProbe probe) {
        CPU cpu = mock(CPU.class);
        when(cpu.getDebugger()).thenReturn(Optional.of(probe));
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(32);
        return cpu;
    }

    @Test
    public void persistentRulesWatchesAndSourceBreakpointsResolveCurrentMappings()
            throws Exception {
        DebugMemory m = new DebugMemory(32, 2, 16);
        DebugProbe probe = new DebugProbe("Z80", m);
        probe.register("PC", 16, () -> 4, v -> {});
        Path file = Files.createTempDirectory("debugger-test").resolve("computer.debugger");
        try (DebugSession session = new DebugSession(cpu(probe), m, file)) {
            session.watches.add("PC");
            DebugSession.Rule r = new DebugSession.Rule("source", "SOURCE", 7, -1, "PC == 4", 1);
            r.source = Path.of("program.asm").toAbsolutePath().normalize().toString();
            r.keepInspection = true;
            session.rules.add(r);
            session.updateProbe();
            session.save();
            assertFalse(session.beforeInstruction(4, 0));
            m.annotations.add(
                    4, new SourceCodeAnnotation(2, SourceCodePosition.of(7, 0, r.source)));
            assertTrue(session.beforeInstruction(4, 0));
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(4, hit.address); // SOURCE rule address is line 7, not address 7.
            assertEquals(0, hit.bank);
            assertTrue(session.keepsInspection(hit));
            assertNull(session.takeBreakpointHit());
            assertFalse(session.beforeInstruction(4, 0));
            m.annotations.clear();
            assertFalse(session.beforeInstruction(4, 0));
        }
        try (DebugSession loaded = new DebugSession(cpu(probe), m, file)) {
            assertEquals(List.of("PC"), loaded.watches);
            assertEquals("source", loaded.rules.get(0).name);
            assertEquals(0, loaded.rules.get(0).hits);
            assertTrue(loaded.rules.get(0).keepInspection);
        }
    }

    @Test
    public void watchpointsUseGuestAccessBankAndHistoryIsBounded() throws Exception {
        DebugMemory m = new DebugMemory(32, 2, 16);
        m.selected = 1;
        DebugProbe probe = new DebugProbe("Z80", m);
        probe.register("PC", 16, () -> 2, v -> {});
        Path file = Files.createTempDirectory("debugger-test").resolve("computer.debugger");
        try (DebugSession s = new DebugSession(cpu(probe), m, file)) {
            s.rules.add(new DebugSession.Rule("write", "WRITE", 3, 1, "", 2));
            s.record(2);
            for (int i = 0; i < 3; i++) {
                assertTrue(probe.begin(2));
                probe.instrumentedMemory().write(3, (byte) i);
                assertEquals(i >= 1, probe.end(true));
            }
            assertEquals(2, s.history().size());
            assertEquals(3, s.rules.get(0).hits);
            DebugSession.BreakpointHit hit = s.takeBreakpointHit();
            assertEquals(3, hit.address);
            assertEquals(1, hit.bank);
            int count = s.history().size();
            probe.memory().read(3);
            probe.memory().write(3, (byte) 12);
            assertEquals(count, s.history().size());
            assertEquals(1, s.history().get(0).accesses.get(0).bank);
            s.record(0);
            assertTrue(s.history().isEmpty());
        }
        assertFalse(probe.isActive());
    }

    @Test
    public void mutationDropsUndeliveredHitButRetainsNewHitGeneratedByStep() throws Exception {
        DebugMemory memory = new DebugMemory(32, 2, 16);
        try (DebugSession session = new DebugSession(cpu(new DebugProbe("Z80", memory)), memory, null)) {
            session.rules.add(new DebugSession.Rule("first", "EXEC", 3, -1, "", 1));
            session.rules.add(new DebugSession.Rule("second", "EXEC", 5, -1, "", 1));
            assertTrue(session.beforeInstruction(3, 0));
            session.runMutation(() -> {}); // Reset/compile before the queued UI inspection.
            assertNull(session.takeBreakpointHit());
            session.runMutation(() -> assertTrue(session.beforeInstruction(5, 1)));
            DebugSession.BreakpointHit fresh = session.takeBreakpointHit();
            assertEquals(5, fresh.address);
            assertEquals(1, fresh.bank);
        }
    }

    @Test
    public void nativeKeepStatePersistsAndRemovalResetsIt() throws Exception {
        DebugMemory memory = new DebugMemory(32, 2, 16);
        DebugProbe probe = new DebugProbe("Z80", memory);
        Path file = Files.createTempDirectory("breakpoint-states").resolve("computer.debugger");
        for (int pass = 0; pass < 2; pass++) {
            CPU cpu = cpu(probe);
            Set<Integer> addresses = new HashSet<>();
            when(cpu.isBreakpointSupported()).thenReturn(true);
            when(cpu.isBreakpointSet(anyInt())).thenAnswer(c -> addresses.contains(c.getArgument(0)));
            when(cpu.getBreakpoints()).thenAnswer(c -> Set.copyOf(addresses));
            doAnswer(c -> addresses.add(c.getArgument(0))).when(cpu).setBreakpoint(anyInt());
            doAnswer(c -> addresses.remove(c.getArgument(0))).when(cpu).unsetBreakpoint(anyInt());
            try (DebugSession session = new DebugSession(cpu, memory, file)) {
                if (pass == 0) {
                    session.setBreakpointState(3, BreakpointState.KEEP_INSPECTION);
                    session.setBreakpointState(5, BreakpointState.ON);
                    session.save();
                }
                assertEquals(BreakpointState.KEEP_INSPECTION, session.breakpointState(3));
                assertEquals(BreakpointState.ON, session.breakpointState(5));
                assertEquals(BreakpointState.OFF, session.breakpointState(7));
                if (pass == 1) {
                    session.setBreakpointState(3, BreakpointState.OFF);
                    session.setBreakpointState(3, BreakpointState.ON);
                    assertEquals(BreakpointState.ON, session.breakpointState(3));
                }
            }
        }
    }

    @Test
    public void portableHitWinsOverNativePcAndNewRunDiscardsPendingHit() throws Exception {
        DebugMemory memory = new DebugMemory(32, 2, 16);
        CPU cpu = cpu(new DebugProbe("Z80", memory));
        when(cpu.isBreakpointSupported()).thenReturn(true);
        when(cpu.getInstructionLocation()).thenReturn(6);
        when(cpu.isBreakpointSet(6)).thenReturn(true);
        try (DebugSession session = new DebugSession(cpu, memory, null)) {
            ArgumentCaptor<CPU.CPUListener> listener = ArgumentCaptor.forClass(CPU.CPUListener.class);
            verify(cpu).addCPUListener(listener.capture());
            CPU.CPUListener events = listener.getValue();
            session.rules.add(new DebugSession.Rule("data", "WRITE", 3, 1, "", 1));
            events.runStateChanged(CPU.RunState.STATE_RUNNING);
            assertTrue(session.afterInstruction(new DebugCPU.Instruction(2, 0, Map.of(), Map.of(),
                    List.of(new DebugCPU.Access(3, 1, 42, true)), true)));
            assertNull(session.takeBreakpointHit()); // Never inspect execution memory while running.
            events.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
            events.internalStateChanged(); // Does not erase a hit before EDT delivery.
            DebugSession.BreakpointHit hit = session.takeBreakpointHit();
            assertEquals(3, hit.address);
            assertEquals(1, hit.bank); // Access bank differs from instruction bank 0.
            assertNull(session.takeBreakpointHit());
            events.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(6, session.takeBreakpointHit().address); // Step/pause at native BP.
            events.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
            events.runStateChanged(CPU.RunState.STATE_RUNNING);
            when(cpu.isBreakpointSet(6)).thenReturn(false);
            events.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
            assertNull(session.takeBreakpointHit());
        }
    }

    @Test
    public void conditionsReadLogicalBankWhenInstructionsExecuteInCommonMemory() throws Exception {
        DebugMemory memory = new DebugMemory(32, 2, 16);
        memory.selected = 1;
        memory.values[1][3] = 42;
        DebugProbe probe = new DebugProbe("Z80", memory);
        try (DebugSession session = new DebugSession(cpu(probe), memory, null)) {
            session.rules.add(new DebugSession.Rule("condition", "EXEC", 20, 0, "mem[3] == 42", 1));
            assertTrue(session.beforeInstruction(20, 0));
            session.rules.clear();
            session.rules.add(new DebugSession.Rule("write", "WRITE", 3, 1, "mem[3] == 42", 1));
            assertTrue(
                    session.afterInstruction(
                            new DebugCPU.Instruction(
                                    20,
                                    0,
                                    Map.of(),
                                    Map.of(),
                                    List.of(new DebugCPU.Access(3, 1, 42, true)),
                                    true)));
        }
    }

    @Test
    public void annotationsRoundTripUnicodeAndRejectAnotherImageAtomically() throws Exception {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {new byte[8], new byte[8]}, 4);
        AnnotationIndex index = new AnnotationIndex(image);
        index.put(new AnnotationIndex.Entry(1, 6, "shared", "český popis", true, "DATA"));
        assertEquals(0, index.entries().get(0).bank);
        assertEquals("shared", index.get(1, 6).label);
        Path file = Files.createTempFile("annotations", ".properties");
        index.save(file);
        AnnotationIndex loaded = new AnnotationIndex(image);
        loaded.load(file);
        assertEquals("český popis", loaded.get(0, 6).comment);
        byte[][] other = {new byte[8], new byte[8]};
        other[1][1] = 1;
        AnnotationIndex wrong = new AnnotationIndex(new MemoryAnalysis(other, 4));
        assertThrows(java.io.IOException.class, () -> wrong.load(file));
        assertTrue(wrong.entries().isEmpty());
        Properties malformed = index.properties();
        malformed.setProperty("entry.0.address", "999");
        assertThrows(java.io.IOException.class, () -> loaded.importProperties(malformed));
        assertEquals(1, loaded.entries().size());
    }

    @Test
    public void flowHandlesLoopsAndIndirectTargets() {
        Disassembler dis = mock(Disassembler.class);
        try {
            when(dis.disassemble(anyInt()))
                    .thenAnswer(a -> new DisassembledInstruction(a.getArgument(0), "test", "00"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        when(dis.getFlow(0))
                .thenReturn(
                        Optional.of(
                                new InstructionFlow(
                                        InstructionFlow.Kind.JUMP, 2, true, List.of(0, 4))));
        when(dis.getFlow(2))
                .thenReturn(
                        Optional.of(
                                new InstructionFlow(
                                        InstructionFlow.Kind.JUMP, 3, false, List.of())));
        when(dis.getFlow(4))
                .thenReturn(
                        Optional.of(
                                new InstructionFlow(
                                        InstructionFlow.Kind.RETURN, 5, false, List.of())));
        FlowAnalysis flow = new FlowAnalysis(dis, 0, 10, null, 0);
        assertEquals(3, flow.instructions.size());
        assertFalse(flow.truncated);
        assertEquals(1, flow.callers(0).size());
    }

    @Test
    public void metadataExportPreservesBankMappingAnnotationsAndImageIdentity() throws Exception {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2, 3}, {4, 5, 3}}, 2);
        AnnotationIndex index = new AnnotationIndex(image);
        index.put(new AnnotationIndex.Entry(1, 1, "counter", "Loop count", true, "DATA"));
        Path directory = Files.createTempDirectory("metadata-export");
        MetadataExport.export(directory, image, index, "Z80", 1, 0);
        assertArrayEquals(
                new byte[] {4, 5, 3}, Files.readAllBytes(directory.resolve("memory-bank-1.bin")));
        Properties p = AtomicProperties.load(directory.resolve("memory-bank-1.properties"));
        assertEquals("1", p.getProperty("bank"));
        assertEquals(image.identity(), p.getProperty("image.identity"));
        AnnotationIndex imported = new AnnotationIndex(image);
        imported.importProperties(p);
        assertEquals("counter", imported.get(1, 1).label);
        assertTrue(imported.get(1, 1).bookmark);
        MetadataExport.export(directory, image, index, "custom-cpu", 1, 65535);
        Properties generic = AtomicProperties.load(directory.resolve("memory-bank-1.properties"));
        assertEquals("custom-cpu", generic.getProperty("processor"));
        assertEquals("65535", generic.getProperty("base"));
        assertThrows(
                IllegalArgumentException.class,
                () -> MetadataExport.export(directory, image, index, "Z80", 1, Integer.MAX_VALUE));
        assertThrows(
                IllegalArgumentException.class,
                () -> MetadataExport.export(directory, image, index, "", 1, 0));
    }

    @Test
    public void scriptRunAssertCaptureAndFailureExitContract() throws Exception {
        DebugMemory m = new DebugMemory(8, 1, 0);
        DebugProbe probe = new DebugProbe("8080", m);
        int[] pc = {0};
        probe.register("PC", 16, () -> pc[0], v -> pc[0] = (int) v);
        CPU cpu = cpu(probe);
        when(cpu.getInstructionLocation()).thenAnswer(a -> pc[0]);
        when(cpu.getAddressSpaceSize()).thenReturn(8);
        List<CPU.CPUListener> listeners = new ArrayList<>();
        doAnswer(
                        a -> {
                            listeners.add(a.getArgument(0));
                            return null;
                        })
                .when(cpu)
                .addCPUListener(any());
        doAnswer(
                        a -> {
                            listeners.remove(a.getArgument(0));
                            return null;
                        })
                .when(cpu)
                .removeCPUListener(any());
        doAnswer(
                        a -> {
                            pc[0]++;
                            for (CPU.CPUListener l : List.copyOf(listeners))
                                l.runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
                            return null;
                        })
                .when(cpu)
                .step();
        Path directory = Files.createTempDirectory("debug-script");
        Path script = directory.resolve("test.debug");
        try (DebugSession session = new DebugSession(cpu, m, directory.resolve("settings"))) {
            Files.writeString(script, "RUN 3 4\nASSERT PC == 3\nSTEP 1\nCAPTURE result.bin\n");
            DebugScript.run(session, script);
            assertEquals(4, pc[0]);
            assertEquals(8, Files.size(directory.resolve("result.bin")));
            Files.writeString(script, "ASSERT PC == 0\n");
            assertThrows(java.io.IOException.class, () -> DebugScript.run(session, script));
            Files.writeString(script, "RUN 7 1\n");
            assertThrows(java.io.IOException.class, () -> DebugScript.run(session, script));
        }
        assertTrue(listeners.isEmpty());
    }
}
