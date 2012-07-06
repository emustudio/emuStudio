/*
 * DrawingPanel.java
 *
 * Created on 3.7.2008, 8:31:58
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2008-2012, Peter Jakubčo
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

import emustudio.gui.ElementPropertiesDialog;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

/**
 * This class handles the drawing canvas - panel by which the user can modelling
 * abstract schemas of virtual computers.
 *
 * The drawing is realized by capturing mouse events (motion, clicks).
 * The "picture" is synchronized with the Schema object automatically.
 *
 * The panel has states, or modes. The initial mode is "moving" mode.
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DrawingPanel extends JPanel implements MouseListener,
        MouseMotionListener {
    /**
     * Default grid gap
     */
    public final static Integer DEFAULT_GRID_GAP = 20;
    
    private final static int RESIZE_TOP = 0;
    private final static int RESIZE_LEFT = 1;
    private final static int RESIZE_BOTTOM = 2;
    private final static int RESIZE_RIGHT = 3;
    
    /**
     * List of event listeners
     */
    private EventListenerList eventListeners;

    /**
     * Color of the grid
     */
    private Color gridColor;

    /**
     * A modelling tool used by this panel in the time. 
     */
    private DrawTool drawTool;

    /**
     * Mode of the panel. One of the modelling, moving or selecting.
     */
    private PanelMode panelMode;

    /**
     * This variable is used when "moving" mode is active and user moves
     * an element. It holds the moving element object.
     *
     * If "modelling" mode is active and when users draws a line, it represents the
     * first element that the line is connected to. If it is selected the element
     * deletion, it represents a shape that should be deleted when mouse is
     * released.
     */
    private Element tmpElem1;

    /**
     * If an element is selected (mouse pressed) and then dragged, this
     * variable holds true. It is false in all other cases.
     *
     * When the mouse is released, the value is tested. If it is true, it means
     * that the element has been moved and therefore possible selection of
     * the other elements should not be cleared.
     *
     */
    private boolean elementDragged;
    
    /**
     * Resize mode contains a value of one of constants: RESIZE_TOP, RESIZE_BOTTOM,
     * RESIZE_LEFT, RESIZE_RIGHT. It is used in "resizing" mode.
     */
    private int resizeMode;

    /**
     * Used when drawing lines. It represents last element that the line
     * is connected to.
     */
    private Element tmpElem2;

    /**
     * Selected line. Used only in "moving" mode.
     *
     * This variable is used if the user wants to remove or moving an existing
     * connection line point.
     */
    private ConnectionLine selLine;

    /**
     * Holds a point of a connection line.
     *
     * This is used in "moving" mode for:
     *   - moving of the connection line point
     *   - add/delete connection line point
     *
     * in the "modelling" mode, it is used for:
     *   - holds temporal point that will be added to temporal points array
     *     when mouse is released, while drawing a line
     */
    private Point selPoint;

    /**
     * This variable contains last sketch point when drawing a connection line.
     * The last point is variable according to the mouse position. It actually
     * is the mouse position when drawing a line.
     *
     * It is used only in "modelling" mode.
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

    private BasicStroke dashedLine;

    private BasicStroke dottedLine;

    private Schema schema;

    /**
     * Temporary points used in the process of connection line drawing.
     * If the line is drawn, these points are saved, they are cleared otherwise.
     */
    private List<Point> tmpPoints;
    
    private String newPluginName;

    /* double buffering */
    private Image dbImage;   // second buffer
    private Graphics2D dbg;  // graphics for double buffering

    /**
     * Future connection line direction. Holds true, if the drawing line
     * should be bidirectional, false otherwise.
     */
    private boolean bidirectional;
    
    private JDialog parent; // for modal showing of pop-up menu Properties dialog

    /**
     * Interface that should be implemented by an event listener.
     */
    public interface DrawEventListener extends EventListener {

        /**
         * This method is called whenever the user uses any of the
         * tools available within this DrawingPanel.
         *
         * The schema editor then can "turn off" the tool.
         */
        public void toolUsed();
    }

    /**
     * Draw tool enum.
     */
    public enum DrawTool {
        /**
         * Compiler drawing tool
         */
        shapeCompiler,

        /**
         * CPU drawing tool
         */
        shapeCPU,

        /**
         * Memory drawing tool
         */
        shapeMemory,

        /**
         * Device drawing tool
         */
        shapeDevice,

        /**
         * Connection line drawing tool
         */
        connectLine,

        /**
         * The removal tool
         */
        delete,

        /**
         * No tool, do nothing
         */
        nothing
    }

    /**
     * Panel mode enum.
     */
    public enum PanelMode {

        /**
         * Modelling mode. New components are being added or deleted, according
         * to selected tool. When mouse clicks on the canvas, the component is
         * created/deleted and mode is switched into "moving" mode.
         * 
         * If a user is creating a line, by clicking on an empty area a line point
         * is created and user continues creating the line - ie. the "modelling" mode
         * stays.
         */
        modelling,

        /**
         * Moving mode, initial. If no objects are selected, mouse is just
         * moving - if the mouse hovers over components or line points, they are
         * highlighted.
         * 
         * If a user presses left mouse button over existing selection, selected
         * components are going to be moved by following mouse moves. The mouse
         * releasement finishes the movement.
         * 
         * If a user presses left mouse button over a line point, it is going to
         * be moved by following mouse moves. The mouse releasement finishes
         * the movement.
         * 
         * If a user presses left mouse button over a line (not on a line point),
         * new line point is created on this location and it is immediately
         * selected for movement.
         * 
         * If a user presses left mouse button over empty area, the mode is
         * switched to "selection" mode.
         */
        moving,
        
        /**
         * Resizing mode. In this mode, user resizes elements.
         */
        resizing,

        /**
         * Selecting mode. By mouse movement more/less components are added
         * into a selection. The mouse releasement switches into "moving" mode.
         */
        selecting
    }

    /**
     * Creates new instance of the modelling panel.
     *
     * @param schema  Schema object for the panel synchronization
     * @param parent  Parent dialog
     */
    public DrawingPanel(Schema schema, JDialog parent) {
        this.parent = parent;
        this.setBackground(Color.WHITE);
        this.schema = schema;

        panelMode = PanelMode.moving;
        drawTool = DrawTool.nothing;

        float dash[] = { 10.0f };
        float dot[] = { 1.0f };
        dashedLine = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
        dottedLine = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 1.0f, dot, 0.0f);

        tmpPoints = new ArrayList<Point>();
        gridColor = new Color(0xDDDDDD);

        eventListeners = new EventListenerList();
        bidirectional = true;
        elementDragged = false;
    }

    /**
     * Adds a DrawEventListener object onto the list of listeners.
     *
     * @param listener listener object
     */
    public void addEventListener(DrawEventListener listener) {
        eventListeners.add(DrawEventListener.class, listener);
    }

    /**
     * Remove DrawEventListener object from the list of listeners.
     *
     * @param listener listener object to remove
     */
    public void removeEventListener(DrawEventListener listener) {
        eventListeners.remove(DrawEventListener.class, listener);
    }

    /**
     * Fires the toolUsed() method on all listeners on listeners list
     */
    private void fireListeners() {
        Object[] listenersList = eventListeners.getListenerList();
        for (int i = listenersList.length-2; i>=0; i-=2) {
            if (listenersList[i]==DrawEventListener.class)
                ((DrawEventListener)listenersList[i+1]).toolUsed();
        }
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
        boolean useGrid = schema.getUseGrid();
        int gridGap = schema.getGridGap();
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
        schema.setUseGrid(useGrid);
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

        // modelling elements in background
        dbg.setColor(getForeground());
        paint(dbg);

        // modelling image on the screen
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
        List<Element> a = schema.getAllElements();
        for (int i = 0; i < a.size(); i++) {
            Element e = a.get(i);
            if (e.getX() + e.getWidth() > area.width)
                area.width = e.getX() + e.getWidth();
            if (e.getY() + e.getHeight() > area.height)
                area.height = e.getY() + e.getHeight();
        }
        for (int i = 0; i < schema.getConnectionLines().size(); i++) {
            List<Point> ps = schema.getConnectionLines().get(i).getPoints();
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
     * Method paints grid to the modelling panel. It should be called first while
     * painting. If the grid is not used, it does nothing.
     *
     * @param g Graphics object, where to paint
     */
    private void paintGrid(Graphics g) {
        boolean useGrid = schema.getUseGrid();
        int gridGap = schema.getGridGap();
        if (!useGrid)
            return;
        g.setColor(gridColor);
        ((Graphics2D)g).setStroke(dottedLine);
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
        List<Element> a = schema.getAllElements();
        
        // najprv mriezka
        paintGrid(g);

        // at first, measure objects
        for (int i = 0; i < a.size(); i++)
            a.get(i).measure(g,0,0);

        // then modelling connection lines (at the bottom)
        for (int i = 0; i < schema.getConnectionLines().size(); i++) {
            ConnectionLine l = schema.getConnectionLines().get(i);
            l.computeArrows(0,0);
            l.draw((Graphics2D)g,false);
        }

        // at least, modelling all other elements
        for (int i = 0; i < a.size(); i++)
            a.get(i).draw(g);

        // ***** HERE BEGINS DRAWING OF TEMPORARY GRAPHICS *****

        if (panelMode == PanelMode.moving) {
            // modelling a small red circle around selected connection line point
            if (selPoint != null)
                ConnectionLine.highlightPoint(selPoint, (Graphics2D)g);
        } else if (panelMode == PanelMode.modelling) {
            // if the connection line is being drawn, modelling the sketch
            if (drawTool == DrawTool.connectLine && tmpElem1 != null) {
                ConnectionLine.drawSketch((Graphics2D)g, tmpElem1,
                        sketchLastPoint, tmpPoints);
            }
        } else if (panelMode == PanelMode.selecting) {
            if ((selectionStart != null) && (selectionEnd != null)) {
                g.setColor(Color.BLUE);
                ((Graphics2D)g).setStroke(dashedLine);

                int x = selectionStart.x;
                int y = selectionStart.y;

                if (selectionEnd.x < x)
                    x = selectionEnd.x;
                if (selectionEnd.y < y)
                    y = selectionEnd.y;
                int w = selectionEnd.x - selectionStart.x;
                int h = selectionEnd.y - selectionStart.y;

                if (w < 0)
                    w = -w;
                if (h < 0)
                    h = -h;
                g.drawRect(x, y, w, h);
            }
        }
    }    

    /**
     * Set a modelling tool.
     *
     * It first clear all "tasks" - clear
     * temporary line points, selection and stop drag-n-drop.
     *
     * If the new modelling tool is null, it then sets the panel
     * mode to "moving" mode.
     *
     * If the tool is not null, the "modelling" mode is activated.
     * The text is used only when the modelling tool is some element - cpu, memory
     * or device. It is not used if the other modelling tool is selected.
     *
     * @param tool panel modelling tool
     * @param text text of the element
     */
    public void setTool(DrawTool tool, String text) {
        this.drawTool = tool;
        this.newPluginName = text;

        cancelTasks();

        if ((tool == null) || (tool == DrawTool.nothing))
            panelMode = PanelMode.moving;
        else
            panelMode = PanelMode.modelling;
    }

    /**
     * Set future connection line direction.
     * @param bidirectional if it is true, drawing line will be bidirectional,
     * if it is false, it will be single-direction oriented.
     */
    public void setFutureLineDirection(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }

    /**
     * Cancel all pending operations, like selection, or line drawing. It then
     * repaints the schema.
     */
    public void cancelTasks() {
        tmpElem1 = null;
        tmpElem2 = null;
        tmpPoints.clear();
        selectionStart = null;
        selectionEnd = null;
        selPoint = null;
        selLine = null;
        repaint();
    }
    
    private void doPop(MouseEvent e, Element elem) {
        ElementPopUpMenu menu = new ElementPopUpMenu(elem,parent);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }


    /**
     * Invoked when user clicks on the panel.
     * 
     * @param e  a mouse event
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if ((panelMode == PanelMode.moving) && (tmpElem1 != null)) {
            if (e.getClickCount() == 2) {
                new ElementPropertiesDialog(parent,tmpElem1).setVisible(true);
                this.repaint();
            }
        }
    }
    
    /**
     * Not implemented.
     * @param e  a mouse event
     */
    @Override
    public void mouseEntered(MouseEvent e){}
    
    /**
     * Not implemented.
     * @param e  a mouse event
     */
    @Override
    public void mouseExited(MouseEvent e){}

    /**
     * When user presses a buton over the panel.
     * @param e  a mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        if (panelMode == PanelMode.moving) {
            if (e.isPopupTrigger()) {
                tmpElem1 = schema.getCrossingElement(p);
                if (tmpElem1 != null)
                    doPop(e, tmpElem1);
                tmpElem1 = null;
            }
            if (e.getButton() == MouseEvent.BUTTON1) {
                // detect if user wants to resize an element
                tmpElem1 = schema.getResizeElement(p);
                if (tmpElem1 != null) {
                    panelMode = PanelMode.resizing;
                    if (tmpElem1.isBottomCrossing(p)) {
                        resizeMode = RESIZE_BOTTOM;
                    } else if (tmpElem1.isLeftCrossing(p)) {
                        resizeMode = RESIZE_LEFT;
                    } else if (tmpElem1.isRightCrossing(p)) {
                        resizeMode = RESIZE_RIGHT;
                    } else if (tmpElem1.isTopCrossing(p)) {
                        resizeMode = RESIZE_TOP;
                    } else {
                        resizeMode = -1; // TODO - corners
                    }
                    return;
                }
                tmpElem1 = schema.getCrossingElement(p);
                if (tmpElem1 != null) {
                    selLine = null;
                    elementDragged = false;
                    return;
                }
            }

            // add/remove a point to/from line, or start point drag-n-drop
            // if the user is near a connection line
            selPoint = null;
            selLine = null;
            selLine = schema.getCrossingLine(p);

            if (selLine != null) {
                selPoint = selLine.containsPoint(p);
            }
            repaint(); // because of drawing selected point

            // if user press a mouse button on empty area, activate "selection"
            // mode
            if (selLine == null) {
                panelMode = PanelMode.selecting;
                selectionStart = e.getPoint(); // point without grid correction
            }
        } else if (panelMode == PanelMode.modelling) {
            if (drawTool == DrawTool.connectLine) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                Element elem = schema.getCrossingElement(e.getPoint());
                if (elem != null) {
                    if (tmpElem1 == null) {
                        tmpElem1 = elem;
                    } else if (tmpElem2 == null) {
                        tmpElem2 = elem;
                    }
                } else {
                    // if user didn't clicked on an element, but on drawing area
                    // means that there a new line point should be created.
                    p.setLocation(searchGridPoint(p));
                    selPoint = p;
                }
            } else if (drawTool == DrawTool.delete) {
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
    
    /**
     * When user releases the button on the panel.
     * @param e  a mouse event
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        if (panelMode == PanelMode.resizing) {
            panelMode = PanelMode.moving;
            resizeMode = -1;
            return;
        }
        if (panelMode == PanelMode.moving) {
            if (e.isPopupTrigger()) {
                tmpElem1 = schema.getCrossingElement(p);
                if (tmpElem1 != null)
                    doPop(e, tmpElem1);
                tmpElem1 = null;
            }
            // if an element was clicked, selecting it
            // if user holds CTRL or SHIFT
            if (e.getButton() == MouseEvent.BUTTON1) {
                int ctrl_shift = e.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK
                        | MouseEvent.CTRL_DOWN_MASK);
                if ((!elementDragged) && (ctrl_shift == 0))
                    schema.selectElements(-1, -1, 0, 0);
                Element elem = schema.getCrossingElement(p);
                if ((tmpElem1 == elem) && (elem != null)) {
                    elem.setSelected(true);
                    repaint();
                    return;
                }
                if ((selLine == null) || (selLine != schema.getCrossingLine(p)))
                    return;
                selLine.setSelected(true);
            }

            // if a point is selected, remove it if user pressed
            // right mouse button
            if (selLine != null && selPoint != null) {
                if (e.getButton() != MouseEvent.BUTTON3)
                    return;
                Point linePoint = selLine.containsPoint(p);
                if ((selLine != schema.getCrossingLine(p))
                        || (selPoint != linePoint)) {
                    selLine = null;
                    selPoint = null;
                    return;
                }
                selLine.removePoint(selPoint);
                selPoint = null;
                selLine = null;
            }
        } else if (panelMode == PanelMode.selecting) {
            panelMode = PanelMode.moving;

            int x = selectionStart.x;
            int y = selectionStart.y;

            if (selectionEnd == null)
                selectionEnd = p;

            if (selectionEnd.x < x)
                x = selectionEnd.x;
            if (selectionEnd.y < y)
                y = selectionEnd.y;
            int w = selectionEnd.x - selectionStart.x;
            int h = selectionEnd.y - selectionStart.y;

            if (w < 0)
                w = -w;
            if (h < 0)
                h = -h;

            schema.selectElements(x,y,w,h);

            selectionStart = null;
            selectionEnd = null;
        } else if (panelMode == PanelMode.modelling) {
            if (drawTool == DrawTool.delete) {
                if (tmpElem1 != null) {
                    if (e.getButton() != MouseEvent.BUTTON1)
                        return;
                    // if the mouse is released upon a point outside a tmpElem1
                    // nothing is done.
                    if (tmpElem1 != schema.getCrossingElement(p)) {
                        tmpElem1 = null;
                        return;
                    }
                    schema.removeElement(tmpElem1);
                    tmpElem1 = null;
                    fireListeners();
                } else if((tmpElem1 == null) && (selLine != null)) {
                    // if the mouse is released upon a point outside the selLine
                    // nothing is done.
                    if (selLine != schema.getCrossingLine(p)) {
                        selLine = null;
                        return;
                    }
                    schema.removeConnectionLine(selLine);
                    selLine = null;
                    fireListeners();
                }
            } else if (drawTool == DrawTool.shapeCompiler) {
                p.setLocation(searchGridPoint(p));
                schema.setCompilerElement(new CompilerElement(newPluginName,p));
                fireListeners();
            } else if(drawTool == DrawTool.shapeCPU) {
                p.setLocation(searchGridPoint(p));
                schema.setCpuElement(new CpuElement(newPluginName,p));
                fireListeners();
            } else if (drawTool == DrawTool.shapeMemory) {
                p.setLocation(searchGridPoint(p));
                schema.setMemoryElement(new MemoryElement(newPluginName,p));
                fireListeners();
            } else if (drawTool == DrawTool.shapeDevice) {
                p.setLocation(searchGridPoint(p));
                schema.addDeviceElement(new DeviceElement(newPluginName,p));
                fireListeners();
            } else if (drawTool == DrawTool.connectLine) {
                sketchLastPoint = null;
                Element elem = schema.getCrossingElement(p);

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
                        return;
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
                        ConnectionLine l = new ConnectionLine(tmpElem1,
                                tmpElem2, tmpPoints);
                        l.setBidirectional(bidirectional);
                        schema.getConnectionLines().add(l);
                    }
                    tmpElem1 = null;
                    tmpElem2 = null;
                    tmpPoints.clear();
                    fireListeners();
                }
            }
        }
        panelSizeCorrection();
        repaint();
    }

    /**
     * When user drags the mouse through the panel
     * @param e  a mouse event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        if (panelMode == PanelMode.resizing) {
            if (tmpElem1 == null)
                return;
            p.setLocation(searchGridPoint(p));
            switch (resizeMode) {
                case RESIZE_TOP:
                    tmpElem1.setSize(tmpElem1.getWidth(), 
                            (tmpElem1.getY() - p.y)*2);
                    break;
                case RESIZE_BOTTOM:
                    tmpElem1.setSize(tmpElem1.getWidth(), 
                            (p.y - tmpElem1.getY())*2);
                    break;
                case RESIZE_LEFT:
                    tmpElem1.setSize((tmpElem1.getX() - p.x) * 2, 
                            tmpElem1.getHeight());
                    break;
                case RESIZE_RIGHT:
                    tmpElem1.setSize((p.x - tmpElem1.getX()) * 2, 
                            tmpElem1.getHeight());
                    break;
            }
        } else if ((panelMode == PanelMode.modelling)
                && (drawTool == DrawTool.connectLine)) {
            if (schema.getCrossingElement(p) == null) {
                // if user didn't clicked on an element, but on drawing area
                // means that there a new line point should be created.
                p.setLocation(searchGridPoint(p));
                selPoint = p;
            }
        } else if (panelMode == PanelMode.moving) {
            if (selLine != null) {
                if (p.getX() < 0 || p.getY() < 0)
                    return;
                if (selPoint == null) {
                    int pi = selLine.getCrossPointAfter(p,
                            ConnectionLine.TOLERANCE); // should not be -1
                    if (pi == -1)
                        return;
                    p.setLocation(searchGridPoint(p));
                    Point linePoint = selLine.containsPoint(p);
                    if (linePoint == null) {
                        selLine.addPoint(pi - 1, p);
                        selPoint = p;
                    } else if (selPoint != linePoint)
                        return;
                }
                p.setLocation(searchGridPoint(p));
                selLine.pointMove(selPoint, p);
            } else if (tmpElem1 != null) {
                if (p.getX() < 0 || p.getY() < 0)
                    return;
                p.setLocation(searchGridPoint(p));

                elementDragged = true;
                // if the element is selected, we must moving all selected elements
                // either.
                if (tmpElem1.selected) {
                    int diffX, diffY;
                    diffX = p.x - tmpElem1.getX();
                    diffY = p.y - tmpElem1.getY();
                    schema.moveSelected(diffX, diffY);
                } else
                    tmpElem1.move(p);
            }
        } else if (panelMode == PanelMode.selecting)
            selectionEnd = e.getPoint();
        panelSizeCorrection();
        repaint();
    }

    /**
     * When a user moves the mouse over the panel
     * @param e  a mouse event
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (panelMode == PanelMode.moving) {
            selPoint = null;
            if (selLine != null) // if a line point was highlighted this will "de-highlight" it
                repaint();
            selLine = null;
            
            // resize mouse pointers
            if (drawTool == DrawTool.nothing) {
                Point p = e.getPoint();
                tmpElem1 = schema.getResizeElement(p);
                if (tmpElem1 != null) {
                    if (tmpElem1.isBottomCrossing(p)) {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    } else if (tmpElem1.isLeftCrossing(p)) {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    } else if (tmpElem1.isRightCrossing(p)) {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    } else if (tmpElem1.isTopCrossing(p)) {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    } else {
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                } else {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
            
            // highlight line points
            for (int i = schema.getConnectionLines().size()-1; i >= 0 ; i--) {
                Point[]ps = schema.getConnectionLines().get(i).getPoints().toArray(new Point[0]);
                Point p = e.getPoint();
                boolean out = false;
                for (int j = 0; j < ps.length; j++) {
                    if (ConnectionLine.isPointSelected(ps[j], p)) {
                        selLine = schema.getConnectionLines().get(i);
                        selPoint  = ps[j];
                        out = true;
                        break;
                    } 
                }
                if (out) {
                    repaint();
                    break;
                }
            }
        } else if (panelMode == PanelMode.modelling)
            if (drawTool == DrawTool.connectLine && tmpElem1 != null) {
                sketchLastPoint = e.getPoint();
                repaint();
            }
    }

}
