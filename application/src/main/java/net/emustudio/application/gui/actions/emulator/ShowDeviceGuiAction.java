package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Supplier;

public class ShowDeviceGuiAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(ShowDeviceGuiAction.class);

    private final JFrame parent;
    private final VirtualComputer computer;
    private final Dialogs dialogs;
    private final Supplier<Integer> selectedIndex;

    public ShowDeviceGuiAction(JFrame parent, VirtualComputer computer, Dialogs dialogs, Supplier<Integer> selectedIndex) {
        super("Show device...");
        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.selectedIndex = Objects.requireNonNull(selectedIndex);
        setEnabled(selectedIndex.get() != -1);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            int i = selectedIndex.get();
            if (i == -1) {
                dialogs.showError("Device has to be selected!", "Show device");
            } else {
                computer.getDevices().get(i).showGUI(parent);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot show device.", e);
            dialogs.showError("Unexpected error. Please see log file for details", "Show device");
        }
    }
}
