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
        Point p = e.getPoint();

        if (resizeMode == -1) {
            computeResizeMode(p);
        }
        if (drawingModel.tmpElem1 == null) {
            return SelectMode.RESIZING;
        }
        p.setLocation(searchGridPoint(p));
        switch (resizeMode) {
            case RESIZE_TOP:
                drawingModel.tmpElem1.setSize(drawingModel.tmpElem1.getWidth(), (drawingModel.tmpElem1.getY() - p.y) * 2);
                break;
            case RESIZE_BOTTOM:
                drawingModel.tmpElem1.setSize(drawingModel.tmpElem1.getWidth(), (p.y - drawingModel.tmpElem1.getY()) * 2);
                break;
            case RESIZE_LEFT:
                drawingModel.tmpElem1.setSize((drawingModel.tmpElem1.getX() - p.x) * 2, drawingModel.tmpElem1.getHeight());
                break;
            case RESIZE_RIGHT:
                drawingModel.tmpElem1.setSize((p.x - drawingModel.tmpElem1.getX()) * 2, drawingModel.tmpElem1.getHeight());
                break;
        }
        return SelectMode.RESIZING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        return SelectMode.RESIZING;
    }

}
