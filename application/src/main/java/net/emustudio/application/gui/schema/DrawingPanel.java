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

/*
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.P;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;
import net.emustudio.application.gui.schema.mode.ModeSelector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DrawingPanel extends JPanel implements MouseListener, MouseMotionListener {
    private final static float[] DASH = {10.0f};
    private final static float[] DOT = {1.0f};

    public final static BasicStroke DASHED_LINE = new BasicStroke(
        1.0f,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER,
        10.0f,
        DASH,
        0.0f);
    private final static BasicStroke DOTTED_LINE = new BasicStroke(
        1.0f,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER,
        1.0f,
        DOT,
        0.0f);

    public enum Tool {
        TOOL_COMPILER,
        TOOL_CPU,
        TOOL_MEMORY,
        TOOL_DEVICE,
        TOOL_CONNECTION,
        TOOL_DELETE,
        TOOL_NOTHING
    }

    private final Color gridColor = new Color(0xDDDDDD);
    private final List<ToolListener> toolListeners = new ArrayList<>();

    private final ModeSelector mode;
    private final DrawingModel drawingModel = new DrawingModel();
    private final Schema schema;

    public interface ToolListener {

        void toolWasUsed();
    }

    public DrawingPanel(Schema schema) {
        this.schema = Objects.requireNonNull(schema);

        mode = new ModeSelector(this, drawingModel);
        mode.select(ModeSelector.SelectMode.MOVING);

        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        setOpaque(true);
    }

    public void addToolListener(ToolListener listener) {
        toolListeners.add(listener);
    }

    public void fireToolWasUsed() {
        toolListeners.forEach(ToolListener::toolWasUsed);
    }

    public void setUsingGrid(boolean useGrid) {
        schema.setUseSchemaGrid(useGrid);
        repaint();
    }

    public void setGridGap(int gridGap) {
        schema.setSchemaGridGap(gridGap);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g;

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        paintGrid(graphics);
        for (Element elem : schema.getAllElements()) {
            elem.measure(graphics);
        }
        for (ConnectionLine line : schema.getConnectionLines()) {
            line.draw(graphics, false);
        }
        for (Element elem : schema.getAllElements()) {
            elem.draw(graphics);
        }
        mode.get().drawTemporaryGraphics(graphics);
    }

    public Schema getSchema() {
        return schema;
    }

    public void cancelDrawing() {
        drawingModel.clear();
    }

    public void setTool(Tool tool, String fileName) {
        drawingModel.clear();

        drawingModel.drawTool = tool;
        drawingModel.pluginFileName = fileName;

        if ((tool == null) || (tool == Tool.TOOL_NOTHING)) {
            mode.select(ModeSelector.SelectMode.MOVING);
        } else {
            mode.select(ModeSelector.SelectMode.MODELING);
        }
        repaint();
    }

    public void setFutureLineDirection(boolean bidirectional) {
        drawingModel.bidirectional = bidirectional;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        mode.select(mode.get().mouseClicked(e));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mode.select(mode.get().mousePressed(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mode.select(mode.get().mouseReleased(e));
        correctPanelSize();
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mode.select(mode.get().mouseDragged(e));
        correctPanelSize();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mode.select(mode.get().mouseMoved(e));
    }

    private void paintGrid(Graphics2D graphics) {
        if (schema.useSchemaGrid()) {
            int gridGap = schema.getSchemaGridGap();
            graphics.setColor(gridColor);
            graphics.setStroke(DOTTED_LINE);

            for (int x = 0; x < getWidth(); x += gridGap) {
                graphics.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += gridGap) {
                graphics.drawLine(0, y, getWidth(), y);
            }
        }
    }

    private void correctPanelSize() {
        Dimension convexHull = new Dimension(0, 0);

        convexHull.width = 0;
        convexHull.height = 0;
        for (Element element : schema.getAllElements()) {
            Rectangle rect = element.getRectangle();

            int leftAndWidth = rect.x + rect.width;
            if (leftAndWidth > convexHull.width) {
                convexHull.width = leftAndWidth;
            }

            int topAndHeight = rect.y + rect.height;
            if (topAndHeight > convexHull.height) {
                convexHull.height = topAndHeight;
            }
        }

        for (ConnectionLine line : schema.getConnectionLines()) {
            for (P p : line.getPoints()) {
                if (p.x > convexHull.width) {
                    convexHull.width = p.ix();
                }
                if (p.y > convexHull.height) {
                    convexHull.height = p.iy();
                }
            }
        }

        if (convexHull.width != 0 && convexHull.height != 0) {
            this.setPreferredSize(convexHull);
            this.revalidate();
        }
    }
}
