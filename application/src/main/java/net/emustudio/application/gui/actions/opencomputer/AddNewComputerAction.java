package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.ConfigFiles;
import net.emustudio.application.gui.dialogs.SchemaEditorDialog;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.internal.Unchecked;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class AddNewComputerAction extends AbstractAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(AddNewComputerAction.class);

    private final Dialogs dialogs;
    private final ConfigFiles configFiles;
    private final ApplicationConfig applicationConfig;
    private final Runnable update;
    private final JDialog parent;

    public AddNewComputerAction(Dialogs dialogs, ConfigFiles configFiles, ApplicationConfig applicationConfig,
                                Runnable update, JDialog parent) {
        super(
            "Create new computer...", new ImageIcon(AddNewComputerAction.class.getResource("/net/emustudio/application/gui/dialogs/list-add.png"))
        );
        this.dialogs = Objects.requireNonNull(dialogs);
        this.configFiles = Objects.requireNonNull(configFiles);
        this.applicationConfig = Objects.requireNonNull(applicationConfig);
        this.update = Objects.requireNonNull(update);
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional<String> computerName = dialogs.readString("Enter computer name:", "Create new computer");
        computerName.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                dialogs.showError("Computer name must be non-empty", "Create new computer");
            } else {
                try {
                    configFiles
                        .loadConfiguration(name)
                        .ifPresentOrElse(
                            c -> dialogs.showError("Computer '" + name + "' already exists, choose another name."),
                            () -> {
                                ComputerConfig newComputer = Unchecked.call(() -> configFiles.createConfiguration(name));
                                Schema schema = new Schema(newComputer, applicationConfig);
                                SchemaEditorDialog di = new SchemaEditorDialog(parent, schema, configFiles, dialogs);
                                di.setVisible(true);
                                update.run();
                            }
                        );
                } catch (IOException ex) {
                    LOGGER.error("Could not load computer with name '" + name + "'", ex);
                    dialogs.showError("Could not load computer with name '" + name + "'. Please see log file for details.");
                }
            }
        });
    }
}
