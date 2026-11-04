/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.DebuggerMemoryImpl;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.CPU;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class DebuggerMemoryImplTest {
    @Test
    public void clearingSourceMappingsPreservesUserAnnotations() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[1][32], 32, false);
        DebuggerMemoryImpl view = new DebuggerMemoryImpl();
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.annotations().put(new AnnotationIndex.Entry(4, "user", "DATA"));
            ProgramIndex index = session.programIndex();
            try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
                update.putAnnotation(2, 1, new ProgramIndex.Range(5, 6), ProgramIndex.Kinds.LABEL, "source");
                assertEquals(ProgramIndex.CommitResult.PUBLISHED, update.commit());
            }
            view.setSession(session);

            session.clearSourceMappings();

            assertEquals("", view.label(5));
            assertEquals("user", view.label(4));
        }
    }

    @Test
    public void metadataUsesCpuVisibleAddresses() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[2][32], 16, false);
        DebuggerMemoryImpl view = new DebuggerMemoryImpl();
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            session.annotations().put(new AnnotationIndex.Entry(4, "one", "DATA"));
            session.annotations().put(new AnnotationIndex.Entry(20, "shared", "DATA"));
            view.setSession(session);

            assertEquals("one", view.label(4));
            assertEquals("shared", view.label(20));
            assertEquals("", view.label(5));
        }
    }

    @Test
    public void currentInstructionMatchesCpuVisibleAddress() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[2][32], 16, false);
        when(machine.cpu.getInstructionLocation()).thenReturn(4, 4, 20);
        DebuggerMemoryImpl view = new DebuggerMemoryImpl();
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            view.setSession(session);

            assertTrue(view.isCurrentInstruction(4));
            assertFalse(view.isCurrentInstruction(5));
            assertTrue(view.isCurrentInstruction(20));
        }
    }

    @Test
    public void listenersObserveChangesAndDetachFromOldSession() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[1][32], 32, false);
        DebuggerMemoryImpl view = new DebuggerMemoryImpl();
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = changes::incrementAndGet;
        view.addListener(listener);
        try (DebugSession session = new DebugSession(machine.cpu, machine.memory, null)) {
            view.setSession(session);
            changes.set(0);

            session.annotations().put(new AnnotationIndex.Entry(4, "entry", "DATA"));
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(2, changes.get());

            view.setSession(null);
            changes.set(0);
            session.annotations().remove(4);
            machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            assertEquals(0, changes.get());
            assertEquals("", view.label(4));
            assertFalse(view.isCurrentInstruction(4));
        }
        view.removeListener(listener);
    }
}
