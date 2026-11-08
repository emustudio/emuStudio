/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DebugScriptTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void emptyLinesAndIndentedCommentsDoNotControlTheCpu() throws Exception {
        Path script = script("\n  # Inspect memory after loading program\n  \t\n# End\n");
        CPU cpu = cpuWithoutDebugger();

        try (DebugSession session = new DebugSession(cpu, null, null)) {
            DebugScript.run(session, script);

            verify(cpu, never()).step();
            verify(cpu, never()).execute();
        }
    }

    @Test
    public void unknownOperationReportsItsPhysicalLineAndStopsScript() throws Exception {
        Path script = script("# Setup\n\nFLY\nSTEP\n");
        CPU cpu = cpuWithoutDebugger();

        try (DebugSession session = new DebugSession(cpu, null, null)) {
            IOException failure = assertThrows(IOException.class, () -> DebugScript.run(session, script));

            assertEquals("Script line 3: Unknown operation: FLY", failure.getMessage());
            assertTrue(failure.getCause() instanceof IllegalArgumentException);
            verify(cpu, never()).step();
        }
    }

    @Test
    public void stepWithoutCountReportsArgumentFailureBeforeStepping() throws Exception {
        Path script = script("STEP\n");
        CPU cpu = cpuWithoutDebugger();

        try (DebugSession session = new DebugSession(cpu, null, null)) {
            IOException failure = assertThrows(IOException.class, () -> DebugScript.run(session, script));

            assertTrue(failure.getMessage().startsWith("Script line 1:"));
            assertTrue(failure.getCause() instanceof NumberFormatException);
            verify(cpu, never()).step();
        }
    }

    @Test
    public void runWithoutBoundsReportsExpectedArgumentsBeforeStepping() throws Exception {
        Path script = script("run\n");
        CPU cpu = cpuWithoutDebugger();

        try (DebugSession session = new DebugSession(cpu, null, null)) {
            IOException failure = assertThrows(IOException.class, () -> DebugScript.run(session, script));

            assertEquals("Script line 1: RUN address max-instructions", failure.getMessage());
            verify(cpu, never()).step();
        }
    }

    @Test
    public void runningCpuIsRejectedBeforeOpeningScriptFile() throws Exception {
        CPU cpu = cpuWithoutDebugger();
        doAnswer(call -> {
            try { new java.util.concurrent.CountDownLatch(1).await(); }
            catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
            return CPU.RunState.STATE_RUNNING;
        }).when(cpu).execute();
        Path missingScript = temporary.getRoot().toPath().resolve("missing.debug");

        try (DebugSession session = new DebugSession(cpu, null, null)) {
            session.execution().start().join();

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> DebugScript.run(session, missingScript));

            assertEquals("Pause CPU first", failure.getMessage());
            verify(cpu, never()).step();
        }
    }

    @Test
    public void closedSessionIsRejectedBeforeOpeningScriptFile() throws Exception {
        CPU cpu = cpuWithoutDebugger();
        DebugSession session = new DebugSession(cpu, null, null);
        session.close();
        Path missingScript = temporary.getRoot().toPath().resolve("missing.debug");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DebugScript.run(session, missingScript));

        assertEquals("Debugger session closed", failure.getMessage());
        verify(cpu, never()).step();
    }

    @Test
    public void falseAssertionReportsItsLineAndDoesNotExecuteAnotherStep() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[4]}, 4);
        Path script = script("# Verify plugin-owned state\nASSERT A == 1\nSTEP 1\n");
        try (DebugSession session = machine.session(temporary.getRoot().toPath().resolve("debug.properties"))) {
            IOException failure = assertThrows(IOException.class, () -> DebugScript.run(session, script));

            assertEquals("Script line 2: Assertion failed: A == 1", failure.getMessage());
            verify(machine.cpu, never()).step();
            verify(machine.cpu, never()).execute();
        }
    }

    @Test
    public void assertionUsesPluginSymbolicComparisonWithoutNumericHostConversion() throws Exception {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {new byte[4]}, 4);
        machine.values.put("A", "ready");
        Path script = script("ASSERT A == \"ready\"\n");
        try (DebugSession session = machine.session(temporary.getRoot().toPath().resolve("debug.properties"))) {
            DebugScript.run(session, script);

            verify(machine.cpu, never()).step();
            verify(machine.cpu, never()).execute();
        }
    }

    private Path script(String contents) throws IOException {
        Path path = temporary.newFile("commands.debug").toPath();
        Files.writeString(path, contents);
        return path;
    }

    private static CPU cpuWithoutDebugger() {
        CPU cpu = mock(CPU.class);
        return cpu;
    }
}
