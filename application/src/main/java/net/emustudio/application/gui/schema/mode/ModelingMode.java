/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.framework.P;
import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;
import net.emustudio.application.gui.schema.mode.ModeSelector.SelectMode;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Modeling drawing mode. New components are being added or deleted, according to selected tool. When mouse clicks on
 * the canvas, the component is created/deleted and mode is switched into "moving" mode.
 * <p>
 * If a user is creating a line, by clicking on an empty area a line point is created and user continues creating the
 * line - ie. the "modelling" mode stays.
 */
class ModelingMode extends AbstractMode {

    /**
     * This variable contains last sketch point when drawing a connection line. The last point is variable according to
     * the mouse position. It actually is the mouse position when drawing a line.
     * <p>
     * It is used only in "modelling" mode.
     */
    private Point sketchLastPoint;

    ModelingMode(DrawingPanel panel, DrawingModel drawingModel) {
        super(panel, drawingModel);
    }

    @Override
    public void drawTemporaryGraphics(Graphics2D graphics) {
        // if the connection line is being drawn, modelling the sketch
        if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CONNECTION && drawingModel.tmpElem1 != null) {
            ConnectionLine.drawSketch(graphics, drawingModel.tmpElem1, sketchLastPoint, drawingModel.tmpPoints);
        }
    }

    @Override
    public SelectMode mouseClicked(MouseEvent e) {
        return SelectMode.MODELING;
    }

    @Override
    public SelectMode mousePressed(MouseEvent e) {
        Point clickPoint = e.getPoint();
        if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CONNECTION) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return SelectMode.MODELING;
            }

            Element elem = schema.getCrossingElement(clickPoint);
            if (elem != null) {
                if (drawingModel.tmpElem1 == null) {
                    drawingModel.tmpElem1 = elem;
                } else if (drawingModel.tmpElem2 == null) {
                    drawingModel.tmpElem2 = elem;
                }
            } else {
                if (drawingModel.tmpElem1 == null) {
                    panel.fireToolWasUsed();
                    return SelectMode.MOVING;
                }
                // if user didn't clicked on an element, but on drawing area
                // means that there a new line point should be created.
                drawingModel.selectedPoint = P.of(clickPoint);
            }
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_DELETE) {
            // only left button is accepted
            if (e.getButton() != MouseEvent.BUTTON1) {
                return SelectMode.MODELING;
            }

            Element elem = schema.getCrossingElement(clickPoint);
            drawingModel.tmpElem1 = elem;

            // delete line?
            if (elem == null) {
                drawingModel.selectedLine = schema.findCrossingLine(clickPoint);
            }
        }
        return SelectMode.MODELING;
    }

    @Override
    public SelectMode mouseReleased(MouseEvent e) {
        Point clickPoint = e.getPoint();
        if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_DELETE) {
            if (drawingModel.tmpElem1 != null) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return SelectMode.MODELING;
                }
                // if the mouse is released upon a point outside a tmpElem1, nothing is done.
                if (drawingModel.tmpElem1 != schema.getCrossingElement(clickPoint)) {
                    drawingModel.tmpElem1 = null;
                    return SelectMode.MODELING;
                }
                schema.removeElement(drawingModel.tmpElem1);
                drawingModel.tmpElem1 = null;
                panel.fireToolWasUsed();
            } else if (drawingModel.selectedLine != null) {
                // if the mouse is released upon a point outside the selLine, nothing is done.
                if (drawingModel.selectedLine != schema.findCrossingLine(clickPoint)) {
                    drawingModel.selectedLine = null;
                    return SelectMode.MODELING;
                }
                schema.removeConnectionLine(drawingModel.selectedLine);
                drawingModel.selectedLine = null;
                panel.fireToolWasUsed();
            }
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_COMPILER) {
            schema.setCompilerElement(clickPoint, drawingModel.pluginFileName);
            panel.fireToolWasUsed();
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CPU) {
            schema.setCpuElement(clickPoint, drawingModel.pluginFileName);
            panel.fireToolWasUsed();
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_MEMORY) {
            schema.setMemoryElement(clickPoint, drawingModel.pluginFileName);
            panel.fireToolWasUsed();
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_DEVICE) {
            schema.addDeviceElement(clickPoint, drawingModel.pluginFileName);
            panel.fireToolWasUsed();
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CONNECTION) {
            sketchLastPoint = null;
            Element element = schema.getCrossingElement(clickPoint);

            if (element != null) {
                if ((drawingModel.tmpElem2 == null) && (drawingModel.tmpElem1 != element)) {
                    drawingModel.tmpElem1 = null;
                    return SelectMode.MODELING;
                } else if ((drawingModel.tmpElem2 != null) && drawingModel.tmpElem2 != element) {
                    drawingModel.tmpElem1 = null;
                    drawingModel.tmpElem2 = null;
                    return SelectMode.MODELING;
                }
            } else {
                if ((drawingModel.tmpElem1 != null) && (drawingModel.selectedPoint != null)) {
                    drawingModel.tmpPoints.add(drawingModel.selectedPoint);
                    drawingModel.selectedPoint = null;
                    return SelectMode.MODELING;
                }
            }
            if ((drawingModel.tmpElem1 != null) && (drawingModel.tmpElem2 != null)) {
                // check if the connection exists already, or if there is a self-connection
                boolean alreadyConnected = schema.isConnected(drawingModel.tmpElem1, drawingModel.tmpElem2);
                if (!alreadyConnected && (drawingModel.tmpElem1 != drawingModel.tmpElem2)) {
                    schema.addConnectionLine(
                            drawingModel.tmpElem1, drawingModel.tmpElem2, drawingModel.tmpPoints, drawingModel.bidirectional
                    );
                }
                drawingModel.tmpElem1 = null;
                drawingModel.tmpElem2 = null;
                drawingModel.tmpPoints.clear();
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
        Point clickPoint = e.getPoint();
        if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CONNECTION) {
            if (schema.getCrossingElement(clickPoint) == null) {
                // if user didn't clicked on an element, but on drawing area means that there a new line point
                // should be created.
                drawingModel.selectedPoint = P.of(clickPoint);
            }
        }
        return SelectMode.MODELING;
    }

    @Override
    public SelectMode mouseMoved(MouseEvent e) {
        if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CONNECTION && drawingModel.tmpElem1 != null) {
            sketchLastPoint = e.getPoint();
            panel.repaint();
        }
        return SelectMode.MODELING;
    }
}
