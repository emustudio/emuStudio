/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.P;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DrawingModel {

    /**
     * Holds a point of a connection line.
     * <p>
     * This is used in "moving" mode for: - moving of the connection line point - add/delete connection line point
     * <p>
     * in the "modelling" mode, it is used for: - holds temporal point that will be added to temporal points array when
     * mouse is released, while drawing a line
     */
    public P selectedPoint;

    public DrawingPanel.Tool drawTool = DrawingPanel.Tool.TOOL_NOTHING;

    /**
     * This variable is used when "moving" mode is active and user moves an element. It holds the moving element object.
     * <p>
     * If "modelling" mode is active and when users draws a line, it represents the first element that the line is
     * connected to. If it is selected the element deletion, it represents a shape that should be deleted when mouse is
     * released.
     */
    public Element tmpElem1;

    /**
     * Used when drawing lines. It represents last element that the line is connected to.
     */
    public Element tmpElem2;

    /**
     * Point where the selection starts. It is set when the "selection" mode is activated.
     */
    public Point selectionStart;

    /**
     * Point where the selection ends. It is set when the "selection" mode is active and mouse released.
     */
    public Point selectionEnd;

    /**
     * Temporary points used in the process of connection line drawing. If the line is drawn, these points are saved,
     * they are cleared otherwise.
     */
    public final List<P> tmpPoints = new ArrayList<>();

    /**
     * Selected line. Used only in "moving" mode.
     * <p>
     * This variable is used if the user wants to remove or moving an existing connection line point.
     */
    public ConnectionLine selectedLine;


    /**
     * If an element is selected (mouse pressed) and then dragged, this variable holds true. It is false in all other
     * cases.
     * <p>
     * When the mouse is released, the value is tested. If it is true, it means that the element has been moved and
     * therefore possible selection of the other elements should not be cleared.
     */
    public boolean elementDragged = false;

    public String pluginFileName;

    /**
     * Future connection line direction. Holds true, if the drawing line should be bidirectional, false otherwise.
     */
    public boolean bidirectional = true;

    public void clear() {
        tmpElem1 = null;
        tmpElem2 = null;
        tmpPoints.clear();
        selectionStart = null;
        selectionEnd = null;
        selectedPoint = null;
        selectedLine = null;
        pluginFileName = null;
        drawTool = DrawingPanel.Tool.TOOL_NOTHING;
    }
}
