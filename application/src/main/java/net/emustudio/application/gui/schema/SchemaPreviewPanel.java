/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.components.P;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.io.FileType;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class SchemaPreviewPanel extends JPanel implements Scrollable {
    private final static Logger LOGGER = LoggerFactory.getLogger(SchemaPreviewPanel.class);

    private final Dialogs dialogs;

    private Schema schema;
    private int schemaWidth;
    private int schemaHeight;
    private File lastImageFile;

    private Rectangle schemaBounds = new Rectangle();
    private final AtomicBoolean savingImage = new AtomicBoolean();

    public SchemaPreviewPanel(Schema schema, Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.schema = schema;

        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        setOpaque(true);
        setToolTipText("Schema fitted to the available space. Export keeps its original size.");
        getAccessibleContext().setAccessibleName("Virtual computer schema preview");
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
        schemaWidth = schemaHeight = 0;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (schemaWidth > 0 && schemaHeight > 0) {
            return new Dimension(schemaWidth, schemaHeight);
        }
        return super.getPreferredSize();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (schema == null) {
            return;
        }

        Graphics2D graphics = (Graphics2D) g.create();
        try {
            measureSchema(graphics);
            drawSchema(graphics, getWidth(), getHeight());
        } finally { graphics.dispose(); }
    }

    public void saveSchemaImage() {
        if (schema != null) {
            Path currentDirectory = Optional
                    .ofNullable(lastImageFile)
                    .map(File::getParentFile)
                    .map(File::toPath)
                    .orElse(Path.of(System.getProperty("user.dir")));

            dialogs.chooseFile(
                    "Save schema image", "Save", currentDirectory, true,
                    List.of(new FileType("PNG image", "png"))
            ).ifPresent(path -> {
                if (!savingImage.compareAndSet(false, true)) {
                    return;
                }
                lastImageFile = path.toFile();

                BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                Graphics2D measuring = measure.createGraphics();
                try { measureSchema(measuring); } finally { measuring.dispose(); }
                BufferedImage bi = new BufferedImage(schemaWidth, schemaHeight, BufferedImage.TYPE_INT_RGB);

                Graphics2D graphics = bi.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, schemaWidth, schemaHeight);
                    RenderingHints hints = new RenderingHints(Map.of(
                            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                            RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY,
                            RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON
                    ));
                    graphics.setRenderingHints(hints);
                    drawSchema(graphics, schemaWidth, schemaHeight);
                } finally {
                    graphics.dispose();
                }

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws IOException {
                        ImageIO.write(bi, "png", lastImageFile);
                        return null;
                    }

                    @Override
                    protected void done() {
                        savingImage.set(false);
                        try {
                            get();
                        } catch (Exception e) {
                            LOGGER.error("Could not save schema image.", e);
                            dialogs.showError("Could not save schema image. Please see log file for details.", "Save schema image");
                        }
                    }
                }.execute();
            });
        } else {
            dialogs.showError("Could not save schema image: schema is not set.", "Save schema image");
        }
    }

    private void measureSchema(Graphics graphics) {
        schema.getAllElements().forEach(element -> element.measure(graphics));
        Rectangle bounds = null;
        for (Element element : schema.getAllElements()) {
            Rectangle rectangle = element.getRectangle();
            if (bounds == null) bounds = new Rectangle(rectangle);
            else bounds.add(rectangle);
        }
        for (ConnectionLine line : schema.getConnectionLines()) {
            for (P p : line.getPoints()) {
                if (bounds == null) bounds = new Rectangle(p.ix(), p.iy(), 0, 0);
                else bounds.add(p.x, p.y);
            }
        }
        schemaBounds = bounds == null ? new Rectangle() : bounds;
        int width = schemaBounds.width + 2 * Schema.MIN_LEFT_MARGIN;
        int height = schemaBounds.height + 2 * Schema.MIN_TOP_MARGIN;
        boolean changed = width != schemaWidth || height != schemaHeight;
        schemaWidth = Math.max(1, width);
        schemaHeight = Math.max(1, height);
        if (changed) revalidate();
    }

    private void drawSchema(Graphics2D graphics, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double scale = Math.min(1, Math.min((double) width / schemaWidth, (double) height / schemaHeight));
        graphics.translate((width - schemaWidth * scale) / 2, (height - schemaHeight * scale) / 2);
        graphics.scale(scale, scale);
        graphics.translate(Schema.MIN_LEFT_MARGIN - schemaBounds.x, Schema.MIN_TOP_MARGIN - schemaBounds.y);
        schema.getConnectionLines().forEach(line -> line.draw(graphics, true));
        schema.getAllElements().forEach(element -> element.draw(graphics));
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(16, (orientation == SwingConstants.HORIZONTAL ? visible.width : visible.height) - 16);
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return true; }
}
