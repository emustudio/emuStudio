/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CompileActionTest {

    @Test
    public void actionIsDisabledAndShowsErrorWhenCompilerIsMissing() {
        VirtualComputer computer = mock(VirtualComputer.class);
        Dialogs dialogs = mock(Dialogs.class);
        Editor editor = mock(Editor.class);
        JTextArea compilerOutput = new JTextArea();

        when(computer.getCompiler()).thenReturn(Optional.empty());

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {
                }
        );
        action.actionPerformed(null);

        assertFalse(action.isEnabled());
        verify(dialogs).showError("Compiler is not set", "Compile");
    }

    @Test
    public void constructorRegistersCompilerListenerThatAppendsMessagesAndMovesEditorCaret() {
        VirtualComputer computer = mock(VirtualComputer.class);
        Dialogs dialogs = mock(Dialogs.class);
        Editor editor = mock(Editor.class);
        Compiler compiler = mock(Compiler.class);
        JTextArea compilerOutput = new JTextArea();
        ArgumentCaptor<CompilerListener> listenerCaptor = ArgumentCaptor.forClass(CompilerListener.class);
        SourceCodePosition position = SourceCodePosition.of(1, 2, "program.asm");

        when(computer.getCompiler()).thenReturn(Optional.of(compiler));

        new CompileAction(computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {
        });
        verify(compiler).addCompilerListener(listenerCaptor.capture());

        CompilerListener listener = listenerCaptor.getValue();
        listener.onStart();
        listener.onMessage(new CompilerMessage(CompilerMessage.MessageType.TYPE_WARNING, "Warn", position));
        listener.onFinish();

        assertTrue(compilerOutput.getText().contains("Compiling started..."));
        assertTrue(compilerOutput.getText().contains("Warn"));
        assertTrue(compilerOutput.getText().contains("Compiling has finished."));
        verify(editor).setPosition(position);
    }

    @Test
    public void actionShowsRunningErrorAndStopsWhenSaveFails() {
        VirtualComputer computer = mock(VirtualComputer.class);
        Dialogs dialogs = mock(Dialogs.class);
        Editor editor = mock(Editor.class);
        Compiler compiler = mock(Compiler.class);
        JTextArea compilerOutput = new JTextArea();
        AtomicInteger titleUpdates = new AtomicInteger();

        when(computer.getCompiler()).thenReturn(Optional.of(compiler));

        CompileAction runningAction = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_RUNNING, compilerOutput, titleUpdates::incrementAndGet
        );
        runningAction.actionPerformed(null);
        verify(dialogs).showError("Emulation must be stopped first.", "Compile");

        reset(dialogs, editor, computer, compiler);
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(editor.saveFile()).thenReturn(false);

        CompileAction saveFailAction = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, titleUpdates::incrementAndGet
        );
        saveFailAction.actionPerformed(null);

        verify(editor).saveFile();
        assertEquals(0, titleUpdates.get());
        verifyNoMoreInteractions(dialogs);
    }

    @Test
    public void actionCompilesCurrentFileAndResetsMemoryAndCpu() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        Dialogs dialogs = mock(Dialogs.class);
        Editor editor = mock(Editor.class);
        Compiler compiler = mock(Compiler.class);
        Memory memory = mock(Memory.class);
        CPU cpu = mock(CPU.class);
        JTextArea compilerOutput = new JTextArea("old");
        AtomicInteger titleUpdates = new AtomicInteger();
        File file = new File("program.asm");

        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(computer.getMemory()).thenReturn(Optional.of(memory));
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(editor.saveFile()).thenReturn(true);
        when(editor.getCurrentFile()).thenReturn(Optional.of(file));

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, titleUpdates::incrementAndGet
        );
        action.actionPerformed(null);

        assertEquals("", compilerOutput.getText());
        assertEquals(1, titleUpdates.get());
        verify(memory).reset();
        verify(compiler).compile(Path.of("program.asm"), Optional.empty());
        verify(cpu).reset();
    }

    @Test
    public void actionAppendsCompilationFailureToOutput() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        Dialogs dialogs = mock(Dialogs.class);
        Editor editor = mock(Editor.class);
        Compiler compiler = mock(Compiler.class);
        JTextArea compilerOutput = new JTextArea();
        File file = new File("program.asm");

        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getCPU()).thenReturn(Optional.empty());
        when(editor.saveFile()).thenReturn(true);
        when(editor.getCurrentFile()).thenReturn(Optional.of(file));
        doThrow(new RuntimeException("compile failed")).when(compiler).compile(Path.of("program.asm"), Optional.empty());

        CompileAction action = new CompileAction(
                computer, dialogs, editor, () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {
                }
        );
        action.actionPerformed(null);

        assertTrue(compilerOutput.getText().contains("Could not compile file:"));
        assertTrue(compilerOutput.getText().contains("compile failed"));
    }
}
