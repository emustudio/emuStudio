/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.mode.ModeSelector.SelectMode;

import java.awt.*;
import java.awt.event.MouseEvent;

class ResizingMode extends AbstractMode {
    private final static int RESIZE_TOP = 0;
    private final static int RESIZE_LEFT = 1;
    private final static int RESIZE_BOTTOM = 2;
    private final static int RESIZE_RIGHT = 3;

    private int resizeMode;

    ResizingMode(DrawingPanel panel, DrawingModel drawingModel) {
        super(panel, drawingModel);
    }

    @Override
    public void drawTemporaryGraphics(Graphics2D graphics) {
    }

    @Override
    public SelectMode mouseClicked(MouseEvent e) {
        return SelectMode.RESIZING;
    }

    @Override
    public SelectMode mousePressed(MouseEvent e) {
        return SelectMode.RESIZING;
    }

    @Override
    public SelectMode mouseReleased(MouseEvent e) {
        resizeMode = -1;
        return SelectMode.MOVING;
    }

    private void computeResizeMode(Point point) {
        if (drawingModel.tmpElem1.crossesBottomBorder(point)) {
            resizeMode = RESIZE_BOTTOM;
        } else if (drawingModel.tmpElem1.crossesLeftBorder(point)) {
            resizeMode = RESIZE_LEFT;
        } else if (drawingModel.tmpElem1.crossesRightBorder(point)) {
            resizeMode = RESIZE_RIGHT;
        } else if (drawingModel.tmpElem1.crossesTopBorder(point)) {
            resizeMode = RESIZE_TOP;
        } else {
            resizeMode = -1; // TODO - corners
        }
    }

    @Override
    public SelectMode mouseDragged(MouseEvent e) {
        Point clickPoint = e.getPoint();

        if (resizeMode == -1) {
            computeResizeMode(clickPoint);
        }
        if (drawingModel.tmpElem1 == null) {
            return SelectMode.RESIZING;
        }
        switch (resizeMode) {
            case RESIZE_TOP:
                drawingModel.tmpElem1.setSize(drawingModel.tmpElem1.getWidth(), (drawingModel.tmpElem1.getY() - clickPoint.y) * 2);
                break;
            case RESIZE_BOTTOM:
                drawingModel.tmpElem1.setSize(drawingModel.tmpElem1.getWidth(), (clickPoint.y - drawingModel.tmpElem1.getY()) * 2);
                break;
            case RESIZE_LEFT:
                drawingModel.tmpElem1.setSize((drawingModel.tmpElem1.getX() - clickPoint.x) * 2, drawingModel.tmpElem1.getHeight());
                break;
            case RESIZE_RIGHT:
                drawingModel.tmpElem1.setSize((clickPoint.x - drawingModel.tmpElem1.getX()) * 2, drawingModel.tmpElem1.getHeight());
                break;
        }
        panel.repaint();
        return SelectMode.RESIZING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        return SelectMode.RESIZING;
    }

}
