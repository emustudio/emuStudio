/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package emustudio.drawing.mode;

import emustudio.drawing.CompilerElement;
import emustudio.drawing.ConnectionLine;
import emustudio.drawing.CpuElement;
import emustudio.drawing.DeviceElement;
import emustudio.drawing.DrawingPanel;
import emustudio.drawing.DrawingPanel.Tool;
import emustudio.drawing.Element;
import emustudio.drawing.MemoryElement;
import emustudio.drawing.Model;
import emustudio.drawing.mode.ModeSelector.SelectMode;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Modeling drawing mode. New components are being added or deleted, according to selected tool. When mouse clicks on
 * the canvas, the component is created/deleted and mode is switched into "moving" mode.
 *
 * If a user is creating a line, by clicking on an empty area a line point is created and user continues creating the
 * line - ie. the "modelling" mode stays.
 */
class ModelingMode extends AbstractMode {

    /**
     * This variable contains last sketch point when drawing a connection line. The last point is variable according to
     * the mouse position. It actually is the mouse position when drawing a line.
     *
     * It is used only in "modelling" mode.
     */
    private Point sketchLastPoint;

    ModelingMode(DrawingPanel panel, Model model) {
        super(panel, model);
    }

    @Override
    public void drawTemporaryGraphics(Graphics2D graphics) {
        // if the connection line is being drawn, modelling the sketch
        if (model.drawTool == Tool.connection && model.tmpElem1 != null) {
            ConnectionLine.drawSketch(graphics, model.tmpElem1, sketchLastPoint, model.tmpPoints);
        }
    }

    @Override
    public SelectMode mouseClicked(MouseEvent e) {
        return SelectMode.MODELING;
    }

    @Override
    public SelectMode mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        if (model.drawTool == Tool.connection) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return SelectMode.MODELING;
            }

            Element elem = schema.getCrossingElement(e.getPoint());
            if (elem != null) {
                if (model.tmpElem1 == null) {
                    model.tmpElem1 = elem;
                } else if (model.tmpElem2 == null) {
                    model.tmpElem2 = elem;
                }
            } else {
                if (model.tmpElem1 == null) {
                    panel.fireToolWasUsed();
                    return SelectMode.MOVING;
                }
                // if user didn't clicked on an element, but on drawing area
                // means that there a new line point should be created.
                p.setLocation(searchGridPoint(p));
                model.selPoint = p;
            }
        } else if (model.drawTool == Tool.delete) {
            // only left button is accepted
            if (e.getButton() != MouseEvent.BUTTON1) {
                return SelectMode.MODELING;
            }

            Element elem = schema.getCrossingElement(e.getPoint());
            model.tmpElem1 = elem;

            // delete line?
            if (elem == null) {
                model.selLine = schema.getCrossingLine(p);
            }
        }
        return SelectMode.MODELING;
    }

    @Override
    public SelectMode mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        if (model.drawTool == Tool.delete) {
            if (model.tmpElem1 != null) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return SelectMode.MODELING;
                }
                // if the mouse is released upon a point outside a tmpElem1, nothing is done.
                if (model.tmpElem1 != schema.getCrossingElement(p)) {
                    model.tmpElem1 = null;
                    return SelectMode.MODELING;
                }
                schema.removeElement(model.tmpElem1);
                model.tmpElem1 = null;
                panel.fireToolWasUsed();
            } else if (model.selLine != null) {
                // if the mouse is released upon a point outside the selLine, nothing is done.
                if (model.selLine != schema.getCrossingLine(p)) {
                    model.selLine = null;
                    return SelectMode.MODELING;
                }
                schema.removeConnectionLine(model.selLine);
                model.selLine = null;
                panel.fireToolWasUsed();
            }
        } else if (model.drawTool == Tool.compiler) {
            p.setLocation(searchGridPoint(p));
            schema.setCompilerElement(new CompilerElement(model.newPluginName, p, schema));
            panel.fireToolWasUsed();
        } else if (model.drawTool == Tool.CPU) {
            p.setLocation(searchGridPoint(p));
            schema.setCpuElement(new CpuElement(model.newPluginName, p, schema));
            panel.fireToolWasUsed();
        } else if (model.drawTool == Tool.memory) {
            p.setLocation(searchGridPoint(p));
            schema.setMemoryElement(new MemoryElement(model.newPluginName, p, schema));
            panel.fireToolWasUsed();
        } else if (model.drawTool == Tool.device) {
            p.setLocation(searchGridPoint(p));
            schema.addDeviceElement(new DeviceElement(model.newPluginName, p, schema));
            panel.fireToolWasUsed();
        } else if (model.drawTool == Tool.connection) {
            sketchLastPoint = null;
            Element elem = schema.getCrossingElement(p);

            if (elem != null) {
                if ((model.tmpElem2 == null) && (model.tmpElem1 != elem)) {
                    model.tmpElem1 = null;
                    return SelectMode.MODELING;
                } else if ((model.tmpElem2 != null) && model.tmpElem2 != elem) {
                    model.tmpElem1 = null;
                    model.tmpElem2 = null;
                }
            } else {
                if ((model.tmpElem1 != null) && (model.selPoint != null)) {
                    model.tmpPoints.add(model.selPoint);
                    model.selPoint = null;
                    return SelectMode.MODELING;
                }
            }
            if ((model.tmpElem1 != null) && (model.tmpElem2 != null)) {
                // kontrola ci nahodou uz spojenie neexistuje, resp. ci nie je spojenie sam so sebou
                boolean b = false;
                for (int i = 0; i < schema.getConnectionLines().size(); i++) {
                    ConnectionLine l = schema.getConnectionLines().get(i);
                    if (l.containsElement(model.tmpElem1) && l.containsElement(model.tmpElem2)) {
                        b = true;
                        break;
                    }
                }
                if (!b && (model.tmpElem1 != model.tmpElem2)) {
                    ConnectionLine l = new ConnectionLine(model.tmpElem1, model.tmpElem2, model.tmpPoints, schema);
                    l.setBidirectional(model.bidirectional);
                    schema.getConnectionLines().add(l);
                }
                model.tmpElem1 = null;
                model.tmpElem2 = null;
                model.tmpPoints.clear();
                panel.fireToolWasUsed();
                return SelectMode.MOVING;
            }
            return SelectMode.MODELING;
        }
        panel.fireToolWasUsed();
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        if (model.drawTool == Tool.connection) {
            if (schema.getCrossingElement(p) == null) {
                // if user didn't clicked on an element, but on drawing area means that there a new line point
                // should be created.
                p.setLocation(searchGridPoint(p));
                model.selPoint = p;
            }
        }
        return SelectMode.MODELING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        if (model.drawTool == Tool.connection && model.tmpElem1 != null) {
            sketchLastPoint = e.getPoint();
            panel.repaint();
        }
        return SelectMode.MODELING;
    }

}
