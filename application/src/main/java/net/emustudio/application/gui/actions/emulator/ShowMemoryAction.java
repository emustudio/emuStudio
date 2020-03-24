package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class ShowMemoryAction extends AbstractAction {

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;

    public ShowMemoryAction(JFrame parent, VirtualComputer computer, Dialogs dialogs) {
        super("Show memory...", new ImageIcon(ShowMemoryAction.class.getResource("/net/emustudio/application/gui/dialogs/grid_memory.gif")));

        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        computer.getMemory()
            .filter(Memory::isShowSettingsSupported)
            .ifPresentOrElse(
                p -> p.showSettings(parent),
                () -> dialogs.showInfo("Memory GUI is not supported", "Show Memory")
            );
    }
}
