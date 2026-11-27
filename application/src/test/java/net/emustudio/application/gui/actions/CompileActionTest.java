/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;


import static net.emustudio.application.TestCpu.mockCpu;

import net.emustudio.application.gui.editor.CompilerOutputPanel;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.CompilerListener;
import net.emustudio.emulib.plugin.compiler.CompilerMessage;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.application.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
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
    private CompilerOutputPanel compilerOutput;

    @Before
    public void setUp() {
        computer = mock(VirtualComputer.class);
        dialogs = mock(Dialogs.class);
        editor = mock(Editor.class);
        compiler = mock(Compiler.class);
        compilerOutput = new CompilerOutputPanel(new EmuStudioGui(), editor::setPosition);
        EmulationController execution = mock(EmulationController.class);
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
    public void diagnosticsKeepTheirPositionsAndOnlyNavigateOnActivation() throws Exception {
        SourceCodePosition position = SourceCodePosition.of(1, 2, "include.asm");
        compileWithMessages(
                new CompilerMessage(CompilerMessage.Type.ERROR, "First error", position),
                new CompilerMessage(CompilerMessage.Type.ERROR, "Second error", SourceCodePosition.of(4, 0, "program.asm")),
                new CompilerMessage(CompilerMessage.Type.ERROR, "No source location", null)
        );

        JTextArea log = components(compilerOutput, JTextArea.class).get(0);
        assertTrue(log.getText().contains("ERROR  include.asm:1:3  First error"));
        verify(editor, never()).setPosition(any());
        SwingUtilities.invokeAndWait(() -> {
            Object enter = log.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            log.setCaretPosition(log.getText().indexOf("First error"));
            log.getActionMap().get(enter).actionPerformed(null);
            log.setCaretPosition(log.getText().indexOf("No source location"));
            log.getActionMap().get(enter).actionPerformed(null);
        });
        verify(editor).setPosition(position);
        verify(editor, times(1)).setPosition(any());
        assertTrue(labels().contains("Compilation failed"));
    }

    @Test
    public void resultDistinguishesConfirmedLoadFromAnUnavailableMemory() throws Exception {
        CompilerMessage output = new CompilerMessage(CompilerMessage.Type.INFO,
                "Compile was successful.\n\tOutput: /example/program.hex\n\tProgram starts at 0x03E8", null);
        compileWithMessages(output, new CompilerMessage(CompilerMessage.Type.INFO,
                "Compiled file was loaded into memory.", null));

        assertTrue(labels().contains("Compiled and loaded"));
        assertTrue(labels().stream().anyMatch(text -> text.contains("program.hex") && text.contains("0x03E8")));
        assertTrue(components(compilerOutput, JTextArea.class).get(0).getText().contains("/example/program.hex"));

        compileWithMessages(output, new CompilerMessage(CompilerMessage.Type.WARNING, "Memory is not available.", null));
        assertTrue(labels().contains("Compiled · memory unavailable"));
        assertFalse(labels().contains("Compiled and loaded"));

        compileWithMessages(output, new CompilerMessage(CompilerMessage.Type.ERROR, "Memory write failed", null));
        assertTrue(labels().contains("Compiled · load unconfirmed"));
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
        JTextArea log = components(compilerOutput, JTextArea.class).get(0);
        log.setText("old");

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

        assertEquals("", log.getText());
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

        assertTrue(labels().contains("Compilation failed"));
        assertTrue(components(compilerOutput, JTextArea.class).get(0).getText().contains("ERROR  compile failed"));
    }

    private void compileWithMessages(CompilerMessage... messages) throws Exception {
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(editor.saveFile()).thenReturn(true);
        when(editor.getCurrentFile()).thenReturn(Optional.of(new File("program.asm")));
        List<CompilerListener> listeners = new ArrayList<>();
        doAnswer(call -> { listeners.add(call.getArgument(0)); return null; }).when(compiler).addCompilerListener(any());
        doAnswer(call -> { listeners.remove(call.getArgument(0)); return null; }).when(compiler).removeCompilerListener(any());
        doAnswer(call -> {
            for (CompilerMessage message : messages) listeners.forEach(listener -> listener.onMessage(message));
            return null;
        }).when(compiler).compile(any(), any());
        CompileAction action = new CompileAction(computer, dialogs, editor,
                () -> CPU.RunState.STATE_STOPPED_BREAK, compilerOutput, () -> {});
        SwingUtilities.invokeAndWait(() -> action.actionPerformed(null));
        assertTrue(listeners.isEmpty());
    }

    private List<String> labels() {
        List<String> labels = new ArrayList<>();
        components(compilerOutput, JLabel.class).forEach(label -> labels.add(label.getText()));
        return labels;
    }

    private static <T extends Component> List<T> components(Container parent, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) matches.add(type.cast(component));
            if (component instanceof Container) matches.addAll(components((Container) component, type));
        }
        return matches;
    }

    private static void flushEdt() {
        try { SwingUtilities.invokeAndWait(() -> {}); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }

}
