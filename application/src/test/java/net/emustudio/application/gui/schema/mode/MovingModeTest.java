/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.components.P;
import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaTestSupport;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MovingModeTest {

    @Test
    public void drawTemporaryGraphicsHighlightsSelectedPointAndMouseClickKeepsMovingMode() {
        DrawingPanel panel = new DrawingPanel(mock(Schema.class));
        DrawingModel model = new DrawingModel();
        model.selectedPoint = P.of(25, 30);
        MovingMode mode = new MovingMode(panel, model);

        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            mode.drawTemporaryGraphics(graphics);
        } finally {
            graphics.dispose();
        }

        assertNotEquals(0, image.getRGB(25, 30));
        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseClicked(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON1, 25, 30)
        ));
    }

    @Test
    public void mousePressedOnEmptyAreaStartsSelectionMode() {
        Schema schema = mock(Schema.class);
        when(schema.getElementByBorderPoint(any(Point.class))).thenReturn(null);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(null);
        when(schema.findCrossingLine(any(Point.class))).thenReturn(null);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.SELECTING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_PRESSED, java.awt.event.MouseEvent.BUTTON1, 10, 20)
        ));
        assertEquals(new Point(10, 20), model.selectionStart);
    }

    @Test
    public void mousePressedOnBorderSwitchesToResizingMode() {
        Schema schema = mock(Schema.class);
        Element element = mock(Element.class);
        when(schema.getElementByBorderPoint(any(Point.class))).thenReturn(element);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.RESIZING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 15, 25)
        ));
        assertSame(element, model.tmpElem1);
    }

    @Test
    public void mousePressedOnElementStartsMovingAndClearsLineSelection() {
        Schema schema = mock(Schema.class);
        Element element = mock(Element.class);
        ConnectionLine oldSelection = mock(ConnectionLine.class);
        when(schema.getElementByBorderPoint(any(Point.class))).thenReturn(null);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(element);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectedLine = oldSelection;
        model.elementDragged = true;
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 30, 40)
        ));
        assertSame(element, model.tmpElem1);
        assertNull(model.selectedLine);
        assertFalse(model.elementDragged);
    }

    @Test
    public void mousePressedOnLineKeepsMovingModeAndSelectsLinePoint() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        P point = P.of(11, 22);
        when(schema.getElementByBorderPoint(any(Point.class))).thenReturn(null);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(null);
        when(schema.findCrossingLine(any(Point.class))).thenReturn(line);
        when(line.findPoint(any(Point.class))).thenReturn(point);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 11, 22)
        ));
        assertSame(line, model.selectedLine);
        assertSame(point, model.selectedPoint);
    }

    @Test
    public void draggingSelectedLineWithoutPointCreatesANewPoint() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectedLine = line;
        MovingMode mode = new MovingMode(panel, model);

        when(line.findCrossingPoint(any(Point.class))).thenReturn(0);
        when(line.findPoint(any(Point.class))).thenReturn(null);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, 30, 40)
        ));

        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        verify(schema).addLinePoint(eq(line), eq(0), pointCaptor.capture());
        assertEquals(new Point(30, 40), pointCaptor.getValue());
        assertEquals(30, model.selectedPoint.ix());
        assertEquals(40, model.selectedPoint.iy());
    }

    @Test
    public void draggingSelectedPointMovesExistingLinePoint() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        P selectedPoint = P.of(4, 5);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectedLine = line;
        model.selectedPoint = selectedPoint;
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_DRAGGED, MouseEvent.BUTTON1, 33, 44)
        ));

        verify(schema).moveLinePoint(line, selectedPoint, new Point(33, 44));
    }

    @Test
    public void draggingSelectedElementMovesSelectionWhenElementIsSelected() {
        Schema schema = mock(Schema.class);
        Element element = mock(Element.class);
        when(element.isSelected()).thenReturn(true);
        when(element.getX()).thenReturn(10);
        when(element.getY()).thenReturn(20);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.tmpElem1 = element;
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_DRAGGED, MouseEvent.BUTTON1, 25, 35)
        ));

        assertTrue(model.elementDragged);
        verify(schema).moveSelection(15, 15);
        verify(schema, never()).moveElement(any(Element.class), any(Point.class));
    }

    @Test
    public void draggingUnselectedElementMovesOnlyThatElement() {
        Schema schema = mock(Schema.class);
        Element element = mock(Element.class);
        when(element.isSelected()).thenReturn(false);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.tmpElem1 = element;
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_DRAGGED, MouseEvent.BUTTON1, 45, 55)
        ));

        assertTrue(model.elementDragged);
        verify(schema).moveElement(element, new Point(45, 55));
        verify(schema, never()).moveSelection(anyInt(), anyInt());
    }

    @Test
    public void mouseReleasedSelectsClickedElementWhenNotDragged() {
        Schema schema = mock(Schema.class);
        Element element = mock(Element.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(element);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.tmpElem1 = element;
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 12, 13)
        ));

        verify(schema).select(-1, -1, 0, 0);
        verify(element).setSelected(true);
    }

    @Test
    public void mouseReleasedSelectsLineWhenReleasedOnSameLine() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(null);
        when(schema.findCrossingLine(any(Point.class))).thenReturn(line);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectedLine = line;
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 17, 19)
        ));

        verify(schema).select(-1, -1, 0, 0);
        verify(line).setSelected(true);
    }

    @Test
    public void rightClickReleaseRemovesSelectedLinePoint() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        P selectedPoint = P.of(70, 80);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectedLine = line;
        model.selectedPoint = selectedPoint;
        MovingMode mode = new MovingMode(panel, model);

        when(schema.findCrossingLine(any(Point.class))).thenReturn(line);
        when(line.findPoint(any(Point.class))).thenReturn(selectedPoint);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, java.awt.event.MouseEvent.BUTTON3, 70, 80)
        ));

        verify(line).removePoint(selectedPoint);
        assertNull(model.selectedLine);
        assertNull(model.selectedPoint);
    }

    @Test
    public void rightClickReleaseClearsSelectionWhenPointDoesNotMatch() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        P selectedPoint = P.of(70, 80);
        P otherPoint = P.of(71, 81);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectedLine = line;
        model.selectedPoint = selectedPoint;
        MovingMode mode = new MovingMode(panel, model);

        when(schema.findCrossingLine(any(Point.class))).thenReturn(line);
        when(line.findPoint(any(Point.class))).thenReturn(otherPoint);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON3, 70, 80)
        ));

        assertNull(model.selectedLine);
        assertNull(model.selectedPoint);
        verify(line, never()).removePoint(any(P.class));
    }

    @Test
    public void mouseMovedUpdatesCursorAndHighlightsLinePoint() {
        Schema schema = mock(Schema.class);
        Element borderElement = mock(Element.class);
        ConnectionLine oldSelection = mock(ConnectionLine.class);
        ConnectionLine line = mock(ConnectionLine.class);
        P point = P.of(40, 50);
        when(schema.getElementByBorderPoint(any(Point.class))).thenReturn(borderElement, (Element) null);
        when(borderElement.crossesBottomBorder(any(Point.class))).thenReturn(true);
        when(schema.getConnectionLines()).thenReturn(Collections.singletonList(line), Collections.emptyList());
        when(line.getPoints()).thenReturn(Collections.singletonList(point));
        when(line.findPoint(any(Point.class))).thenReturn(point);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectedLine = oldSelection;
        MovingMode mode = new MovingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseMoved(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_MOVED, MouseEvent.NOBUTTON, 40, 50)
        ));
        assertEquals(Cursor.S_RESIZE_CURSOR, panel.getCursor().getType());
        assertSame(line, model.selectedLine);
        assertSame(point, model.selectedPoint);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseMoved(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_MOVED, MouseEvent.NOBUTTON, 5, 5)
        ));
        assertEquals(Cursor.DEFAULT_CURSOR, panel.getCursor().getType());
        assertNull(model.selectedLine);
        assertNull(model.selectedPoint);
    }
}
