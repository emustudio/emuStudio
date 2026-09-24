/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.schema.elements.CompilerElement;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SchemaPreviewPanelTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void paintComputesPreferredSizeAndSaveWritesImage() throws Exception {
        Dialogs dialogs = mock(Dialogs.class);
        Path output = temporaryFolder.newFile("schema.png").toPath();

        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "preview")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));
            schema.setCompilerElement(new Point(50, 60), "compiler.jar");
            CompilerElement compiler = findElement(schema);
            compiler.measure(SchemaTestSupport.createGraphics());

            SchemaPreviewPanel panel = new SchemaPreviewPanel(schema, dialogs);
            Graphics2D graphics = SchemaTestSupport.createGraphics();
            try {
                panel.paintComponent(graphics);
            } finally {
                graphics.dispose();
            }

            assertTrue(panel.getPreferredSize().width > 0);
            assertTrue(panel.getPreferredSize().height > 0);

            when(dialogs.chooseFile(anyString(), anyString(), any(Path.class), eq(true), any(FileExtensionsFilter.class)))
                    .thenReturn(Optional.of(output));

            panel.saveSchemaImage();

            assertTrue(waitForImage(output));
        }
    }

    @Test
    public void saveWithoutSchemaShowsError() {
        Dialogs dialogs = mock(Dialogs.class);
        SchemaPreviewPanel panel = new SchemaPreviewPanel(null, dialogs);

        panel.saveSchemaImage();

        verify(dialogs).showError("Could not save schema image: schema is not set.", "Save schema image");
    }

    private CompilerElement findElement(Schema schema) {
        for (net.emustudio.application.gui.schema.elements.Element element : schema.getAllElements()) {
            if (element instanceof CompilerElement) {
                return (CompilerElement) element;
            }
        }
        throw new AssertionError("Missing compiler element");
    }

    private boolean waitForImage(Path image) throws Exception {
        for (int i = 0; i < 200; i++) {
            if (Files.size(image) > 0) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }
}
