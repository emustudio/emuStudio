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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
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
            panel.setSize(45, 35);
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
            BufferedImage saved = javax.imageio.ImageIO.read(output.toFile());
            assertEquals(panel.getPreferredSize().width, saved.getWidth());
            assertEquals(panel.getPreferredSize().height, saved.getHeight());
            assertTrue(saved.getWidth() > panel.getWidth());
        }
    }

    @Test
    public void fittingLargeSchemaPreservesPositionsAndSelection() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "large preview")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(false, 10));
            schema.setCompilerElement(new Point(450, 500), "compiler.jar");
            schema.setCpuElement(new Point(2200, 1500), "cpu.jar");
            schema.selectAll();
            List<Point> positions = schema.getAllElements().stream()
                    .map(element -> new Point(element.getX(), element.getY())).collect(Collectors.toList());
            SchemaPreviewPanel panel = new SchemaPreviewPanel(schema, mock(Dialogs.class));
            panel.setSize(320, 180);
            BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try { panel.paintComponent(graphics); } finally { graphics.dispose(); }

            assertEquals(positions, schema.getAllElements().stream()
                    .map(element -> new Point(element.getX(), element.getY())).collect(Collectors.toList()));
            assertTrue(schema.getAllElements().stream().allMatch(element -> element.isSelected()));
            assertTrue(panel.getPreferredSize().width > panel.getWidth());
            assertTrue(panel.getScrollableTracksViewportWidth());
            assertTrue(panel.getScrollableTracksViewportHeight());
            assertTrue(hasDrawing(image, 0, 160));
            assertTrue(hasDrawing(image, 160, 320));
        }
    }

    private boolean hasDrawing(BufferedImage image, int from, int to) {
        for (int x = from; x < to; x++)
            for (int y = 0; y < image.getHeight(); y++)
                if (image.getRGB(x, y) != Color.WHITE.getRGB()) return true;
        return false;
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
