/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.components.P;
import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaTestSupport;
import net.emustudio.application.gui.schema.elements.CompilerElement;
import org.junit.Test;

import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ResizingModeTest {

    @Test
    public void basicMethodsStayInResizingMode() {
        DrawingPanel panel = new DrawingPanel(mock(Schema.class));
        DrawingModel model = new DrawingModel();
        ResizingMode mode = new ResizingMode(panel, model);
        Graphics2D graphics = SchemaTestSupport.createGraphics();
        try {
            mode.drawTemporaryGraphics(graphics);
        } finally {
            graphics.dispose();
        }

        assertEquals(ModeSelector.SelectMode.RESIZING, mode.mouseClicked(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_CLICKED, java.awt.event.MouseEvent.BUTTON1, 1, 1)
        ));
        assertEquals(ModeSelector.SelectMode.RESIZING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_PRESSED, java.awt.event.MouseEvent.BUTTON1, 1, 1)
        ));
        assertEquals(ModeSelector.SelectMode.RESIZING, mode.mouseMoved(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_MOVED, java.awt.event.MouseEvent.NOBUTTON, 1, 1)
        ));
    }

    @Test
    public void draggingWithoutElementJustKeepsResizingMode() {
        DrawingPanel panel = new DrawingPanel(mock(Schema.class));
        DrawingModel model = new DrawingModel();
        ResizingMode mode = new ResizingMode(panel, model);
        setResizeMode(mode, 0);

        assertEquals(ModeSelector.SelectMode.RESIZING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, 20, 30)
        ));
    }

    @Test
    public void draggingOnMeasuredBorderResizesElementAndReleaseReturnsMoving() {
        DrawingPanel panel = new DrawingPanel(mock(Schema.class));
        DrawingModel model = new DrawingModel();
        CompilerElement element = measuredElement();
        model.tmpElem1 = element;

        int oldWidth = element.getWidth();
        Rectangle rect = element.getRectangle();
        ResizingMode mode = new ResizingMode(panel, model);

        setResizeMode(mode, 3);
        assertEquals(ModeSelector.SelectMode.RESIZING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, rect.x + rect.width + 20, element.getY())
        ));
        assertTrue(element.getWidth() > oldWidth);
        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, java.awt.event.MouseEvent.BUTTON1, rect.x + rect.width + 4, element.getY())
        ));
    }

    @Test
    public void draggingFromTopBottomAndLeftBordersComputesResizeMode() {
        DrawingPanel panel = new DrawingPanel(mock(Schema.class));
        CompilerElement topElement = measuredElement();
        DrawingModel topModel = new DrawingModel();
        topModel.tmpElem1 = topElement;
        ResizingMode topMode = new ResizingMode(panel, topModel);
        setResizeMode(topMode, -1);

        int originalTopHeight = topElement.getHeight();
        Rectangle topRect = topElement.getRectangle();
        assertEquals(ModeSelector.SelectMode.RESIZING, topMode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, topElement.getX(), topRect.y - 4)
        ));
        assertNotEquals(originalTopHeight, topElement.getHeight());

        CompilerElement bottomElement = measuredElement();
        DrawingModel bottomModel = new DrawingModel();
        bottomModel.tmpElem1 = bottomElement;
        ResizingMode bottomMode = new ResizingMode(panel, bottomModel);
        setResizeMode(bottomMode, -1);

        int originalBottomHeight = bottomElement.getHeight();
        Rectangle bottomRect = bottomElement.getRectangle();
        assertEquals(ModeSelector.SelectMode.RESIZING, bottomMode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, bottomElement.getX(), bottomRect.y + bottomRect.height + 4)
        ));
        assertNotEquals(originalBottomHeight, bottomElement.getHeight());

        CompilerElement leftElement = measuredElement();
        DrawingModel leftModel = new DrawingModel();
        leftModel.tmpElem1 = leftElement;
        ResizingMode leftMode = new ResizingMode(panel, leftModel);
        setResizeMode(leftMode, -1);

        int originalLeftWidth = leftElement.getWidth();
        Rectangle leftRect = leftElement.getRectangle();
        assertEquals(ModeSelector.SelectMode.RESIZING, leftMode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, leftRect.x - 4, leftElement.getY())
        ));
        assertNotEquals(originalLeftWidth, leftElement.getWidth());
    }

    @Test
    public void draggingAwayFromBordersLeavesElementSizeUntouched() {
        DrawingPanel panel = new DrawingPanel(mock(Schema.class));
        DrawingModel model = new DrawingModel();
        CompilerElement element = measuredElement();
        model.tmpElem1 = element;
        ResizingMode mode = new ResizingMode(panel, model);
        setResizeMode(mode, -1);

        int width = element.getWidth();
        int height = element.getHeight();

        assertEquals(ModeSelector.SelectMode.RESIZING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, element.getX(), element.getY())
        ));
        assertEquals(width, element.getWidth());
        assertEquals(height, element.getHeight());
    }

    private CompilerElement measuredElement() {
        CompilerElement element = new CompilerElement(P.of(100, 100), "compiler", "compiler.jar");
        Graphics2D graphics = SchemaTestSupport.createGraphics();
        try {
            element.measure(graphics);
        } finally {
            graphics.dispose();
        }
        return element;
    }

    private void setResizeMode(ResizingMode mode, int value) {
        try {
            Field field = ResizingMode.class.getDeclaredField("resizeMode");
            field.setAccessible(true);
            field.setInt(mode, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
