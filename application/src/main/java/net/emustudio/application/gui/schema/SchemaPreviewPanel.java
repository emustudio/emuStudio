/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.P;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SchemaPreviewPanel extends JPanel {
    private final static Logger LOGGER = LoggerFactory.getLogger(SchemaPreviewPanel.class);

    private final Dialogs dialogs;

    private Schema schema;
    private int schemaWidth;
    private int schemaHeight;
    private File lastImageFile;

    /**
     * Left factor is a constant used in panel resizing. It is a distance between panel left and the x position of the
     * nearest point in the schema.
     */
    private int leftFactor = 0;
    /**
     * Top factor is a constant used in panel resizing. It is a distance between panel top and the y position of the
     * nearest point in the schema.
     */
    private int topFactor = 0;

    private boolean panelResized = false;

    public SchemaPreviewPanel(Schema schema, Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.schema = schema;

        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        setOpaque(true);
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
        panelResized = false;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (schema == null) {
            return;
        }

        Graphics2D graphics = (Graphics2D) g;

        boolean moved = panelResized;
        if (!panelResized) {
            resizePanel(g);
        }
        if (!moved) {
            schema.selectAll();
            schema.moveSelection(-leftFactor, -topFactor);
            schema.deselectAll();
            schema.getAllElements().forEach(elem -> elem.measure(g));
        }
        schema.getConnectionLines().forEach(line -> line.draw(graphics, true));
        schema.getAllElements().forEach(element -> element.draw(graphics));
    }

    public void saveSchemaImage() {
        if (schema != null && panelResized) {
            Path currentDirectory = Optional
                .ofNullable(lastImageFile)
                .map(File::getParentFile)
                .map(File::toPath)
                .orElse(Path.of(System.getProperty("user.dir")));

            dialogs.chooseFile(
                "Save schema image", "Save", currentDirectory, true,
                new FileExtensionsFilter("PNG image", "png")
            ).ifPresent(path -> {
                lastImageFile = path.toFile();

                // Save the image
                BufferedImage bi = new BufferedImage(schemaWidth, schemaHeight, BufferedImage.TYPE_INT_RGB);

                Graphics2D graphics = bi.createGraphics();
                graphics.setBackground(Color.WHITE);
                graphics.fillRect(0, 0, schemaWidth, schemaHeight);
                RenderingHints hints = new RenderingHints(Map.of(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                    RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY,
                    RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON
                ));

                graphics.setRenderingHints(hints);
                paintComponent(graphics);
                try {
                    ImageIO.write(bi, "png", lastImageFile);
                } catch (IOException e) {
                    LOGGER.error("Could not save schema image.", e);
                    dialogs.showError("Could not save schema image. Please see log file for details.", "Save schema image");
                }
            });
        } else {
            dialogs.showError("Could not save schema image: schema is not set.", "Save schema image");
        }
    }

    private void resizePanel(Graphics graphics) {
        if (schema == null) {
            schemaHeight = 0;
            schemaWidth = 0;
            leftFactor = 0;
            topFactor = 0;
            panelResized = true;
            return;
        }

        schema.getAllElements().forEach(element -> element.measure(graphics));

        Rectangle schemaRectangle = new Rectangle(-1, -1, 0, 0);
        for (Element element : schema.getAllElements()) {
            Rectangle rectangle = element.getRectangle();
            schemaRectangle.add(rectangle);
        }

        for (ConnectionLine line : schema.getConnectionLines()) {
            for (P p : line.getPoints()) {
                schemaRectangle.add(p.x, p.y);
            }
        }

        leftFactor = schemaRectangle.x - Schema.MIN_LEFT_MARGIN;
        topFactor = schemaRectangle.y - Schema.MIN_TOP_MARGIN;
        if (schemaRectangle.width != 0 && schemaRectangle.height != 0) {
            this.setSize(
                schemaRectangle.width - leftFactor + Schema.MIN_LEFT_MARGIN,
                schemaRectangle.height - topFactor + Schema.MIN_TOP_MARGIN
            );
            this.revalidate();
        }
        schemaWidth = schemaRectangle.width + Schema.MIN_LEFT_MARGIN;
        schemaHeight = schemaRectangle.height + Schema.MIN_TOP_MARGIN;
        panelResized = true;
    }
}
