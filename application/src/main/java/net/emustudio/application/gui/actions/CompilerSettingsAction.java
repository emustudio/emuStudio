package net.emustudio.application.gui.actions;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;

public class CompilerSettingsAction extends AbstractAction {
    private final JFrame parent;
    private final VirtualComputer computer;

    public CompilerSettingsAction(JFrame parent, VirtualComputer computer) {
        super("Compiler settings...");
        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);

        putValue(MNEMONIC_KEY, KeyEvent.VK_S);

        setEnabled(computer.getCompiler().filter(Compiler::isShowSettingsSupported).isPresent());
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional<Compiler> compiler = computer.getCompiler();
        if (compiler.isPresent() && (compiler.get().isShowSettingsSupported())) {
            compiler.get().showSettings(parent);
        }
    }
}
