/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.components.P;
import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaTestSupport;
import net.emustudio.application.gui.schema.elements.CompilerElement;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.Element;
import org.junit.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class ModelingModeTest {

    @Test
    public void drawTemporaryGraphicsDrawsConnectionSketchAndMouseClickKeepsModelingMode() throws Exception {
        Schema schema = mock(Schema.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        model.tmpElem1 = new CompilerElement(P.of(10, 10), "compiler", "compiler.jar");
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseMoved(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_MOVED, MouseEvent.NOBUTTON, 40, 50)
        ));

        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            mode.drawTemporaryGraphics(graphics);
        } finally {
            graphics.dispose();
        }

        assertNotEquals(0, image.getRGB(25, 30));
        assertEquals(new Point(40, 50), SchemaTestSupport.getField(mode, "sketchLastPoint", Point.class));
        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseClicked(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON1, 40, 50)
        ));
    }

    @Test
    public void deviceToolAddsElementAndNotifiesToolListener() {
        Schema schema = mock(Schema.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_DEVICE;
        model.pluginFileName = "device.jar";
        final AtomicInteger toolUses = new AtomicInteger();
        panel.addToolListener(new DrawingPanel.ToolListener() {
            @Override
            public void toolWasUsed() {
                toolUses.incrementAndGet();
            }
        });

        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, java.awt.event.MouseEvent.BUTTON1, 30, 40)
        ));

        verify(schema).addDeviceElement(new Point(30, 40), "device.jar");
        assertEquals(1, toolUses.get());
    }

    @Test
    public void connectionPressTracksEndpointsAndCanvasPoint() {
        Schema schema = mock(Schema.class);
        Element first = mock(Element.class);
        Element second = mock(Element.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(first, second, null);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 10, 10)
        ));
        assertSame(first, model.tmpElem1);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 20, 20)
        ));
        assertSame(second, model.tmpElem2);

        model.tmpElem2 = null;
        assertEquals(ModeSelector.SelectMode.MODELING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 30, 40)
        ));
        assertEquals(30, model.selectedPoint.ix());
        assertEquals(40, model.selectedPoint.iy());
    }

    @Test
    public void connectionPressIgnoresNonLeftButton() {
        Schema schema = mock(Schema.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3, 5, 5)
        ));
        verifyNoInteractions(schema);
    }

    @Test
    public void emptyConnectionPressWithoutStartElementCancelsToolUsage() {
        Schema schema = mock(Schema.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(null);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        final AtomicInteger toolUses = new AtomicInteger();
        panel.addToolListener(new DrawingPanel.ToolListener() {
            @Override
            public void toolWasUsed() {
                toolUses.incrementAndGet();
            }
        });

        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_PRESSED, java.awt.event.MouseEvent.BUTTON1, 5, 5)
        ));
        assertEquals(1, toolUses.get());
    }

    @Test
    public void deletePressTracksCrossingLineWhenNoElementWasHit() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(null);
        when(schema.findCrossingLine(any(Point.class))).thenReturn(line);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_DELETE;
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 9, 9)
        ));
        assertSame(line, model.selectedLine);
    }

    @Test
    public void deleteToolRemovesElementWhenReleasedOverSameElement() {
        Schema schema = mock(Schema.class);
        Element element = mock(Element.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(element);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_DELETE;
        model.tmpElem1 = element;
        final AtomicInteger toolUses = new AtomicInteger();
        panel.addToolListener(new DrawingPanel.ToolListener() {
            @Override
            public void toolWasUsed() {
                toolUses.incrementAndGet();
            }
        });

        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, java.awt.event.MouseEvent.BUTTON1, 10, 10)
        ));

        verify(schema).removeElement(element);
        assertEquals(1, toolUses.get());
    }

    @Test
    public void deleteReleaseKeepsModelingWhenReleasedWithWrongButtonOrOutsideElement() {
        Schema schema = mock(Schema.class);
        Element originalElement = mock(Element.class);
        Element otherElement = mock(Element.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(otherElement);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_DELETE;
        model.tmpElem1 = originalElement;
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON3, 10, 10)
        ));
        assertSame(originalElement, model.tmpElem1);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 10, 10)
        ));
        assertNull(model.tmpElem1);
        verify(schema, never()).removeElement(any(Element.class));
    }

    @Test
    public void deleteReleaseRemovesSelectedLineWhenReleasedOnSameLine() {
        Schema schema = mock(Schema.class);
        ConnectionLine line = mock(ConnectionLine.class);
        when(schema.findCrossingLine(any(Point.class))).thenReturn(line);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_DELETE;
        model.selectedLine = line;
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 14, 16)
        ));
        verify(schema).removeConnectionLine(line);
        assertNull(model.selectedLine);
    }

    @Test
    public void toolSpecificReleaseDelegatesToCompilerCpuAndMemoryFactories() {
        Schema schema = mock(Schema.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.pluginFileName = "plugin.jar";
        ModelingMode mode = new ModelingMode(panel, model);

        model.drawTool = DrawingPanel.Tool.TOOL_COMPILER;
        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 1, 2)
        ));
        verify(schema).setCompilerElement(new Point(1, 2), "plugin.jar");

        model.drawTool = DrawingPanel.Tool.TOOL_CPU;
        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 3, 4)
        ));
        verify(schema).setCpuElement(new Point(3, 4), "plugin.jar");

        model.drawTool = DrawingPanel.Tool.TOOL_MEMORY;
        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 5, 6)
        ));
        verify(schema).setMemoryElement(new Point(5, 6), "plugin.jar");
    }

    @Test
    public void connectionToolAddsLineWhenBothEndpointsAreReady() {
        Schema schema = mock(Schema.class);
        Element first = mock(Element.class);
        Element second = mock(Element.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(second);
        when(schema.isConnected(first, second)).thenReturn(false);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        model.tmpElem1 = first;
        model.tmpElem2 = second;
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, java.awt.event.MouseEvent.BUTTON1, 50, 50)
        ));

        verify(schema).addConnectionLine(first, second, model.tmpPoints, true);
    }

    @Test
    public void connectionReleaseResetsWhenReleasedOnUnexpectedElement() {
        Schema schema = mock(Schema.class);
        Element first = mock(Element.class);
        Element second = mock(Element.class);
        Element third = mock(Element.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        ModelingMode mode = new ModelingMode(panel, model);

        model.tmpElem1 = first;
        when(schema.getCrossingElement(any(Point.class))).thenReturn(second);
        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 10, 10)
        ));
        assertNull(model.tmpElem1);

        model.tmpElem1 = first;
        model.tmpElem2 = second;
        when(schema.getCrossingElement(any(Point.class))).thenReturn(third);
        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 12, 12)
        ));
        assertNull(model.tmpElem1);
        assertNull(model.tmpElem2);
    }

    @Test
    public void connectionReleaseAddsIntermediatePointWhenReleasedOnCanvas() {
        Schema schema = mock(Schema.class);
        Element first = mock(Element.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(null);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        model.tmpElem1 = first;
        model.selectedPoint = P.of(22, 33);
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 22, 33)
        ));
        assertEquals(1, model.tmpPoints.size());
        assertEquals(22, model.tmpPoints.get(0).ix());
        assertEquals(33, model.tmpPoints.get(0).iy());
        assertNull(model.selectedPoint);
    }

    @Test
    public void connectionReleaseSkipsExistingConnectionButStillResetsState() {
        Schema schema = mock(Schema.class);
        Element first = mock(Element.class);
        Element second = mock(Element.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(second);
        when(schema.isConnected(first, second)).thenReturn(true);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        model.tmpElem1 = first;
        model.tmpElem2 = second;
        model.tmpPoints.add(P.of(1, 1));
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 50, 50)
        ));

        verify(schema, never()).addConnectionLine(any(Element.class), any(Element.class), anyList(), anyBoolean());
        assertNull(model.tmpElem1);
        assertNull(model.tmpElem2);
        assertTrue(model.tmpPoints.isEmpty());
    }

    @Test
    public void draggingConnectionOnCanvasUpdatesSelectedPoint() {
        Schema schema = mock(Schema.class);
        when(schema.getCrossingElement(any(Point.class))).thenReturn(null);

        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.drawTool = DrawingPanel.Tool.TOOL_CONNECTION;
        ModelingMode mode = new ModelingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MODELING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_DRAGGED, MouseEvent.BUTTON1, 60, 70)
        ));
        assertEquals(60, model.selectedPoint.ix());
        assertEquals(70, model.selectedPoint.iy());
    }
}
