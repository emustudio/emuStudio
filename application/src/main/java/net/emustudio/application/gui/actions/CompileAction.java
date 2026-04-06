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

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_COMPILER;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;


public class CompileAction extends AbstractAction {

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
        computer.getCompiler().ifPresent(c -> c.addCompilerListener(new CompilerListener() {

            @Override
            public void onStart() {
                compilerOutput.append("Compiling started...\n");
            }

            @Override
            public void onMessage(CompilerMessage message) {
                compilerOutput.append(message.getFormattedMessage() + "\n");
                message.getPosition().ifPresent(editor::setPosition);
            }

            @Override
            public void onFinish() {
                compilerOutput.append("Compiling has finished.\n");
            }
        }));
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCompiler().ifPresentOrElse(compiler -> {
            if (runState.get() == CPU.RunState.STATE_RUNNING) {
                dialogs.showError("Emulation must be stopped first.", "Compile");
            } else if (editor.saveFile()) {
                updateTitle.run();

                editor.getCurrentFile().ifPresent(file -> {
                    compilerOutput.setText("");

                    try {
                        computer.getMemory().ifPresent(Memory::reset);
                        compiler.compile(file.toPath(), Optional.empty());
                        computer.getCPU().ifPresent(Plugin::reset);
                    } catch (Exception e) {
                        compilerOutput.append("Could not compile file: " + e + "\n");
                    }
                });
            }
        }, () -> dialogs.showError("Compiler is not set", "Compile"));
    }
}
