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
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;
import net.emustudio.application.gui.schema.mode.ModeSelector.SelectMode;
import net.emustudio.application.gui.P;

import java.awt.*;
import java.awt.event.MouseEvent;

import static net.emustudio.application.gui.P.SELECTION_TOLERANCE;

/**
 * Moving drawing mode, initial.
 * <p>
 * If no objects are selected, mouse is just moving - if the mouse hovers over components or line points, they are
 * highlighted.
 * <p>
 * If a user presses left mouse button over existing selection, selected components are going to be moved by following
 * mouse moves. The mouse releasement finishes the movement.
 * <p>
 * If a user presses left mouse button over a line point, it is going to be moved by following mouse moves. The mouse
 * releasement finishes the movement.
 * <p>
 * If a user presses left mouse button over a line (not on a line point), new line point is created on this location and
 * it is immediately selected for movement.
 * <p>
 * If a user presses left mouse button over empty area, the mode is switched to "selection" mode.
 */
class MovingMode extends AbstractMode {

    MovingMode(DrawingPanel panel, DrawingModel drawingModel) {
        super(panel, drawingModel);
    }

    @Override
    public void drawTemporaryGraphics(Graphics2D graphics) {
        // modelling a small red circle around selected connection line point
        if (drawingModel.selectedPoint != null) {
            ConnectionLine.highlightPoint(drawingModel.selectedPoint, graphics);
        }
    }

    @Override
    public SelectMode mouseClicked(MouseEvent e) {
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        if (e.getButton() == MouseEvent.BUTTON1) {
            // detect if user wants to resize an element
            drawingModel.tmpElem1 = schema.getElementByBorderPoint(p);
            if (drawingModel.tmpElem1 != null) {
                return SelectMode.RESIZING;
            }
            drawingModel.tmpElem1 = schema.getCrossingElement(p);
            if (drawingModel.tmpElem1 != null) {
                drawingModel.selectedLine = null;
                drawingModel.elementDragged = false;
                return SelectMode.MOVING;
            }
        }

        // add/remove a point to/from line, or start point drag-n-drop
        // if the user is near a connection line
        drawingModel.selectedPoint = null;
        drawingModel.selectedLine = null;
        drawingModel.selectedLine = schema.getCrossingLine(p);

        if (drawingModel.selectedLine != null) {
            drawingModel.selectedPoint = drawingModel.selectedLine.findPoint(p, SELECTION_TOLERANCE);
        }
        panel.repaint(); // because of drawing selected point

        // if user press a mouse button on empty area, activate "selection" mode
        if (drawingModel.selectedLine == null) {
            drawingModel.selectionStart = e.getPoint(); // point without grid correction
            return SelectMode.SELECTING;
        }
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        // if an element was clicked, selecting it, if user holds CTRL or SHIFT
        if (e.getButton() == MouseEvent.BUTTON1) {
            int ctrl_shift = e.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK
                | MouseEvent.CTRL_DOWN_MASK);
            if ((!drawingModel.elementDragged) && (ctrl_shift == 0)) {
                schema.selectElements(-1, -1, 0, 0);
            }
            Element elem = schema.getCrossingElement(p);
            if ((drawingModel.tmpElem1 == elem) && (elem != null) && (!drawingModel.elementDragged)) {
                elem.setSelected(true);
                panel.repaint();
                return SelectMode.MOVING;
            }
            if ((drawingModel.selectedLine == null) || (drawingModel.selectedLine != schema.getCrossingLine(p))) {
                return SelectMode.MOVING;
            }
            drawingModel.selectedLine.setSelected(true);
        }

        // if a point is selected, remove it if user pressed, right mouse button
        if (drawingModel.selectedLine != null && drawingModel.selectedPoint != null) {
            if (e.getButton() != MouseEvent.BUTTON3) {
                return SelectMode.MOVING;
            }
            P linePoint = drawingModel.selectedLine.findPoint(p, SELECTION_TOLERANCE);
            if ((drawingModel.selectedLine != schema.getCrossingLine(p))
                || (drawingModel.selectedPoint != linePoint)) {
                drawingModel.selectedLine = null;
                drawingModel.selectedPoint = null;
                return SelectMode.MOVING;
            }
            drawingModel.selectedLine.removePoint(drawingModel.selectedPoint);
            drawingModel.selectedPoint = null;
            drawingModel.selectedLine = null;
        }
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        if (drawingModel.selectedLine != null) {
            if (p.getX() < 0 || p.getY() < 0) {
                return SelectMode.MOVING;
            }
            Point gridPoint = searchGridPoint(p);
            if (drawingModel.selectedPoint == null) {
                int pi = drawingModel.selectedLine.getCrossPoint(p, SELECTION_TOLERANCE); // should not be -1
                if (pi == -1) {
                    return SelectMode.MOVING;
                }
                p.setLocation(gridPoint);
                P linePoint = drawingModel.selectedLine.findPoint(p, SELECTION_TOLERANCE);
                if (linePoint == null) {
                    drawingModel.selectedLine.addPoint(pi, p);
                    drawingModel.selectedPoint = P.of(p);
                }
            } else {
                p.setLocation(gridPoint);
                drawingModel.selectedLine.movePoint(schema, drawingModel.selectedPoint, p);
            }
        } else if (drawingModel.tmpElem1 != null) {
            if (p.getX() < 0 || p.getY() < 0) {
                return SelectMode.MOVING;
            }
            p.setLocation(searchGridPoint(p));

            drawingModel.elementDragged = true;
            // if the element is selected, we must moving all selected elements either.
            if (drawingModel.tmpElem1.isSelected()) {
                int diffX, diffY;
                diffX = p.x - drawingModel.tmpElem1.getX();
                diffY = p.y - drawingModel.tmpElem1.getY();
                schema.moveSelection(diffX, diffY);
            } else {
                drawingModel.tmpElem1.move(schema, p);
            }
        }
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        Point mousePoint = e.getPoint();
        drawingModel.selectedPoint = null;
        if (drawingModel.selectedLine != null) {
            panel.repaint();
        }
        drawingModel.selectedLine = null;

        // resize mouse pointers
        if (drawingModel.drawTool == DrawingPanel.Tool.nothing) {
            drawingModel.tmpElem1 = schema.getElementByBorderPoint(mousePoint);
            if (drawingModel.tmpElem1 != null) {
                if (drawingModel.tmpElem1.crossesBottomBorder(mousePoint)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                } else if (drawingModel.tmpElem1.crossesLeftBorder(mousePoint)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                } else if (drawingModel.tmpElem1.crossesRightBorder(mousePoint)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                } else if (drawingModel.tmpElem1.crossesTopBorder(mousePoint)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                } else {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            } else {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        // highlight line points
        outerloop:
        for (ConnectionLine line : schema.getConnectionLines()) {
            for (P point : line.getPoints()) {
                if (point.equals(mousePoint)) {
                    drawingModel.selectedLine = line;
                    drawingModel.selectedPoint = point;
                    panel.repaint();
                    break outerloop;
                }
            }
        }

        return SelectMode.MOVING;
    }
}
