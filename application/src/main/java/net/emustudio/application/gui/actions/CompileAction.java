/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.editor.CompilerOutputPanel;
import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.compiler.CompilerListener;
import net.emustudio.emulib.plugin.compiler.CompilerMessage;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.application.ui.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static net.emustudio.application.gui.framework.EmuStudioGui.loadIcon;


public class CompileAction extends AbstractAction {
    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Editor editor;
    private final Supplier<CPU.RunState> runState;
    private final CompilerOutputPanel compilerOutput;
    private final Runnable updateTitle;
    private volatile Path compilingSource;

    public CompileAction(VirtualComputer computer, Dialogs dialogs, Editor editor, Supplier<CPU.RunState> runState,
                         CompilerOutputPanel compilerOutput, Runnable updateTitle) {
        super("Compile", loadIcon("/net/emustudio/application/gui/icons/compiler.png"));

        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.editor = Objects.requireNonNull(editor);
        this.runState = Objects.requireNonNull(runState);
        this.compilerOutput = Objects.requireNonNull(compilerOutput);
        this.updateTitle = Objects.requireNonNull(updateTitle);

        putValue(SHORT_DESCRIPTION, "Save & Compile source file");
        putValue(MNEMONIC_KEY, KeyEvent.VK_C);

        setEnabled(computer.getCompiler().isPresent());
    }

    public Optional<Path> getCompilingSource() {
        return Optional.ofNullable(compilingSource);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCompiler().ifPresentOrElse(compiler -> {
            if (runState.get() == CPU.RunState.STATE_RUNNING
                    || computer.getExecution().map(execution -> execution.isBusy()).orElse(false)) {
                dialogs.showError("Emulation must be stopped first.", "Compile");
            } else if (editor.saveFile()) {
                updateTitle.run();

                editor.getCurrentFile().ifPresent(file -> {
                    Path source = file.toPath();
                    compilerOutput.begin(source);
                    AtomicBoolean failed = new AtomicBoolean();

                    Runnable compile = () -> {
                        CompilerListener listener = message -> {
                            if (message.getType() == CompilerMessage.Type.ERROR) failed.set(true);
                            runOnEdt(() -> compilerOutput.append(message));
                        };
                        compiler.addCompilerListener(listener);
                        try {
                            compilingSource = source;
                            compiler.compile(source, Optional.empty());
                            if (failed.get()) throw new IllegalStateException("Compilation failed");
                        } finally {
                            compilingSource = null;
                            compiler.removeCompilerListener(listener);
                        }
                    };
                    setEnabled(false);
                    CompletableFuture<?> completed = computer.getExecution()
                            .<CompletableFuture<?>>map(execution -> execution.compile(compile))
                            .orElseGet(() -> CompletableFuture.runAsync(compile));
                    completed.whenComplete((ignored, failure) -> runOnEdt(() -> {
                        if (failure != null && !failed.get()) {
                            Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                                    ? failure.getCause() : failure;
                            String reason = Objects.toString(cause.getMessage(), cause.getClass().getSimpleName());
                            compilerOutput.append(new CompilerMessage(CompilerMessage.Type.ERROR, reason, null));
                        }
                        compilerOutput.finish(failure != null);
                        setEnabled(true);
                    }));
                });
            }
        }, () -> {
            dialogs.showError("Compiler is not set", "Compile");
        });
    }

    private static void runOnEdt(Runnable operation) {
        if (SwingUtilities.isEventDispatchThread()) operation.run();
        else SwingUtilities.invokeLater(operation);
    }
}
