/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.elements;

import net.emustudio.application.gui.components.P;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaTestSupport;
import net.emustudio.application.settings.PluginConnection;
import net.emustudio.application.settings.SchemaPoint;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ConnectionLineTest {

    @Test
    public void pointManagementMovementAndConnectionConversionWork() {
        CompilerElement compiler = new CompilerElement(P.of(40, 50), "compiler", "compiler.jar");
        CpuElement cpu = new CpuElement(P.of(160, 50), "cpu", "cpu.jar");
        ConnectionLine line = new ConnectionLine(compiler, cpu, List.of(P.of(100, 80)), true);
        P point = line.getPoints().get(0);

        assertSame(point, line.findPoint(new Point(100, 80)));
        assertTrue(line.isAreaCrossingPoint(new Point(95, 75), new Point(105, 85)));

        line.movePoint(point, P.of(110, 90));
        assertEquals(110, point.ix());
        assertEquals(90, point.iy());

        line.moveAllPoints(10, 10, new java.util.function.Function<P, P>() {
            @Override
            public P apply(P p) {
                return P.of(p.ix(), p.iy());
            }
        });
        assertEquals(120, point.ix());
        assertEquals(100, point.iy());

        line.addPoint(0, P.of(70, 60));
        assertEquals(2, line.getPoints().size());
        line.removePoint(point);
        assertEquals(1, line.getPoints().size());
        assertTrue(line.containsElement(compiler));

        DeviceElement replacement = new DeviceElement(P.of(200, 60), "device", "device.jar");
        line.replaceElement(cpu, replacement);
        CompilerElement replacementCompiler = new CompilerElement(P.of(30, 60), "replacement", "replacement.jar");
        line.replaceElement(compiler, replacementCompiler);

        PluginConnection connection = line.toPluginConnection();
        assertEquals(replacementCompiler.save().getPluginId(), connection.getFromPluginId());
        assertEquals(replacement.save().getPluginId(), connection.getToPluginId());
        assertEquals(List.of(SchemaPoint.of(70, 60)), connection.getSchemaPoints());
    }

    @Test
    public void crossingHelpersAndGeometryDetectionWorkForStraightAndBentLines() {
        CompilerElement compiler = new CompilerElement(P.of(40, 50), "compiler", "compiler.jar");
        CpuElement cpu = new CpuElement(P.of(160, 50), "cpu", "cpu.jar");
        ConnectionLine straightLine = new ConnectionLine(compiler, cpu, List.<P>of(), false);
        ConnectionLine bentLine = new ConnectionLine(compiler, cpu, List.of(P.of(100, 80)), true);

        assertTrue(ConnectionLine.isAreaCrossingPoint(new Point(0, 0), new Point(50, 50), new Point(25, 25)));
        assertTrue(ConnectionLine.isAreaCrossing(new Point(0, 0), new Point(100, 100), new Point(40, 0), new Point(60, 120)));
        assertEquals(0, straightLine.findCrossingPoint(new Point(100, 50)));
        assertTrue(bentLine.isAreaCrossing(new Point(90, 70), new Point(110, 90)));
    }

    @Test
    public void drawingComputesExpectedArrowHeadsForOneWayAndTwoWayLines() throws Exception {
        CompilerElement compiler = new CompilerElement(P.of(40, 50), "compiler", "compiler.jar");
        CpuElement cpu = new CpuElement(P.of(160, 50), "cpu", "cpu.jar");
        ConnectionLine oneWay = new ConnectionLine(compiler, cpu, Collections.emptyList(), false);
        ConnectionLine twoWay = new ConnectionLine(compiler, cpu, Collections.singletonList(P.of(100, 80)), true);
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        oneWay.setSelected(true);
        assertTrue(oneWay.isSelected());
        oneWay.draw(graphics, false);
        twoWay.draw(graphics, true);

        assertNull(SchemaTestSupport.getField(oneWay, "arrow1LeftEnd", P.class));
        assertNull(SchemaTestSupport.getField(oneWay, "arrow1RightEnd", P.class));
        assertNotNull(SchemaTestSupport.getField(oneWay, "arrow2LeftEnd", P.class));
        assertNotNull(SchemaTestSupport.getField(oneWay, "arrow2RightEnd", P.class));

        assertNotNull(SchemaTestSupport.getField(twoWay, "arrow1LeftEnd", P.class));
        assertNotNull(SchemaTestSupport.getField(twoWay, "arrow1RightEnd", P.class));
        assertNotNull(SchemaTestSupport.getField(twoWay, "arrow2LeftEnd", P.class));
        assertNotNull(SchemaTestSupport.getField(twoWay, "arrow2RightEnd", P.class));

        ConnectionLine.drawSketch(graphics, compiler, new Point(200, 100), Arrays.asList(P.of(-10, -10), P.of(70, 60)));
        ConnectionLine.highlightPoint(P.of(70, 60), graphics);

        assertNotEquals(0, image.getRGB(Schema.MIN_LEFT_MARGIN, Schema.MIN_TOP_MARGIN));
        assertNotEquals(0, image.getRGB(70, 60));
    }

    @Test
    public void crossingPointLookupHandlesTailSegmentsMissesAndDegenerateLines() {
        CompilerElement compiler = new CompilerElement(P.of(40, 50), "compiler", "compiler.jar");
        CpuElement cpu = new CpuElement(P.of(160, 50), "cpu", "cpu.jar");
        ConnectionLine bentLine = new ConnectionLine(compiler, cpu, Collections.singletonList(P.of(100, 50)), false);

        assertEquals(1, bentLine.findCrossingPoint(new Point(110, 50)));
        assertEquals(-1, bentLine.findCrossingPoint(new Point(130, 90)));
        assertEquals(-1, bentLine.findCrossingPoint(null));
        assertFalse(bentLine.isAreaCrossing(new Point(0, 0), new Point(10, 10)));

        P original = bentLine.getPoints().get(0).copy();
        bentLine.movePoint(P.of(999, 999), P.of(1, 1));
        assertEquals(original.ix(), bentLine.getPoints().get(0).ix());
        assertEquals(original.iy(), bentLine.getPoints().get(0).iy());

        CompilerElement overlappingCompiler = new CompilerElement(P.of(40, 50), "compiler", "compiler.jar");
        CpuElement overlappingCpu = new CpuElement(P.of(40, 50), "cpu", "cpu.jar");
        ConnectionLine degenerateLine = new ConnectionLine(overlappingCompiler, overlappingCpu, Collections.emptyList(), true);

        assertFalse(degenerateLine.isAreaCrossing(new Point(30, 30), new Point(60, 60)));
        assertEquals(-1, degenerateLine.findCrossingPoint(new Point(40, 50)));
    }
}
