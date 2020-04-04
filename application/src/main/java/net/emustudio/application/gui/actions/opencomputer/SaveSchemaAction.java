package net.emustudio.application.gui.actions.opencomputer;

import net.emustudio.application.gui.schema.SchemaPreviewPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class SaveSchemaAction extends AbstractAction {
    private final SchemaPreviewPanel preview;

    public SaveSchemaAction(SchemaPreviewPanel preview) {
        super(
            "Save schema image...",
            new ImageIcon(SaveSchemaAction.class.getResource("/net/emustudio/application/gui/dialogs/document-save.png"))
        );
        this.preview = Objects.requireNonNull(preview);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        preview.saveSchemaImage();
    }
}
