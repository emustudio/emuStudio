package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.virtualcomputer.VirtualComputer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class StepBackAction extends AbstractAction {

    private final VirtualComputer computer;
    private final DebugTableModel debugTableModel;
    private final Runnable refreshDebugTable;

    public StepBackAction(VirtualComputer computer, DebugTableModel debugTableModel, Runnable refreshDebugTable) {
        super("Step Back", new ImageIcon(StepBackAction.class.getResource("/net/emustudio/application/gui/dialogs/go-previous.png")));

        this.computer = Objects.requireNonNull(computer);
        this.debugTableModel = Objects.requireNonNull(debugTableModel);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresent(cpu -> {
            int pc = cpu.getInstructionLocation();
            if (pc > 0) {
                cpu.setInstructionLocation(debugTableModel.guessPreviousInstructionLocation());
                refreshDebugTable.run();
            }
        });
    }
}
