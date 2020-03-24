package net.emustudio.application.gui.actions;

import net.emustudio.application.gui.editor.Editor;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Supplier;

public class CompileAction extends AbstractAction {

    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Editor editor;
    private final Supplier<CPU.RunState> runState;
    private final JTextArea compilerOutput;
    private final Runnable updateTitle;

    public CompileAction(VirtualComputer computer, Dialogs dialogs, Editor editor, Supplier<CPU.RunState> runState,
                         JTextArea compilerOutput, Runnable updateTitle) {
        super("Compile", new ImageIcon(CompileAction.class.getResource("/net/emustudio/application/gui/dialogs/compile.png")));

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
                        compiler.compile(file.getAbsolutePath());
                        int programStart = compiler.getProgramLocation();

                        computer.getMemory().ifPresent(memory -> memory.setProgramLocation(programStart));

                        computer.getCPU().ifPresent(cpu -> cpu.reset(programStart));
                    } catch (Exception e) {
                        compilerOutput.append("Could not compile file: " + e.toString() + "\n");
                    }
                });
            }
        }, () -> dialogs.showError("Compiler is not set", "Compile"));
    }
}
