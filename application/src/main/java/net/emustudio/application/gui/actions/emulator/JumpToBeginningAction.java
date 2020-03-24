package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class JumpToBeginningAction extends AbstractAction {
    private final VirtualComputer computer;
    private final Runnable refreshDebugTable;

    public JumpToBeginningAction(VirtualComputer computer, Runnable refreshDebugTable) {
        super(
            "Jump to beginning",
            new ImageIcon(JumpToBeginningAction.class.getResource("/net/emustudio/application/gui/dialogs/go-first.png"))
        );

        this.computer = Objects.requireNonNull(computer);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresent(cpu -> {
            cpu.setInstructionLocation(0);
            refreshDebugTable.run();
        });
    }
}
