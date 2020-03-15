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

import net.emustudio.application.configuration.PluginConfig;
import net.emustudio.application.gui.NamePath;
import net.emustudio.application.gui.P;
import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.elements.*;
import net.emustudio.application.gui.schema.mode.ModeSelector.SelectMode;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

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
                drawingModel.selectedPoint = P.of(searchGridPoint(clickPoint));
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
                drawingModel.selectedLine = schema.getCrossingLine(clickPoint);
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
                if (drawingModel.selectedLine != schema.getCrossingLine(clickPoint)) {
                    drawingModel.selectedLine = null;
                    return SelectMode.MODELING;
                }
                schema.removeConnectionLine(drawingModel.selectedLine);
                drawingModel.selectedLine = null;
                panel.fireToolWasUsed();
            }
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_COMPILER) {
            schema.setCompilerElement(new CompilerElement(createConfig(PLUGIN_TYPE.COMPILER, clickPoint)));
            panel.fireToolWasUsed();
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CPU) {
            schema.setCpuElement(new CpuElement(createConfig(PLUGIN_TYPE.CPU, clickPoint)));
            panel.fireToolWasUsed();
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_MEMORY) {
            schema.setMemoryElement(new MemoryElement(createConfig(PLUGIN_TYPE.MEMORY, clickPoint)));
            panel.fireToolWasUsed();
        } else if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_DEVICE) {
            schema.addDeviceElement(new DeviceElement(createConfig(PLUGIN_TYPE.DEVICE, clickPoint)));
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
                boolean alreadyConnected = schema.getConnectionLines().stream()
                    .anyMatch(l -> l.containsElement(drawingModel.tmpElem1) && l.containsElement(drawingModel.tmpElem2));

                if (!alreadyConnected && (drawingModel.tmpElem1 != drawingModel.tmpElem2)) {
                    ConnectionLine line = new ConnectionLine(
                        drawingModel.tmpElem1, drawingModel.tmpElem2,
                        drawingModel.tmpPoints,
                        drawingModel.bidirectional
                    );
                    schema.addConnectionLine(line);
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
        Point p = e.getPoint();
        if (drawingModel.drawTool == DrawingPanel.Tool.TOOL_CONNECTION) {
            if (schema.getCrossingElement(p) == null) {
                // if user didn't clicked on an element, but on drawing area means that there a new line point
                // should be created.
                p.setLocation(searchGridPoint(p));
                drawingModel.selectedPoint =  P.of(p);
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

    private PluginConfig createConfig(PLUGIN_TYPE pluginType, Point clickPoint) {
        NamePath namePath = drawingModel.pluginNamePath;
        return PluginConfig.create(pluginType, namePath.name, namePath.path, searchGridPoint(clickPoint));
    }
}
