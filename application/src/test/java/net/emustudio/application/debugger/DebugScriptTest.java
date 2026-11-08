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
            verify(cpu, never()).pause();
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
        AtomicReference<CPU.CPUListener> listener = new AtomicReference<>();
        doAnswer(call -> {
            listener.set(call.getArgument(0));
            return null;
        }).when(cpu).addCPUListener(any());
        doAnswer(call -> {
            listener.get().runStateChanged(CPU.RunState.STATE_STOPPED_BREAK);
            return null;
        }).when(cpu).pause();
        Path missingScript = temporary.getRoot().toPath().resolve("missing.debug");

        try (DebugSession session = new DebugSession(cpu, null, null)) {
            listener.get().runStateChanged(CPU.RunState.STATE_RUNNING);

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

    private Path script(String contents) throws IOException {
        Path path = temporary.newFile("commands.debug").toPath();
        Files.writeString(path, contents);
        return path;
    }

    private static CPU cpuWithoutDebugger() {
        CPU cpu = mock(CPU.class);
        when(cpu.getDebugger()).thenReturn(Optional.empty());
        return cpu;
    }
}
