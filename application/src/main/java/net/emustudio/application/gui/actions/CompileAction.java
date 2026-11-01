/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ui.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static net.emustudio.emulib.runtime.ui.StandardIcons.ICON_COMPILER;
import static net.emustudio.application.gui.framework.EmuStudioGui.loadIcon;


public class CompileAction extends AbstractAction {
    public static final String STATUS_PROPERTY = "compileStatus";

    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Editor editor;
    private final Supplier<CPU.RunState> runState;
    private final JTextArea compilerOutput;
    private final Runnable updateTitle;

    public CompileAction(VirtualComputer computer, Dialogs dialogs, Editor editor, Supplier<CPU.RunState> runState,
                         JTextArea compilerOutput, Runnable updateTitle) {
        super("Compile", loadIcon(ICON_COMPILER));

        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.editor = Objects.requireNonNull(editor);
        this.runState = Objects.requireNonNull(runState);
        this.compilerOutput = Objects.requireNonNull(compilerOutput);
        this.updateTitle = Objects.requireNonNull(updateTitle);

        putValue(SHORT_DESCRIPTION, "Save & Compile source file");
        putValue(MNEMONIC_KEY, KeyEvent.VK_C);

        setEnabled(computer.getCompiler().isPresent());
        putValue(STATUS_PROPERTY, isEnabled() ? "Ready to compile." : "No compiler configured.");
        computer.getCompiler().ifPresent(c -> c.addCompilerListener(new CompilerListener() {
            private int errors, warnings;

            @Override
            public void onStart() {
                errors = warnings = 0;
                putValue(STATUS_PROPERTY, "Compiling…");
                compilerOutput.append("Compiling started...\n");
            }

            @Override
            public void onMessage(CompilerMessage message) {
                if (message.getMessageType() == CompilerMessage.MessageType.TYPE_ERROR) errors++;
                if (message.getMessageType() == CompilerMessage.MessageType.TYPE_WARNING) warnings++;
                compilerOutput.append(message.getFormattedMessage() + "\n");
                message.getPosition().ifPresent(editor::setPosition);
            }

            @Override
            public void onFinish() {
                compilerOutput.append("Compiling has finished.\n");
                putValue(STATUS_PROPERTY, errors > 0
                        ? "Compilation failed (" + errors + (errors == 1 ? " error)." : " errors).") + " Fix source and compile again."
                        : warnings > 0 ? "Compilation complete (" + warnings + (warnings == 1 ? " warning)." : " warnings).")
                        : "Compilation complete.");
            }
        }));
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCompiler().ifPresentOrElse(compiler -> {
            if (runState.get() == CPU.RunState.STATE_RUNNING) {
                putValue(STATUS_PROPERTY, "Pause emulation before compiling.");
                dialogs.showError("Emulation must be stopped first.", "Compile");
            } else if (editor.saveFile()) {
                updateTitle.run();

                editor.getCurrentFile().ifPresent(file -> {
                    compilerOutput.setText("");

                    Runnable compile = () -> {
                        putValue(STATUS_PROPERTY, "Compiling…");
                        try {
                            computer.getMemory().ifPresent(Memory::reset);
                            compiler.compile(file.toPath(), Optional.empty());
                            computer.getCPU().ifPresent(Plugin::reset);
                        } catch (Exception e) {
                            putValue(STATUS_PROPERTY, "Compilation failed. See compiler output and compile again.");
                            compilerOutput.append("Could not compile file: " + e + "\n");
                        }
                    };
                    computer.getDebugger().ifPresentOrElse(debugger -> debugger.runMutation(compile), compile);
                });
            } else putValue(STATUS_PROPERTY, "Source not saved — compilation cancelled.");
        }, () -> {
            putValue(STATUS_PROPERTY, "No compiler configured.");
            dialogs.showError("Compiler is not set", "Compile");
        });
    }
}
