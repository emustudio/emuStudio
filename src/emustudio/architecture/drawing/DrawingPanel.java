/*
 * DrawingPanel.java
 *
 * Created on 3.7.2008, 8:31:58
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.architecture.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 * This class handles the drawing canvas - panel by which the user can draw
 * abstract schemas of virtual computers.
 *
 * The drawing is realized by capturing mouse events (motion, clicks).
 *
 * The "picture" is synchronized with the Schema object automatically.
 *
 * The panel has states, or modes.
 * 
 * In "draw" mode, the draw tool is selected and mouse clicks are events causing
 * drawing in the mode.
 * 
 * In "move" mode, no draw tool is selected and elements selection, and drag-n-drop
 * technique are used for elements and points of connection lines by mouse events.
 * Also here it is allowed the movement of a elements selection.
 *
 * Finally in "select" mode, no draw tool is selected neither and user is able
 * to select one or more elements and lines. This mode can be activated only from
 * the "move" mode. When user finishes to select elements, the mode is returned
 * to the "move" mode.
 *
 * The initial mode is "move" mode.
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DrawingPanel extends JPanel implements MouseListener,
        MouseMotionListener {
    /**
     * Whether to use and draw grid
     */
    private boolean useGrid;
    
    /**
     * Gap between vertical and horizontal grid lines
     */
    private int gridGap;

    /**
     * Color of the grid
     */
    private Color gridColor;

    /**
     * A draw tool used by this panel in the time. 
     */
    private PanelDrawTool drawTool;

    /**
     * Mode of the panel. One of the draw, move or select.
     */
    private PanelMode panelMode;

    /**
     * This variable is used when "move" mode is active and user moves
     * an element. It holds the moving element object.
     *
     * If "draw" mode is active and when users draws a line, it represents the
     * first element that the line is connected to. If it is selected the element
     * deletion, it represents a shape that should be deleted when mouse is
     * released.
     */
    private Element tmpElem1;

    /**
     * Used when drawing lines. It represents last element that the line
     * is connected to.
     */
    private Element tmpElem2;

    /**
     * Selected line. Used only in "move" mode.
     *
     * This variable is used if the user wants to remove or move an existing
     * connection line point.
     */
    private ConnectionLine selLine;

    /**
     * Holds a point of a connection line.
     *
     * This is used in "move" mode for:
     *   - move of the connection line point
     *   - add/delete connection line point
     *
     * in the "draw" mode, it is used for:
     *   - holds temporal point that will be added to temporal points array
     *     when mouse is released, while drawing a line
     */
    private Point selPoint;

    /**
     * This variable contains last sketch point when drawing a connection line.
     * The last point is variable according to the mouse position. It actually
     * is the mouse position when drawing a line.
     *
     * It is used only in "draw" mode.
     */
    private Point sketchLastPoint;

    /**
     * Point where the selection starts. It is set when the "selection" mode
     * is activated.
     */
    private Point selectionStart;

    /**
     * Point where the selection ends. It is set when the "selection" mode
     * is active and mouse released.
     */
    private Point selectionEnd;

    private BasicStroke thickLine;
    private Schema schema;

    /**
     * Temporary points used in the process of connection line drawing.
     * If the line is drawn, these points are saved, they are cleared otherwise.
     */
    private ArrayList<Point> tmpPoints;
    
    private String newText;

    /* double buffering */
    private Image dbImage;   // second buffer
    private Graphics2D dbg;  // graphics for double buffering
    
    /**
     * Draw tool enum. This panel
     */
    public enum PanelDrawTool {
        shapeCompiler,
        shapeCPU,
        shapeMemory,
        shapeDevice,
        connectLine,
        delete,
        nothing
    }

    public enum PanelMode {
        draw,
        move,
        select
    }

    /**
     * Creates new instance of the draw panel.
     *
     * @param schema  Schema object for the panel synchronization
     * @param useGrid whether to use grid
     * @param gridGap grid gap in pixels
     */
    public DrawingPanel(Schema schema, boolean useGrid, int gridGap) {
        this.setBackground(Color.WHITE);
        this.schema = schema;
        this.useGrid = useGrid;
        this.gridGap = gridGap;

        panelMode = PanelMode.move;
        drawTool = PanelDrawTool.nothing;

        thickLine = new BasicStroke(2);
        tmpPoints = new ArrayList<Point>();
        gridColor = new Color(0xBFBFBF);
    }

    /**
     * This method searchs for the nearest point that crosses the grid. If the
     * grid is not used, it just return the point represented by the parameter.
     *
     * @param old Point that needs to be corrected by the grid
     * @return nearest grid point from the parameter, or the old point,
     * if grid is not used.
     */
    private Point searchGridPoint(Point old) {
        if (!useGrid || gridGap <= 0)
            return old;
        int dX = (int)Math.round(old.x / (double)gridGap);
        int dY = (int)Math.round(old.y / (double)gridGap);
        return new Point(dX * gridGap, dY * gridGap);
    }

    /**
     * Set/unset to use grid. After the change, the panel is repainted.
     *
     * @param useGrid whether to use grid or not
     */
    public void setUseGrid(boolean useGrid) {
        this.useGrid = useGrid;
        repaint();
    }

    /**
     * Set the grid gap. After this change, the panel is repainted.
     *
     * @param gridGap grid gap in pixels
     */
    public void setGridGap(int gridGap) {
        this.gridGap = gridGap;
        repaint();
    }
    
    /**
     * Override previous update method in order to implement
     * double-buffering. As a second buffer is used the Image object.
     *
     * @param g Graphics object where to paint
     */
    @Override
    public void update(Graphics g) {
        // initialize buffer if needed
        if (dbImage == null) {
            dbImage = createImage (this.getSize().width,
                    this.getSize().height);
            dbg = (Graphics2D)dbImage.getGraphics();
        }
        // clear screen in background
        dbg.setColor(getBackground());
        dbg.fillRect (0, 0, this.getSize().width,
                this.getSize().height);

        // draw elements in background
        dbg.setColor(getForeground());
        paint(dbg);

        // draw image on the screen
        g.drawImage(dbImage, 0, 0, this);
    }

    /**
     * Perform a correction of the panel size. It means that the panel size
     * will be accomodated to the schema needs.
     *
     * It is called after each schema change - new elements creation, or elements
     * movement.
     *
     * The method searches for the furthest elements (or connection line points,
     * because the line cannot be further than the line point) and fit the
     * panel size only from the right and bottom.
     */
    private void panelSizeCorrection() {
        // hladanie najvzdialenejsich elementov (alebo bodov lebo ciara
        // nemoze byt dalej ako bod)
        Dimension area = new Dimension(0,0); // velkost kresliacej plochy

        area.width=0;
        area.height=0;
        ArrayList<Element> a = schema.getAllElements();
        for (int i = 0; i < a.size(); i++) {
            Element e = a.get(i);
            if (e.getX() + e.getWidth() > area.width)
                area.width = e.getX() + e.getWidth();
            if (e.getY() + e.getHeight() > area.height)
                area.height = e.getY() + e.getHeight();
        }
        for (int i = 0; i < schema.getConnectionLines().size(); i++) {
            ArrayList<Point> ps = schema.getConnectionLines().get(i).getPoints();
            for (int j = 0; j < ps.size(); j++) {
                Point p = ps.get(j);
                if ((int)p.getX() > area.width)
                    area.width = (int)p.getX();
                if ((int)p.getY() > area.height)
                    area.height = (int)p.getY();
            }
        }
        if (area.width != 0 && area.height != 0) {
            this.setPreferredSize(area);
            this.revalidate();
        }
    }

    /**
     * Method paints grid to the draw panel. It should be called first while
     * painting. If the grid is not used, it does nothing.
     *
     * @param g Graphics object, where to paint
     */
    private void paintGrid(Graphics g) {
        if (!useGrid)
            return;
        g.setColor(gridColor);
        for (int xi = 0; xi < this.getWidth(); xi+=gridGap)
            g.drawLine(xi, 0, xi, this.getHeight());
        for (int yi = 0; yi < this.getHeight(); yi+= gridGap)
            g.drawLine(0, yi, this.getWidth(), yi);
    }

    /**
     * Draw the schema to the graphics object. It overrides original panel paint
     * method.
     *
     * @param g Graphics object where to paint
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ArrayList<Element> a = schema.getAllElements();
        
        // najprv mriezka
        paintGrid(g);

        // at first, measure objects
        for (int i = 0; i < a.size(); i++)
            a.get(i).measure(g,0,0);

        // then draw connection lines (at the bottom)
        for (int i = 0; i < schema.getConnectionLines().size(); i++)
            schema.getConnectionLines().get(i).draw((Graphics2D)g);

        // at least, draw all other elements
        for (int i = 0; i < a.size(); i++)
            a.get(i).draw(g);

        // ***** HERE BEGINS DRAWING OF TEMPORARY GRAPHICS *****

        if (panelMode == PanelMode.move) {
            // draw a small red circle around selected connection line point
            if (selPoint != null) {
                int xx = (int)selPoint.getX();
                int yy = (int)selPoint.getY();
                g.setColor(Color.red);
                ((Graphics2D)g).setStroke(thickLine);
                g.drawOval(xx-4, yy-4, 8, 8);
            }
        } else if (panelMode == PanelMode.draw) {
            // if the connection line is being drawn, draw the sketch
            if (drawTool == PanelDrawTool.connectLine && tmpElem1 != null) {
                ConnectionLine.drawSketch((Graphics2D)g, tmpElem1,
                        sketchLastPoint, tmpPoints);
            }
        } else if (panelMode == PanelMode.select) {
            // TODO
        }
    }

    /**
     * Set a draw tool.
     *
     * It first clear all "tasks" - clear
     * temporary line points, selection and stop drag-n-drop.
     *
     * If the new draw tool is null, it then sets the panel
     * mode to "move" mode.
     *
     * If the tool is not null, the "draw" mode is activated.
     * The text is used only when the draw tool is some element - cpu, memory
     * or device. It is not used if the other draw tool is selected.
     *
     * @param tool panel draw tool
     * @param text text of the element
     */
    public void setTool(PanelDrawTool tool, String text) {
        this.drawTool = tool;
        this.newText = text;

        cancelTasks();

        if ((tool == null) || (tool == PanelDrawTool.nothing))
            panelMode = PanelMode.move;
        else
            panelMode = PanelMode.draw;
    }

    /**
     * Cancel all pending operations, like selection, or line drawing. It then
     * repaints the schema.
     */
    public void cancelTasks() {
        tmpElem1 = null;
        tmpElem2 = null;
        tmpPoints.clear();
        repaint();
    }


    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e){}
    @Override
    public void mouseExited(MouseEvent e){}

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        if (panelMode == PanelMode.move) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                tmpElem1 = schema.getCrossingElement(p);
                if (tmpElem1 != null) {
                    selLine = null;
                    selPoint = null;
                    return;
                }
            }

            // add/remove a point to/from line, or start point drag-n-drop
            // if the user is near a connection line
            selPoint = null;
            selLine = null;
            selLine = schema.getCrossingLine(p);

            if (selLine != null) {
                int pi = selLine.getCrossPointAfter(p); // should not be -1
                p.setLocation(searchGridPoint(p));
                Point linePoint = selLine.containsPoint(p);
                if (linePoint == null) {
                    selLine.addPoint(pi - 1, p);
                    selPoint = p;
                } else
                    selPoint = linePoint;
            }
            repaint(); // because of drawing selected point

            // if user press a mouse button on empty area, activate "selection"
            // mode
            if (selPoint == null) {
                panelMode = PanelMode.select;
                selectionStart = e.getPoint(); // point without grid correction
            }
        } else if (panelMode == PanelMode.draw) {
            if (drawTool == PanelDrawTool.connectLine) {
                if (e.getButton() != MouseEvent.BUTTON1)
                    return;

                Element elem = schema.getCrossingElement(e.getPoint());
                if (elem != null) {
                    if (tmpElem1 == null)
                        tmpElem1 = elem;
                    else if (tmpElem2 == null)
                        tmpElem2 = elem;
                    return;
                } else {
                    // if user didn't clicked on an element, but on drawing area
                    // means that there a new line point should be created.
                    p.setLocation(searchGridPoint(p));
                    selPoint = p;
                }
            } else if (drawTool == PanelDrawTool.delete) {
                // only left button is accepted
                if (e.getButton() != MouseEvent.BUTTON1)
                    return;

                Element elem = schema.getCrossingElement(e.getPoint());
                tmpElem1 = elem;

                // delete line?
                if (elem == null)
                    selLine = schema.getCrossingLine(p);
            }
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        p.setLocation(searchGridPoint(p));
        if (panelMode == PanelMode.move) {
            // if a point is selected, remove it if user pressed
            // right mouse button
            if (selLine != null && selPoint != null) {
                if (e.getButton() != MouseEvent.BUTTON3)
                    return;
                Point linePoint = selLine.containsPoint(p);
                if ((selLine != schema.getCrossingLine(e.getPoint()))
                        || (selPoint != linePoint)) {
                    selLine = null;
                    selPoint = null;
                    return;
                }
                selLine.removePoint(selPoint);
                selPoint = null;
                selLine = null;
            }
        } else if (panelMode == PanelMode.select) {
            selectionEnd = e.getPoint();
            panelMode = PanelMode.move;

            // TODO: Select elements contained in the selection area
        } else if (panelMode == PanelMode.draw) {
            if (drawTool == PanelDrawTool.delete) {
                if (tmpElem1 != null) {
                    if (e.getButton() != MouseEvent.BUTTON1)
                        return;
                    // if the mouse is released upon a point outside a tmpElem1
                    // nothing is done.
                    if (tmpElem1 != schema.getCrossingElement(e.getPoint())) {
                        tmpElem1 = null;
                        return;
                    }
                    schema.removeElement(tmpElem1);
                    tmpElem1 = null;
                } else if((tmpElem1 == null) && (selLine != null)) {
                    // if the mouse is released upon a point outside the selLine
                    // nothing is done.
                    if (selLine != schema.getCrossingLine(e.getPoint())) {
                        selLine = null;
                        return;
                    }
                    schema.removeConnectionLine(selLine);
                    selLine = null;
                }
            } else if (drawTool == PanelDrawTool.shapeCompiler)
                schema.setCompilerElement(new CompilerElement(p, newText));
            else if(drawTool == PanelDrawTool.shapeCPU)
                schema.setCpuElement(new CpuElement(p, newText));
            else if (drawTool == PanelDrawTool.shapeMemory)
                schema.setMemoryElement(new MemoryElement(p, newText));
            else if (drawTool == PanelDrawTool.shapeDevice)
                schema.addDeviceElement(new DeviceElement(p, newText));
            else if (drawTool == PanelDrawTool.connectLine) {
                sketchLastPoint = null;
                Element elem = schema.getCrossingElement(e.getPoint());

                if (elem != null) {
                    if ((tmpElem2 == null) && (tmpElem1 != elem)) {
                        tmpElem1 = null;
                        return;
                    } else if ((tmpElem2 != null) && tmpElem2 != elem) {
                        tmpElem1 = null;
                        tmpElem2 = null;
                    }
                } else {
                    if ((tmpElem1 != null) && (selPoint != null)) {
                        tmpPoints.add(selPoint);
                        selPoint = null;
                    }
                }
                if ((tmpElem1 != null) && (tmpElem2 != null)) {
                    // kontrola ci nahodou uz spojenie neexistuje
                    // resp. ci nie je spojenie sam so sebou
                    boolean b = false;
                    for (int i = 0; i < schema.getConnectionLines().size(); i++) {
                        ConnectionLine l = schema.getConnectionLines().get(i);
                        if (l.containsElement(tmpElem1)
                                && l.containsElement(tmpElem2)) {
                            b = true;
                            break;
                        }
                    }
                    if (!b && (tmpElem1 != tmpElem2)) {
                        schema.getConnectionLines().add(new ConnectionLine(tmpElem1,
                                tmpElem2, tmpPoints));
                    }
                    tmpElem1 = null;
                    tmpElem2 = null;
                    tmpPoints.clear();
                }
            }
        }
        panelSizeCorrection();
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        if ((panelMode == PanelMode.draw)
                && (drawTool == PanelDrawTool.connectLine)) {
            if (schema.getCrossingElement(p) == null) {
                // if user didn't clicked on an element, but on drawing area
                // means that there a new line point should be created.
                p.setLocation(searchGridPoint(p));
                selPoint = p;
            }
        } else if (panelMode == PanelMode.move) {
            if ((selPoint != null) && (selLine != null)) {
                if (p.getX() < 0 || p.getY() < 0)
                    return;
                p.setLocation(searchGridPoint(p));
                selLine.pointMove(selPoint, p);
            } else if (tmpElem1 != null) {
                if (p.getX() < 0 || p.getY() < 0)
                    return;
                p.setLocation(searchGridPoint(p));
                tmpElem1.move(p);
            }
        } else if (panelMode == PanelMode.select) {
            selectionEnd = e.getPoint();
            // TODO: draw selection sketch
        }
        panelSizeCorrection();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (panelMode == PanelMode.move) {
            selPoint = null;
            if (selLine != null)  // ???
                repaint();
            selLine = null;

            for (int i = schema.getConnectionLines().size()-1; i >= 0 ; i--) {
                Point[]ps = schema.getConnectionLines().get(i).getPoints().toArray(new Point[0]);
                Point p = e.getPoint();
                boolean out = false;
                for (int j = 0; j < ps.length; j++) {
                    double d = Math.hypot(ps[j].getX() - p.getX(), 
                            ps[j].getY() - p.getY());
                    if (d < 8) {
                        selLine = schema.getConnectionLines().get(i);
                        selPoint  = ps[j];
                        repaint();
                        out = true;
                        break;
                    } 
                }
                if (out)
                    break;
            }
        } else if (panelMode == PanelMode.draw)
            if (drawTool == PanelDrawTool.connectLine && tmpElem1 != null) {
                sketchLastPoint = e.getPoint();
                repaint();
            }
    }

}
