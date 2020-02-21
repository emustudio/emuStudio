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
package emustudio.drawing;

import emustudio.drawing.mode.ModeSelector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the drawing canvas - panel by which the user can modelling abstract schemas of virtual computers.
 * <p>
 * The drawing is realized by capturing mouse events (motion, clicks). The "picture" is synchronized with the Schema
 * object automatically.
 * <p>
 * The panel has states, or modes. The initial mode is "moving" mode.
 */
public class DrawingPanel extends JPanel implements MouseListener, MouseMotionListener {
    private final static float DASH[] = {10.0f};
    private final static float DOT[] = {1.0f};

    final static Integer DEFAULT_GRID_GAP = 20;

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
        compiler,
        CPU,
        memory,
        device,
        connection,
        delete,
        nothing
    }

    private final Color gridColor = new Color(0xDDDDDD);
    private final List<ToolListener> toolListeners = new ArrayList<>();

    private final ModeSelector mode;
    private final Model model;
    private final Schema schema;

    private Graphics2D doubleBufferingGraphics;
    private Image secondBuffer;

    private final JDialog parentDialog;

    public interface ToolListener {

        void toolWasUsed();
    }

    public DrawingPanel(Schema schema, JDialog parent) {
        this.parentDialog = parent;
        this.schema = schema;

        model = new Model();
        mode = new ModeSelector(this, model);
        mode.select(ModeSelector.SelectMode.MOVING);

        this.setBackground(Color.WHITE);
    }

    public void addToolListener(ToolListener listener) {
        toolListeners.add(listener);
    }

    public JDialog getParentDialog() {
        return parentDialog;
    }

    public void fireToolWasUsed() {
        toolListeners.forEach(ToolListener::toolWasUsed);
    }

    public void setUsingGrid(boolean useGrid) {
        schema.setUsingGrid(useGrid);
        repaint();
    }

    /**
     * Set the grid gap. After this change, the panel is repainted.
     *
     * @param gridGap grid gap in pixels
     */
    public void setGridGap(int gridGap) {
        schema.setGridGap(gridGap);
        repaint();
    }

    @Override
    public void update(Graphics g) {
        if (secondBuffer == null) {
            secondBuffer = createImage(this.getSize().width, this.getSize().height);
            doubleBufferingGraphics = (Graphics2D) secondBuffer.getGraphics();
        }

        doubleBufferingGraphics.setColor(getBackground());
        doubleBufferingGraphics.fillRect(0, 0, this.getSize().width, this.getSize().height);

        doubleBufferingGraphics.setColor(getForeground());
        paint(doubleBufferingGraphics);

        g.drawImage(secondBuffer, 0, 0, this);
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
            for (Point p : line.getPoints()) {
                if (p.x > convexHull.width) {
                    convexHull.width = p.x;
                }
                if (p.y > convexHull.height) {
                    convexHull.height = p.y;
                }
            }
        }

        if (convexHull.width != 0 && convexHull.height != 0) {
            this.setPreferredSize(convexHull);
            this.revalidate();
        }
    }

    private void paintGrid(Graphics2D graphics) {
        if (!schema.isGridUsed()) {
            return;
        }

        int gridGap = schema.getGridGap();
        graphics.setColor(gridColor);
        graphics.setStroke(DOTTED_LINE);

        for (int x = 0; x < getWidth(); x += gridGap) {
            graphics.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += gridGap) {
            graphics.drawLine(0, y, getWidth(), y);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g;

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
        model.clear();
    }

    public void setTool(Tool tool, String newPluginName) {
        model.clear();

        model.drawTool = tool;
        model.newPluginName = newPluginName;

        if ((tool == null) || (tool == Tool.nothing)) {
            mode.select(ModeSelector.SelectMode.MOVING);
        } else {
            mode.select(ModeSelector.SelectMode.MODELING);
        }
        repaint();
    }

    public void setFutureLineDirection(boolean bidirectional) {
        model.bidirectional = bidirectional;
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

}
