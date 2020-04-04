package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.ConfigFiles;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class DeleteComputerAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeleteComputerAction.class);

    private final Dialogs dialogs;
    private final ConfigFiles configFiles;
    private final Runnable update;
    private final JList<ComputerConfig> lstConfig;

    public DeleteComputerAction(Dialogs dialogs, ConfigFiles configFiles, Runnable update,
                                JList<ComputerConfig> lstConfig) {
        super(
            "Delete computer", new ImageIcon(DeleteComputerAction.class.getResource("/net/emustudio/application/gui/dialogs/list-remove.png"))
        );
        this.dialogs = Objects.requireNonNull(dialogs);
        this.configFiles = Objects.requireNonNull(configFiles);
        this.update = Objects.requireNonNull(update);
        this.lstConfig = Objects.requireNonNull(lstConfig);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional
            .ofNullable(lstConfig.getSelectedValue())
            .ifPresentOrElse(computer -> {
                Dialogs.DialogAnswer answer = dialogs.ask("Do you really want to delete selected computer?", "Delete computer");
                if (answer == Dialogs.DialogAnswer.ANSWER_YES) {
                    try {
                        configFiles.removeConfiguration(computer.getName());
                        lstConfig.clearSelection();
                        update.run();
                    } catch (IOException ex) {
                        LOGGER.error("Could not remove computer configuration", ex);
                        dialogs.showError("Computer could not be deleted. Please consult log for details.");
                    }
                }
            }, () -> dialogs.showError("A computer has to be selected!", "Delete computer"));
    }
}
