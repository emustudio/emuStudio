/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.AbstractSwingTest;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class DrawingPanelTest extends AbstractSwingTest {

    @Test
    public void setToolUpdatesDrawingModelAndMode() throws Exception {
        Schema schema = mockSchemaForPanel();
        DrawingPanel panel = new DrawingPanel(schema);

        panel.setTool(DrawingPanel.Tool.TOOL_DEVICE, "device.jar");

        DrawingModel drawingModel = SchemaTestSupport.getField(panel, "drawingModel", DrawingModel.class);
        Object modeSelector = SchemaTestSupport.getField(panel, "mode", Object.class);
        Object currentMode = modeSelector.getClass().getMethod("get").invoke(modeSelector);

        assertEquals(DrawingPanel.Tool.TOOL_DEVICE, drawingModel.drawTool);
        assertEquals("device.jar", drawingModel.pluginFileName);
        assertEquals("ModelingMode", currentMode.getClass().getSimpleName());

        panel.setTool(null, null);
        currentMode = modeSelector.getClass().getMethod("get").invoke(modeSelector);

        assertEquals(null, drawingModel.drawTool);
        assertEquals("MovingMode", currentMode.getClass().getSimpleName());
    }

    @Test
    public void gridDirectionCancelAndToolListenersUpdateStateAndDelegateToSchema() throws Exception {
        Schema schema = mockSchemaForPanel();
        DrawingPanel panel = new DrawingPanel(schema);
        final AtomicInteger toolUseCount = new AtomicInteger();
        panel.addToolListener(new DrawingPanel.ToolListener() {
            @Override
            public void toolWasUsed() {
                toolUseCount.incrementAndGet();
            }
        });

        panel.setTool(DrawingPanel.Tool.TOOL_CONNECTION, "connection.jar");
        panel.setFutureLineDirection(false);
        panel.setUsingGrid(false);
        panel.setGridGap(25);
        panel.fireToolWasUsed();
        panel.cancelDrawing();

        DrawingModel drawingModel = SchemaTestSupport.getField(panel, "drawingModel", DrawingModel.class);
        assertFalse(drawingModel.bidirectional);
        assertEquals(DrawingPanel.Tool.TOOL_NOTHING, drawingModel.drawTool);
        assertEquals(1, toolUseCount.get());
        verify(schema).setUseSchemaGrid(false);
        verify(schema).setSchemaGridGap(25);
    }

    private Schema mockSchemaForPanel() {
        Schema schema = mock(Schema.class);
        when(schema.useSchemaGrid()).thenReturn(true);
        when(schema.getSchemaGridGap()).thenReturn(20);
        when(schema.getAllElements()).thenReturn(Collections.emptyList());
        when(schema.getConnectionLines()).thenReturn(Collections.emptyList());
        return schema;
    }
}
