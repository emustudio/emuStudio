/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaTestSupport;
import org.junit.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class SelectingModeTest {

    @Test
    public void drawTemporaryGraphicsAndBasicMouseMethodsStayInSelectingMode() {
        DrawingPanel panel = new DrawingPanel(mock(Schema.class));
        DrawingModel model = new DrawingModel();
        model.selectionStart = new Point(50, 60);
        model.selectionEnd = new Point(20, 30);
        SelectingMode mode = new SelectingMode(panel, model);

        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            mode.drawTemporaryGraphics(graphics);
        } finally {
            graphics.dispose();
        }

        assertNotEquals(0, image.getRGB(20, 30));
        assertEquals(ModeSelector.SelectMode.SELECTING, mode.mouseClicked(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON1, 5, 5)
        ));
        assertEquals(ModeSelector.SelectMode.SELECTING, mode.mousePressed(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 5, 5)
        ));
        assertEquals(ModeSelector.SelectMode.SELECTING, mode.mouseMoved(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_MOVED, MouseEvent.NOBUTTON, 5, 5)
        ));
    }

    @Test
    public void dragAndReleaseNormalizeSelectionRectangleAndReturnMovingMode() {
        Schema schema = mock(Schema.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectionStart = new Point(50, 60);
        SelectingMode mode = new SelectingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.SELECTING, mode.mouseDragged(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, java.awt.event.MouseEvent.BUTTON1, 20, 30)
        ));
        assertEquals(new Point(20, 30), model.selectionEnd);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, java.awt.event.MouseEvent.BUTTON1, 20, 30)
        ));

        verify(schema).select(20, 30, 30, 30);
        assertNull(model.selectionStart);
        assertNull(model.selectionEnd);
    }

    @Test
    public void releaseWithoutDraggingUsesReleasePointAsSelectionEnd() {
        Schema schema = mock(Schema.class);
        DrawingPanel panel = new DrawingPanel(schema);
        DrawingModel model = new DrawingModel();
        model.selectionStart = new Point(10, 15);
        SelectingMode mode = new SelectingMode(panel, model);

        assertEquals(ModeSelector.SelectMode.MOVING, mode.mouseReleased(
                SchemaTestSupport.mouseEvent(panel, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 40, 55)
        ));

        verify(schema).select(10, 15, 30, 40);
        assertNull(model.selectionStart);
        assertNull(model.selectionEnd);
    }
}
