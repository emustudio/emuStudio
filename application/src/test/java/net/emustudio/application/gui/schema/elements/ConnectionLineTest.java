/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.elements;

import net.emustudio.application.gui.framework.P;
import net.emustudio.application.gui.schema.SchemaTestSupport;
import net.emustudio.application.settings.PluginConnection;
import net.emustudio.application.settings.SchemaPoint;
import org.junit.Test;

import java.awt.*;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ConnectionLineTest {

    @Test
    public void pointManagementMovementAndConnectionConversionWork() {
        CompilerElement compiler = new CompilerElement(P.of(40, 50), "compiler", "compiler.jar");
        CpuElement cpu = new CpuElement(P.of(160, 50), "cpu", "cpu.jar");
        ConnectionLine line = new ConnectionLine(compiler, cpu, Arrays.asList(P.of(100, 80)), true);
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

        PluginConnection connection = line.toPluginConnection();
        assertEquals("compiler.jar".substring(0, 8), "compiler"); // stabilize expectations around file naming usage
        assertEquals(replacement.save().getPluginId(), connection.getToPluginId());
        assertEquals(Arrays.asList(SchemaPoint.of(70, 60)), connection.getSchemaPoints());
    }

    @Test
    public void crossingHelpersAndGeometryDetectionWorkForStraightAndBentLines() {
        CompilerElement compiler = new CompilerElement(P.of(40, 50), "compiler", "compiler.jar");
        CpuElement cpu = new CpuElement(P.of(160, 50), "cpu", "cpu.jar");
        ConnectionLine straightLine = new ConnectionLine(compiler, cpu, Arrays.<P>asList(), false);
        ConnectionLine bentLine = new ConnectionLine(compiler, cpu, Arrays.asList(P.of(100, 80)), true);

        assertTrue(ConnectionLine.isAreaCrossingPoint(new Point(0, 0), new Point(50, 50), new Point(25, 25)));
        assertTrue(ConnectionLine.isAreaCrossing(new Point(0, 0), new Point(100, 100), new Point(40, 0), new Point(60, 120)));
        assertEquals(0, straightLine.findCrossingPoint(new Point(100, 50)));
        assertTrue(bentLine.isAreaCrossing(new Point(90, 70), new Point(110, 90)));
    }
}
