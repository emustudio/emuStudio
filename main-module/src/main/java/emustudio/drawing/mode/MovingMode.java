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

import emustudio.drawing.*;
import emustudio.drawing.DrawingPanel.Tool;
import emustudio.drawing.mode.ModeSelector.SelectMode;
import emustudio.gui.ElementPropertiesDialog;

import java.awt.*;
import java.awt.event.MouseEvent;

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

    MovingMode(DrawingPanel panel, Model model) {
        super(panel, model);
    }

    private void doPop(MouseEvent e, Element elem) {
        ElementPopUpMenu menu = new ElementPopUpMenu(elem, panel.getParentDialog());
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    @Override
    public void drawTemporaryGraphics(Graphics2D graphics) {
        // modelling a small red circle around selected connection line point
        if (model.selPoint != null) {
            ConnectionLine.highlightPoint(model.selPoint, graphics);
        }
    }

    @Override
    public SelectMode mouseClicked(MouseEvent e) {
        if (model.tmpElem1 != null) {
            if (e.getClickCount() == 2) {
                new ElementPropertiesDialog(panel.getParentDialog(), model.tmpElem1).setVisible(true);
                panel.repaint();
            }
        }
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        showPopupIfPressed(e);
        if (e.getButton() == MouseEvent.BUTTON1) {
            // detect if user wants to resize an element
            model.tmpElem1 = schema.getElementByBorderPoint(p);
            if (model.tmpElem1 != null) {
                return SelectMode.RESIZING;
            }
            model.tmpElem1 = schema.getCrossingElement(p);
            if (model.tmpElem1 != null) {
                model.selLine = null;
                model.elementDragged = false;
                return SelectMode.MOVING;
            }
        }

        // add/remove a point to/from line, or start point drag-n-drop
        // if the user is near a connection line
        model.selPoint = null;
        model.selLine = null;
        model.selLine = schema.getCrossingLine(p);

        if (model.selLine != null) {
            model.selPoint = model.selLine.containsPoint(p, ConnectionLine.TOLERANCE);
        }
        panel.repaint(); // because of drawing selected point

        // if user press a mouse button on empty area, activate "selection" mode
        if (model.selLine == null) {
            model.selectionStart = e.getPoint(); // point without grid correction
            return SelectMode.SELECTING;
        }
        return SelectMode.MOVING;
    }

    private void showPopupIfPressed(MouseEvent e) {
        Point p = e.getPoint();
        if (e.isPopupTrigger()) {
            model.tmpElem1 = schema.getCrossingElement(p);
            if (model.tmpElem1 != null) {
                doPop(e, model.tmpElem1);
            }
            model.tmpElem1 = null;
        }
    }

    @Override
    public SelectMode mouseReleased(MouseEvent e) {
        Point p = e.getPoint();
        showPopupIfPressed(e);
        // if an element was clicked, selecting it, if user holds CTRL or SHIFT
        if (e.getButton() == MouseEvent.BUTTON1) {
            int ctrl_shift = e.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK
                | MouseEvent.CTRL_DOWN_MASK);
            if ((!model.elementDragged) && (ctrl_shift == 0)) {
                schema.selectElements(-1, -1, 0, 0);
            }
            Element elem = schema.getCrossingElement(p);
            if ((model.tmpElem1 == elem) && (elem != null) && (!model.elementDragged)) {
                elem.setSelected(true);
                panel.repaint();
                return SelectMode.MOVING;
            }
            if ((model.selLine == null) || (model.selLine != schema.getCrossingLine(p))) {
                return SelectMode.MOVING;
            }
            model.selLine.setSelected(true);
        }

        // if a point is selected, remove it if user pressed, right mouse button
        if (model.selLine != null && model.selPoint != null) {
            if (e.getButton() != MouseEvent.BUTTON3) {
                return SelectMode.MOVING;
            }
            Point linePoint = model.selLine.containsPoint(p, ConnectionLine.TOLERANCE);
            if ((model.selLine != schema.getCrossingLine(p))
                || (model.selPoint != linePoint)) {
                model.selLine = null;
                model.selPoint = null;
                return SelectMode.MOVING;
            }
            model.selLine.removePoint(model.selPoint);
            model.selPoint = null;
            model.selLine = null;
        }
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        if (model.selLine != null) {
            if (p.getX() < 0 || p.getY() < 0) {
                return SelectMode.MOVING;
            }
            Point gridPoint = searchGridPoint(p);
            if (model.selPoint == null) {
                int pi = model.selLine.getCrossPoint(p, ConnectionLine.TOLERANCE); // should not be -1
                if (pi == -1) {
                    return SelectMode.MOVING;
                }
                p.setLocation(gridPoint);
                Point linePoint = model.selLine.containsPoint(p, ConnectionLine.TOLERANCE);
                if (linePoint == null) {
                    model.selLine.addPoint(pi, p);
                    model.selPoint = p;
                }
            } else {
                p.setLocation(gridPoint);
                model.selLine.movePoint(model.selPoint, p);
            }
        } else if (model.tmpElem1 != null) {
            if (p.getX() < 0 || p.getY() < 0) {
                return SelectMode.MOVING;
            }
            p.setLocation(searchGridPoint(p));

            model.elementDragged = true;
            // if the element is selected, we must moving all selected elements either.
            if (model.tmpElem1.isSelected()) {
                int diffX, diffY;
                diffX = p.x - model.tmpElem1.getX();
                diffY = p.y - model.tmpElem1.getY();
                schema.moveSelection(diffX, diffY);
            } else {
                model.tmpElem1.move(p);
            }
        }
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        model.selPoint = null;
        if (model.selLine != null) {
            panel.repaint();
        }
        model.selLine = null;

        // resize mouse pointers
        if (model.drawTool == Tool.nothing) {
            Point p = e.getPoint();
            model.tmpElem1 = schema.getElementByBorderPoint(p);
            if (model.tmpElem1 != null) {
                if (model.tmpElem1.crossesBottomBorder(p)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                } else if (model.tmpElem1.crossesLeftBorder(p)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                } else if (model.tmpElem1.crossesRightBorder(p)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                } else if (model.tmpElem1.crossesTopBorder(p)) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                } else {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            } else {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        // highlight line points
        for (int i = schema.getConnectionLines().size() - 1; i >= 0; i--) {
            Point[] ps = schema.getConnectionLines().get(i).getPoints().toArray(new Point[0]);
            Point p = e.getPoint();
            boolean out = false;
            for (Point p1 : ps) {
                if (ConnectionLine.doPointsEqual(p1, p)) {
                    model.selLine = schema.getConnectionLines().get(i);
                    model.selPoint = p1;
                    out = true;
                    break;
                }
            }
            if (out) {
                panel.repaint();
                break;
            }
        }
        return SelectMode.MOVING;
    }

}
