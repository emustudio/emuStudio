package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class OpenComputerAction extends AbstractAction {
    private final Dialogs dialogs;
    private final JDialog parent;
    private final JList<ComputerConfig> lstConfig;
    private final Consumer<ComputerConfig> selectComputer;

    public OpenComputerAction(Dialogs dialogs, JDialog parent, JList<ComputerConfig> lstConfig,
                              Consumer<ComputerConfig> selectComputer) {
        super(
            "Create new computer...", new ImageIcon(OpenComputerAction.class.getResource("/net/emustudio/application/gui/dialogs/list-add.png"))
        );
        this.dialogs = Objects.requireNonNull(dialogs);
        this.parent = Objects.requireNonNull(parent);
        this.lstConfig = Objects.requireNonNull(lstConfig);
        this.selectComputer = Objects.requireNonNull(selectComputer);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional
            .ofNullable(lstConfig.getSelectedValue())
            .ifPresentOrElse(computer -> {
                selectComputer.accept(computer);
                parent.dispose();
            }, () -> dialogs.showError("A computer has to be selected!", "Open computer"));
    }
}
