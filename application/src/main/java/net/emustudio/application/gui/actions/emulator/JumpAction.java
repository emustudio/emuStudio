package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class JumpAction extends AbstractAction {

    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Runnable refreshDebugTable;

    public JumpAction(VirtualComputer computer, Dialogs dialogs, Runnable refreshDebugTable) {
        super("Jump...", new ImageIcon(JumpAction.class.getResource("/net/emustudio/application/gui/dialogs/go-jump.png")));

        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.refreshDebugTable = Objects.requireNonNull(refreshDebugTable);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getCPU().ifPresentOrElse(cpu -> {
            try {
                dialogs
                    .readInteger("Memory address:", "Jump to address", 0)
                    .ifPresent(address -> {
                        if (!cpu.setInstructionLocation(address)) {
                            int memorySize = computer.getMemory().map(Memory::getSize).orElse(0);
                            String maxSizeMessage = (memorySize == 0) ? "" : "(probably accepts range from 0 to " + memorySize + ")";
                            dialogs.showError("Invalid memory address" + maxSizeMessage);
                        } else {
                            refreshDebugTable.run();
                        }
                    });
            } catch (NumberFormatException e) {
                dialogs.showError("Invalid address format", "Jump to address");
            }
        }, () -> dialogs.showInfo("CPU is not set", "Jump to address"));
    }
}
