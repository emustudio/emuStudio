/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.emulib.plugins.cpu.CPUSnapshot;

import static net.emustudio.application.TestCpu.mockCpu;

import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CompileActionTest {
    private VirtualComputer computer;
    private Dialogs dialogs;
    private Editor editor;
    private Compiler compiler;
    private JTextArea compilerOutput;

    @Before
    public void setUp() {
        computer = mock(VirtualComputer.class);
        dialogs = mock(Dialogs.class);
        editor = mock(Editor.class);
        compiler = mock(Compiler.class);
        compilerOutput = new JTextArea();
        EmulationController execution = mock(EmulationController.class);
        when(execution.snapshot()).thenReturn(new CPUSnapshot(
                CPU.RunState.STATE_STOPPED_BREAK, false, false, 0, Float.NaN));
        when(computer.getExecution()).thenReturn(Optional.of(execution));
        when(execution.compile(any())).thenAnswer(call -> {
            CompletableFuture<CPU.RunState> result = new CompletableFuture<>();
            try {
                ((Runnable) call.getArgument(0)).run();
                computer.getCPU().ifPresent(CPU::reset);
                result.complete(CPU.RunState.STATE_STOPPED_BREAK);
            } catch (RuntimeException failure) { result.completeExceptionally(failure); }
            return result;
        });
    }

    @Test
    public void actionIsDisabledAndShowsErrorWhenCompilerIsMissing() {
        when(computer.getCompiler()).thenReturn(Optional.empty());

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {}
        );
        action.actionPerformed(null);

        assertFalse(action.isEnabled());
        verify(dialogs).showError("Compiler is not set", "Compile");
    }

    @Test
    public void constructorRegistersCompilerListenerThatAppendsMessagesAndMovesEditorCaret() {
        SourceCodePosition position = SourceCodePosition.of(1, 2, "program.asm");
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        ArgumentCaptor<CompilerListener> listenerCaptor = ArgumentCaptor.forClass(CompilerListener.class);

        new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {}
        );
        verify(compiler).addCompilerListener(listenerCaptor.capture());

        CompilerListener listener = listenerCaptor.getValue();
        listener.onStart();
        listener.onMessage(new CompilerMessage(CompilerMessage.Type.WARNING, "Warn", position));
        listener.onFinish();
        flushEdt();

        assertTrue(compilerOutput.getText().contains("Compiling started..."));
        assertTrue(compilerOutput.getText().contains("Warn"));
        assertTrue(compilerOutput.getText().contains("Compiling has finished."));
        verify(editor).setPosition(position);
        verifyNoMoreInteractions(editor);
    }

    @Test
    public void actionShowsErrorWhenEmulationIsRunning() {
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_RUNNING, compilerOutput, () -> {}
        );
        action.actionPerformed(null);

        verify(dialogs).showError("Emulation must be stopped first.", "Compile");
    }

    @Test
    public void actionDoesNotCompileWhenSaveFails() {
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(editor.saveFile()).thenReturn(false);
        AtomicInteger titleUpdates = new AtomicInteger();

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput,
                titleUpdates::incrementAndGet
        );
        action.actionPerformed(null);

        verify(editor).saveFile();
        assertEquals(0, titleUpdates.get());
    }

    @Test
    public void actionCompilesCurrentFileAndResetsCpuWithoutResettingMemory() throws Exception {
        Memory memory = mock(Memory.class);
        CPU cpu = mockCpu();
        File file = new File("program.asm");
        AtomicInteger titleUpdates = new AtomicInteger();
        compilerOutput.setText("old");

        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(computer.getMemory()).thenReturn(Optional.of(memory));
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(editor.saveFile()).thenReturn(true);
        when(editor.getCurrentFile()).thenReturn(Optional.of(file));

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput,
                titleUpdates::incrementAndGet
        );
        action.actionPerformed(null);

        assertEquals("", compilerOutput.getText());
        assertEquals(1, titleUpdates.get());
        verify(memory, never()).reset();
        verify(compiler).compile(Path.of("program.asm"), Optional.empty());
        verify(cpu).reset();
    }

    @Test
    public void failedCompilationDoesNotResetMemoryOrCpu() {
        Memory memory = mock(Memory.class);
        CPU cpu = mockCpu();
        List<CompilerListener> listeners = new ArrayList<>();
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(computer.getMemory()).thenReturn(Optional.of(memory));
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getDebugger()).thenReturn(Optional.empty());
        when(editor.saveFile()).thenReturn(true);
        when(editor.getCurrentFile()).thenReturn(Optional.of(new File("program.asm")));
        doAnswer(call -> { listeners.add(call.getArgument(0)); return null; }).when(compiler).addCompilerListener(any());
        doAnswer(call -> {
            CompilerMessage error = new CompilerMessage(CompilerMessage.Type.ERROR, "invalid", null);
            List.copyOf(listeners).forEach(listener -> listener.onMessage(error));
            return null;
        }).when(compiler).compile(any(), any());
        CompileAction action = new CompileAction(computer, dialogs, editor,
                () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {});

        action.actionPerformed(null);

        verify(compiler).compile(any(), any());
        verify(memory, never()).reset();
        verify(cpu, never()).reset();
    }

    @Test
    public void actionAppendsCompilationFailureToOutput() throws Exception {
        File file = new File("program.asm");

        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getCPU()).thenReturn(Optional.empty());
        when(editor.saveFile()).thenReturn(true);
        when(editor.getCurrentFile()).thenReturn(Optional.of(file));
        doThrow(new RuntimeException("compile failed")).when(compiler).compile(Path.of("program.asm"), Optional.empty());

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {}
        );
        action.actionPerformed(null);
        flushEdt();

        assertTrue(compilerOutput.getText().contains("Could not compile file:"));
        assertTrue(compilerOutput.getText().contains("compile failed"));
    }

    private static void flushEdt() {
        try { SwingUtilities.invokeAndWait(() -> {}); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }

}
