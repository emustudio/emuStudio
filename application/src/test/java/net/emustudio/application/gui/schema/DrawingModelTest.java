/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.components.P;
import net.emustudio.application.gui.schema.elements.CompilerElement;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.CpuElement;
import org.junit.Test;

import java.awt.*;
import java.util.Arrays;

import static org.junit.Assert.*;

public class DrawingModelTest {

    @Test
    public void clearResetsTransientDrawingState() {
        DrawingModel model = new DrawingModel();
        CompilerElement compiler = new CompilerElement(P.of(30, 30), "compiler", "compiler.jar");
        CpuElement cpu = new CpuElement(P.of(90, 30), "cpu", "cpu.jar");
        ConnectionLine line = new ConnectionLine(compiler, cpu, Arrays.asList(P.of(60, 40)), true);

        model.drawTool = DrawingPanel.Tool.TOOL_DEVICE;
        model.pluginFileName = "device.jar";
        model.tmpElem1 = compiler;
        model.tmpElem2 = cpu;
        model.tmpPoints.add(P.of(70, 80));
        model.selectionStart = new Point(1, 2);
        model.selectionEnd = new Point(3, 4);
        model.selectedPoint = P.of(5, 6);
        model.selectedLine = line;

        model.clear();

        assertEquals(DrawingPanel.Tool.TOOL_NOTHING, model.drawTool);
        assertNull(model.pluginFileName);
        assertNull(model.tmpElem1);
        assertNull(model.tmpElem2);
        assertTrue(model.tmpPoints.isEmpty());
        assertNull(model.selectionStart);
        assertNull(model.selectionEnd);
        assertNull(model.selectedPoint);
        assertNull(model.selectedLine);
    }
}
