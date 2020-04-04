package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.ConfigFiles;
import net.emustudio.emulib.runtime.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class RenameComputerAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(RenameComputerAction.class);

    private final Dialogs dialogs;
    private final ConfigFiles configFiles;
    private final Runnable update;
    private final JList<ComputerConfig> lstConfig;

    public RenameComputerAction(Dialogs dialogs, ConfigFiles configFiles, Runnable update,
                                JList<ComputerConfig> lstConfig) {
        super(
            "Rename computer...", new ImageIcon(RenameComputerAction.class.getResource("/net/emustudio/application/gui/dialogs/rename-computer.png"))
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
            .ifPresentOrElse(computer -> dialogs
                .readString("Enter new computer name:", "Rename computer")
                .ifPresent(newName -> {
                    if (newName.trim().isEmpty()) {
                        dialogs.showError("Computer name must be non-empty", "Rename computer");
                    } else {
                        lstConfig.clearSelection();
                        try {
                            configFiles.renameConfiguration(computer, newName);
                            update.run();
                        } catch (CannotUpdateSettingException | IOException ex) {
                            LOGGER.error("Could not rename computer", ex);
                            dialogs.showError("Computer could not be renamed. Please see log file for details.");
                        }
                    }
                }), () -> dialogs.showError("A computer has to be selected!", "Rename computer"));
    }
}
