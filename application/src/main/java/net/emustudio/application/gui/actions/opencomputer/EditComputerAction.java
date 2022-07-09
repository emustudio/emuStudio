package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.gui.dialogs.SchemaEditorDialog;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

public class EditComputerAction extends AbstractAction {
    private final Dialogs dialogs;
    private final ApplicationConfig applicationConfig;
    private final Runnable update;
    private final JDialog parent;
    private final JList<ComputerConfig> lstConfig;

    public EditComputerAction(Dialogs dialogs, ApplicationConfig applicationConfig,
                              Runnable update, JDialog parent, JList<ComputerConfig> lstConfig) {
        super(
            "Edit computer...", new ImageIcon(EditComputerAction.class.getResource("/net/emustudio/application/gui/dialogs/computer.png"))
        );
        this.dialogs = Objects.requireNonNull(dialogs);
        this.applicationConfig = Objects.requireNonNull(applicationConfig);
        this.update = Objects.requireNonNull(update);
        this.parent = Objects.requireNonNull(parent);
        this.lstConfig = Objects.requireNonNull(lstConfig);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Optional
            .ofNullable(lstConfig.getSelectedValue())
            .ifPresentOrElse(computer -> {
                Schema schema = new Schema(computer, applicationConfig);
                new SchemaEditorDialog(parent, schema, dialogs).setVisible(true);
                update.run();
            }, () -> dialogs.showError("A computer has to be selected!", "Edit computer"));
    }
}
