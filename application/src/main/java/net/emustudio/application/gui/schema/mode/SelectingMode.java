/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.mode.ModeSelector.SelectMode;

import java.awt.*;
import java.awt.event.MouseEvent;

class SelectingMode extends AbstractMode {

    SelectingMode(DrawingPanel panel, DrawingModel drawingModel) {
        super(panel, drawingModel);
    }

    @Override
    public void drawTemporaryGraphics(Graphics2D graphics) {
        if ((drawingModel.selectionStart != null) && (drawingModel.selectionEnd != null)) {
            graphics.setColor(Color.BLUE);
            graphics.setStroke(DrawingPanel.DASHED_LINE);

            int x = drawingModel.selectionStart.x;
            int y = drawingModel.selectionStart.y;

            if (drawingModel.selectionEnd.x < x) {
                x = drawingModel.selectionEnd.x;
            }
            if (drawingModel.selectionEnd.y < y) {
                y = drawingModel.selectionEnd.y;
            }
            int w = drawingModel.selectionEnd.x - drawingModel.selectionStart.x;
            int h = drawingModel.selectionEnd.y - drawingModel.selectionStart.y;

            if (w < 0) {
                w = -w;
            }
            if (h < 0) {
                h = -h;
            }
            graphics.drawRect(x, y, w, h);
        }
    }

    @Override
    public SelectMode mouseClicked(MouseEvent e) {
        return SelectMode.SELECTING;
    }

    @Override
    public SelectMode mousePressed(MouseEvent e) {
        return SelectMode.SELECTING;
    }

    @Override
    public SelectMode mouseReleased(MouseEvent e) {
        Point p = e.getPoint();

        int x = drawingModel.selectionStart.x;
        int y = drawingModel.selectionStart.y;

        if (drawingModel.selectionEnd == null) {
            drawingModel.selectionEnd = p;
        }

        if (drawingModel.selectionEnd.x < x) {
            x = drawingModel.selectionEnd.x;
        }
        if (drawingModel.selectionEnd.y < y) {
            y = drawingModel.selectionEnd.y;
        }
        int w = drawingModel.selectionEnd.x - drawingModel.selectionStart.x;
        int h = drawingModel.selectionEnd.y - drawingModel.selectionStart.y;

        if (w < 0) {
            w = -w;
        }
        if (h < 0) {
            h = -h;
        }

        schema.select(x, y, w, h);

        drawingModel.selectionStart = null;
        drawingModel.selectionEnd = null;
        return SelectMode.MOVING;
    }

    @Override
    public SelectMode mouseDragged(MouseEvent e) {
        drawingModel.selectionEnd = e.getPoint();
        return SelectMode.SELECTING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        return SelectMode.SELECTING;
    }

}
