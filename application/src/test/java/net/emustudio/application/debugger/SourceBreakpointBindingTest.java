/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** Exercises source-rule rebinding through the session and its real instruction-boundary listener. */
public class SourceBreakpointBindingTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void preboundManyToManyTargetsSurviveRunInvalidationButObservedWritesClearThem() throws Exception {
        String text = "NOP\nDB 1,2\nHALT\n";
        Path source = source(text);
        try (Fixture fixture = new Fixture(source, text, Map.of(
                1, Set.of(annotation(source, 2, 3)), 3, Set.of(annotation(source, 2, 5))))) {
            DebugSession.Rule rule = fixture.rule(2);
            verify(fixture.machine.debug).addListener(fixture.session);
            ProgramIndex.Snapshot beforeRun = fixture.index.snapshot();
            List<ProgramIndex.Mapping> original = new ArrayList<>();
            beforeRun.forEachMapping(original::add);
            assertEquals(2, original.size());
            assertTrue(fixture.mappings.addressOf(SourceCodePosition.of(2, 0, source.toString())).isEmpty());

            fixture.session.runExecution(() -> fixture.machine.state(CPU.RunState.STATE_RUNNING));

            assertTrue(fixture.index.snapshot().memoryRevision() > beforeRun.memoryRevision());
            assertTrue(fixture.mappings.sourceAt(1, 1).isEmpty());
            assertTrue(fixture.session.beforeInstruction(1, 0));
            assertFalse("Resume skip follows the address across banks", fixture.session.beforeInstruction(1, 1));
            assertFalse(fixture.session.beforeInstruction(2, 1));
            assertTrue(fixture.session.beforeInstruction(3, 1));
            assertEquals(2, rule.hits);

            // An operand-cell write is observable even though it is not a mapped instruction start.
            fixture.cells[1][2] = 42;
            fixture.machine.memoryChanged(2);

            assertFalse(fixture.session.beforeInstruction(1, 1));
            assertFalse(fixture.session.beforeInstruction(3, 1));
            assertEquals(2, rule.hits);
            fixture.machine.state(CPU.RunState.STATE_STOPPED_BREAK);
            fixture.session.updateProbe();
            fixture.session.runExecution(() -> fixture.machine.state(CPU.RunState.STATE_RUNNING));
            assertFalse(fixture.session.beforeInstruction(1, 1));
            assertFalse(fixture.session.beforeInstruction(3, 1));
            assertEquals(2, rule.hits);

            List<ProgramIndex.Mapping> historical = new ArrayList<>();
            fixture.index.snapshot().forEachMapping(historical::add);
            assertEquals(2, historical.size());
            assertEquals(beforeRun.memoryRevision(), historical.get(0).memoryRevision());
            assertEquals(beforeRun.memoryRevision(), original.get(0).memoryRevision());
        }
    }

    @Test public void sourceTargetsMatchAddressesAcrossPrivateBanksAndSharedAliases() throws Exception {
        String text = "NOP\nDB 1,2\nHALT\n";
        Path source = source(text);
        try (Fixture fixture = new Fixture(source, text, Map.of(
                0, Set.of(annotation(source, 1, 0)), 6, Set.of(annotation(source, 3, 0))))) {
            DebugSession.Rule privateRule = fixture.rule(1);
            DebugSession.Rule commonRule = fixture.rule(3);
            verify(fixture.machine.debug).addListener(fixture.session);
            assertEquals(0, fixture.selectedBank.get());
            assertEquals(0, fixture.mappings.sourceAt(6, 1).orElseThrow().bank);

            fixture.session.runExecution(() -> fixture.machine.state(CPU.RunState.STATE_RUNNING));

            assertTrue(fixture.session.beforeInstruction(0, 0));
            assertFalse(fixture.session.beforeInstruction(0, 1));
            assertFalse(fixture.session.beforeInstruction(2, 1));
            assertTrue(fixture.session.beforeInstruction(0, 1));
            assertTrue(fixture.session.beforeInstruction(6, 0));
            assertFalse(fixture.session.beforeInstruction(6, 1));
            assertFalse(fixture.session.beforeInstruction(2, 1));
            assertTrue(fixture.session.beforeInstruction(6, 1));
            assertEquals(2, privateRule.hits);
            assertEquals(2, commonRule.hits);
            assertEquals(0, fixture.selectedBank.get());
        }
    }

    private Path source(String text) throws Exception {
        Path source = temporary.newFile("source.asm").toPath().toAbsolutePath().normalize();
        Files.writeString(source, text);
        return source;
    }

    private static SourceCodeAnnotation annotation(Path source, int line, int column) {
        return new SourceCodeAnnotation(7, SourceCodePosition.of(line, column, source.toString()));
    }

    private static final class Fixture implements AutoCloseable {
        final byte[][] cells = {new byte[8], new byte[8]};
        final DebuggerTestMachine machine = new DebuggerTestMachine(cells, 4, false);
        final AtomicInteger selectedBank = new AtomicInteger(1);
        final DebugSession session;
        final ProgramIndex index;
        final SourceAddressMap mappings;
        final Path source;

        Fixture(Path source, String text, Map<Integer, Set<SourceCodeAnnotation>> metadata) throws Exception {
            this.source = source;
            Annotations annotations = new Annotations();
            metadata.forEach((address, entries) -> entries.forEach(entry -> annotations.add(address, entry)));
            when(machine.memory.annotations()).thenReturn(annotations);
            when(machine.memory.getSelectedBank()).thenAnswer(call -> selectedBank.get());
            when(machine.memory.getBank(anyInt())).thenAnswer(call -> (Integer) call.getArgument(0) < 4 ? selectedBank.get() : 0);
            Compiler compiler = mock(Compiler.class);
            when(compiler.getSourcePositionColumnUnit()).thenReturn(SourceCodePosition.ColumnUnit.CODE_POINT);
            session = new DebugSession(machine.cpu, machine.memory, null);
            index = session.programIndex();
            mappings = SourceAddressMap.capture(compiler, machine.memory, null, Map.of(source, text), index, 1);
            session.setSourceMappings(mappings);
            selectedBank.set(0);
        }

        DebugSession.Rule rule(int line) {
            DebugSession.Rule rule = new DebugSession.Rule("line " + line, "SOURCE", line, "", 1);
            rule.source = source.toString();
            session.rules.add(rule);
            session.updateProbe();
            return rule;
        }

        @Override public void close() { session.close(); }
    }
}
