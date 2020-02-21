/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.drawing;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SchemaTest {

    private static Graphics graphicsMock;

    @BeforeClass
    public static void setUpClass() {
        graphicsMock = EasyMock.createNiceMock(Graphics.class);
        FontMetrics fontMetricsMock = EasyMock.createNiceMock(FontMetrics.class);
        Rectangle2D rect = EasyMock.createNiceMock(Rectangle2D.class);
        EasyMock.expect(rect.getWidth()).andReturn(0.0);
        EasyMock.expect(fontMetricsMock.getStringBounds(EasyMock.anyObject(String.class),
            EasyMock.eq(graphicsMock))).andReturn(rect).anyTimes();
        Font fontMock = EasyMock.createNiceMock(Font.class);
        EasyMock.expect(fontMock.deriveFont(EasyMock.anyInt())).andReturn(fontMock).anyTimes();
        EasyMock.expect(graphicsMock.getFont()).andReturn(fontMock).anyTimes();

        EasyMock.expect(graphicsMock.getFontMetrics(EasyMock.anyObject(Font.class))).andReturn(fontMetricsMock).anyTimes();

        EasyMock.replay(rect, fontMetricsMock, fontMock, graphicsMock);
    }

    @Test
    public void testGetConfigName() {
        Schema schema = new Schema("test", new Properties());
        Assert.assertEquals("test", schema.getConfigName());
    }

    @Test
    public void testSetConfigName() {
        Schema schema = new Schema();
        schema.setConfigName("test");
        Assert.assertEquals("test", schema.getConfigName());
    }

    @Test
    public void testGetCompilerElement() {
        Schema schema = new Schema();
        CompilerElement compiler = new CompilerElement("compiler", new Properties(), schema);
        schema.setCompilerElement(compiler);
        Assert.assertSame(compiler, schema.getCompilerElement());
    }

    @Test
    public void testGetCpuElement() {
        Schema schema = new Schema();
        CpuElement cpu = new CpuElement("cpu", new Properties(), schema);
        schema.setCpuElement(cpu);
        Assert.assertSame(cpu, schema.getCpuElement());
    }

    @Test
    public void testGetMemoryElement() {
        Schema schema = new Schema();
        MemoryElement memory = new MemoryElement("mem", new Properties(), schema);
        schema.setMemoryElement(memory);
        Assert.assertSame(memory, schema.getMemoryElement());
    }

    @Test
    public void testAddDeviceElement() {
        DeviceElement deviceElement;
        Schema schema = new Schema();
        schema.addDeviceElement(null);
        Assert.assertEquals(0, schema.getDeviceElements().size());

        for (int i = 0; i < 10; i++) {
            deviceElement = new DeviceElement("dev-" + i, new Properties(), schema);
            schema.addDeviceElement(deviceElement);
            Assert.assertEquals(i + 1, schema.getDeviceElements().size());
        }
    }

    @Test
    public void testGetDeviceElements() {
        Schema schema = new Schema();
        DeviceElement deviceElement = new DeviceElement("dev-0", new Properties(), schema);

        schema.addDeviceElement(deviceElement);
        deviceElement = new DeviceElement("dev-1", new Properties(), schema);
        schema.addDeviceElement(deviceElement);
        deviceElement = new DeviceElement("dev-2", new Properties(), schema);
        schema.addDeviceElement(deviceElement);

        List<DeviceElement> result = schema.getDeviceElements();
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("dev-0", result.get(0).getPluginName());
        Assert.assertEquals("dev-1", result.get(1).getPluginName());
        Assert.assertEquals("dev-2", result.get(2).getPluginName());
    }

    @Test
    public void testRemoveDeviceElement() {
        Schema schema = new Schema();
        DeviceElement device = new DeviceElement("dev-0", new Properties(), schema);
        schema.addDeviceElement(device);
        Assert.assertEquals(1, schema.getDeviceElements().size());
        schema.removeDeviceElement(device);
        Assert.assertEquals(0, schema.getDeviceElements().size());
    }

    @Test
    public void testRemoveElement() {
        Schema schema = new Schema();
        CompilerElement elem = new CompilerElement("compiler", new Properties(), schema);
        DeviceElement elem2 = new DeviceElement("dev-0", new Properties(), schema);
        schema.setCompilerElement(elem);
        Assert.assertSame(elem, schema.getCompilerElement());
        schema.addDeviceElement(elem2);
        Assert.assertEquals(1, schema.getDeviceElements().size());
        Assert.assertSame(elem2, schema.getDeviceElements().get(0));
        schema.removeElement(elem);
        Assert.assertNull(schema.getCompilerElement());
        Assert.assertEquals(1, schema.getDeviceElements().size());
        schema.removeElement(elem2);
        Assert.assertEquals(0, schema.getDeviceElements().size());
    }

    @Test
    public void testGetAllElements() {
        Schema schema = new Schema();
        CompilerElement compiler = new CompilerElement("compiler", new Properties(), schema);
        DeviceElement device = new DeviceElement("dev-0", new Properties(), schema);
        schema.setCompilerElement(compiler);
        schema.addDeviceElement(device);

        List<Element> result = schema.getAllElements();
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(compiler));
        Assert.assertTrue(result.contains(device));
    }

    @Test
    public void testAddGetRemoveConnectionLines() {
        Schema schema = new Schema();
        CompilerElement compiler = new CompilerElement("compiler", new Properties(), schema);
        DeviceElement device = new DeviceElement("dev-0", new Properties(), schema);
        MemoryElement memory = new MemoryElement("memory", new Properties(), schema);

        schema.setCompilerElement(compiler);
        schema.addDeviceElement(device);
        schema.setMemoryElement(memory);

        ConnectionLine lin0 = new ConnectionLine(compiler, device, new ArrayList<>(), schema);
        ConnectionLine lin1 = new ConnectionLine(compiler, memory, new ArrayList<>(), schema);
        schema.addConnectionLine(lin0);
        schema.addConnectionLine(lin1);

        List<ConnectionLine> result = schema.getConnectionLines();
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(lin0));
        Assert.assertTrue(result.contains(lin1));

        schema.removeConnectionLine(lin1);
        Assert.assertTrue(schema.getConnectionLines().contains(lin0));
        Assert.assertFalse(schema.getConnectionLines().contains(lin1));

        schema.removeConnectionLine(0);
        Assert.assertFalse(schema.getConnectionLines().contains(lin0));

        Assert.assertEquals(0, schema.getConnectionLines().size());
    }

    @Test
    public void testGetCrossingElement() {
        Schema schema = new Schema();
        Properties props = new Properties();
        props.setProperty("compiler", "compiler");
        props.setProperty("point.x", "100");
        props.setProperty("point.y", "100");
        props.setProperty("width", "100");
        props.setProperty("height", "30");

        CompilerElement elem = new CompilerElement("compiler", props, schema);
        schema.setCompilerElement(elem);
        Assert.assertSame(elem, schema.getCompilerElement());

        Point p;
        Assert.assertNull(schema.getCrossingElement(null));

        p = new Point(110, 110); // somewhere in the middle
        Assert.assertSame(elem, schema.getCrossingElement(p));

        p = new Point(50, 85); // left upper corner
        Assert.assertSame(elem, schema.getCrossingElement(p));

        p = new Point(49, 85); // one pixel left from left upper corner
        Assert.assertNull(schema.getCrossingElement(p));

        p = new Point(150, 85); // right upper corner
        Assert.assertSame(elem, schema.getCrossingElement(p));

        p = new Point(150, 84); // one pixel above right upper corner
        Assert.assertNull(schema.getCrossingElement(p));

        p = new Point(50, 115); // left bottom corner
        Assert.assertSame(elem, schema.getCrossingElement(p));

        p = new Point(49, 115); // one pixel left from left bottom corner
        Assert.assertNull(schema.getCrossingElement(p));

        p = new Point(150, 115); // right bottom corner
        Assert.assertSame(elem, schema.getCrossingElement(p));

        p = new Point(151, 115); // one pixel right from right bottom corner
        Assert.assertNull(schema.getCrossingElement(p));
    }

    @Test
    public void testGetResizeElement() {
        Schema schema = new Schema();
        CompilerElement elem = mockCompiler(schema, 100, 100, 100, 30);
        Assert.assertSame(elem, schema.getCompilerElement());

        Point p;
        Element result = schema.getElementByBorderPoint(null);
        Assert.assertNull(result);

        p = new Point(60, 100); // left border
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(60 - Element.MOUSE_TOLERANCE, 100); // left border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(60 + Element.MOUSE_TOLERANCE, 100); // left border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(60 + Element.MOUSE_TOLERANCE + 1, 100); // left border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(140 + Element.MOUSE_TOLERANCE, 100); // right border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(140 - Element.MOUSE_TOLERANCE, 100); // right border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(140 + Element.MOUSE_TOLERANCE + 1, 100); // right border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(140 - Element.MOUSE_TOLERANCE - 1, 100); // right border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 75 - Element.MOUSE_TOLERANCE); // upper border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(100, 75 + Element.MOUSE_TOLERANCE); // upper border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(100, 75 - Element.MOUSE_TOLERANCE - 1); // upper border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 75 + Element.MOUSE_TOLERANCE + 1); // upper border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 125 - Element.MOUSE_TOLERANCE); // bottom border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(100, 125 + Element.MOUSE_TOLERANCE); // bottom border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertSame(elem, result);

        p = new Point(100, 125 - Element.MOUSE_TOLERANCE - 1); // bottom border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 125 + Element.MOUSE_TOLERANCE + 1); // bottom border with tolerance
        result = schema.getElementByBorderPoint(p);
        Assert.assertNull(result);
    }

    @Test
    public void testGetSetUseGrid() {
        Schema schema = new Schema();
        Assert.assertEquals(true, schema.isGridUsed());

        schema.setUsingGrid(false);
        Assert.assertEquals(false, schema.isGridUsed());
    }

    @Test
    public void testSetGetGridGap() {
        Schema instance = new Schema();
        int expResult = 40;
        instance.setGridGap(expResult);
        int result = instance.getGridGap();
        Assert.assertEquals(expResult, result);

        expResult = 23;
        instance.setGridGap(expResult);
        result = instance.getGridGap();
        Assert.assertEquals(expResult, result);
    }

    @Test
    public void testGetCrossingLine() {
        Schema schema = new Schema();
        CompilerElement compiler = mockCompiler(schema, 100, 100, 100, 30);
        DeviceElement device = mockDevice(schema, 250, 100, 100, 30);
        MemoryElement memory = mockMemory(schema, 150, 200, 100, 30);

        ConnectionLine line0 = mockConnectionLine(compiler, memory, schema);
        ConnectionLine line1 = mockConnectionLine(compiler, device, schema);
        ConnectionLine line2 = mockConnectionLine(device, memory, schema, new Point(250, 200));

        Point p;
        ConnectionLine result = schema.getCrossingLine(null);
        Assert.assertSame(null, result);


        /*             l
         *             i                 width  = 100
         * +---------+ n  +---------+    height = 30
         * |   [A]   | e1 |   [B]   |
         * |    x    |<-->|    x    |
         * | 100,100 |    | 250,100 |
         * +---------+    +---------+   AC = C - A = (50, 100) = (1,2)
         *       ^.[P]         ^         P = A + AC/2 = [125, 150]; AC' = (-150, 125) = (-2,1)
         *   line0 v           |         P'= P + AC' = [123, 151]
         *      +---------+    |         d(P,P') = sqrt(4 + 1) = sqrt(5) < 5 (tolerance)
         *      |   [C]   |    |
         *      |    x    |<---+ line2
         *      | 150,200 | 250,200
         *      +---------+
         */
        p = new Point(125, 150);
        result = schema.getCrossingLine(p);
        Assert.assertSame(line0, result);

        p = new Point(123, 151);
        result = schema.getCrossingLine(p);
        Assert.assertSame(line0, result);

        /*
         * toleranceFactor = TOLERANCE / sqrt(x^2 + y^2); where AC' = (x,y)
         */
        int maxT = (int) (ConnectionLine.TOLERANCE / Math.sqrt(5) + 20);
        ConnectionLine expResult;
        for (int toleranceFactor = -20; toleranceFactor < maxT; toleranceFactor++) {
            p = new Point(125 + toleranceFactor * (-2), 150 + toleranceFactor);

            double a = 200 - 100;
            double b = 100 - 150;
            double c = -a * 100 - b * 100;
            double d = Math.abs(a * p.x + b * p.y + c) / Math.hypot(a, b);

            if (d <= ConnectionLine.TOLERANCE) {
                expResult = line0;
            } else {
                expResult = null;
            }
            result = schema.getCrossingLine(p);
            Assert.assertEquals(expResult, result);
        }

        p = new Point(175, 100);
        result = schema.getCrossingLine(p);
        Assert.assertSame(line1, result);

        p = new Point(175, 100 + ConnectionLine.TOLERANCE);
        result = schema.getCrossingLine(p);
        Assert.assertSame(line1, result);

        p = new Point(175, 100 - ConnectionLine.TOLERANCE);
        result = schema.getCrossingLine(p);
        Assert.assertSame(line1, result);

        p = new Point(250 + ConnectionLine.TOLERANCE, 200);
        result = schema.getCrossingLine(p);
        Assert.assertSame(line2, result);

        p = new Point(250 + ConnectionLine.TOLERANCE + 1, 200);
        result = schema.getCrossingLine(p);
        Assert.assertNull(result);

        p = new Point(250 + ConnectionLine.TOLERANCE + 20, 200);
        result = schema.getCrossingLine(p);
        Assert.assertNull(result);

        schema.destroy();
        Assert.assertEquals(0, schema.getAllElements().size());
        Assert.assertNull(schema.getCompilerElement());
        Assert.assertNull(schema.getMemoryElement());
        Assert.assertNull(schema.getCpuElement());
        Assert.assertEquals(0, schema.getConnectionLines().size());

    }

    @Test
    public void testSelectElements() {
        Schema instance = new Schema();
        CompilerElement compiler = mockCompiler(instance, 100, 100, 100, 30);
        DeviceElement device = mockDevice(instance, 250, 100, 100, 30);
        MemoryElement memory = mockMemory(instance, 150, 200, 100, 30);

        ConnectionLine line0 = mockConnectionLine(compiler, memory, instance);
        ConnectionLine line1 = mockConnectionLine(compiler, device, instance);
        ConnectionLine line2 = mockConnectionLine(device, memory, instance, new Point(250, 200));

        int x = 50;
        int y = 20;
        int width = 300;
        int height = 40;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 50;
        y = 20;
        width = 300;
        height = 65;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(compiler.isSelected());
        Assert.assertTrue(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 49;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 60;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 99;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 140;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertTrue(memory.isSelected());
        Assert.assertTrue(line0.isSelected());
        Assert.assertTrue(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 230;
        y = 190;
        width = 25;
        height = 20;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 230;
        y = 190;
        width = 19;
        height = 20;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 230;
        y = 190;
        width = 20;
        height = 20;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 0;
        y = 0;
        width = 300;
        height = 200;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(compiler.isSelected());
        Assert.assertTrue(device.isSelected());
        Assert.assertTrue(memory.isSelected());
        Assert.assertTrue(line0.isSelected());
        Assert.assertTrue(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 160;
        y = 90;
        width = 10;
        height = 11;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertTrue(line1.isSelected());
        Assert.assertFalse(line2.isSelected());
    }

    @Test
    public void testMoveSelection() {
        Schema schema = new Schema();
        CompilerElement compiler = mockCompiler(schema, 100, 100, 100, 30);
        DeviceElement device = mockDevice(schema, 250, 100, 100, 30);
        MemoryElement memory = mockMemory(schema, 150, 200, 100, 30);

        ConnectionLine line0 = mockConnectionLine(compiler, memory, schema);
        ConnectionLine line1 = mockConnectionLine(compiler, device, schema);
        ConnectionLine line2 = mockConnectionLine(device, memory, schema, new Point(250, 200));

        int x = 0;
        int y = 90;
        int width = 100;
        int height = 30;
        schema.selectElements(x, y, width, height);
        Assert.assertTrue(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertFalse(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        // left
        schema.moveSelection(-10, 0);
        Assert.assertEquals(90, compiler.getX());

        // right
        Assert.assertTrue(schema.moveSelection(10, 0));
        Assert.assertEquals(100, compiler.getX());
        Assert.assertFalse(schema.moveSelection(71, 0));
        Assert.assertEquals(100, compiler.getX()); // move fails
        Assert.assertFalse(schema.moveSelection(70, 0));
        Assert.assertEquals(100, compiler.getX()); // move fails
        Assert.assertTrue(schema.moveSelection(69, 0));
        Assert.assertEquals(169, compiler.getX()); // move succeeds

        // down
        Assert.assertFalse(schema.moveSelection(0, 70));
        Assert.assertEquals(100, compiler.getY());
        Assert.assertTrue(schema.moveSelection(0, 39));
        Assert.assertEquals(139, compiler.getY());

        // up
        Assert.assertTrue(schema.moveSelection(0, -39));
        Assert.assertEquals(100, compiler.getY());

        // back left
        Assert.assertTrue(schema.moveSelection(-69, 0));
        Assert.assertEquals(100, compiler.getX());

        // test left boundary
        Assert.assertTrue(schema.moveSelection(-60 + Schema.MIN_LEFT_MARGIN + 1, 0));
        Assert.assertEquals(40 + Schema.MIN_LEFT_MARGIN + 1, compiler.getX());
        Assert.assertTrue(schema.moveSelection(60 - Schema.MIN_LEFT_MARGIN - 1, 0));
        Assert.assertEquals(100, compiler.getX());
        Assert.assertTrue(schema.moveSelection(-60 + Schema.MIN_LEFT_MARGIN, 0));
        Assert.assertEquals(40 + Schema.MIN_LEFT_MARGIN, compiler.getX());
        Assert.assertTrue(schema.moveSelection(60 - Schema.MIN_LEFT_MARGIN, 0));
        Assert.assertEquals(100, compiler.getX());
        Assert.assertFalse(schema.moveSelection(-60 + Schema.MIN_LEFT_MARGIN - 1, 0));
        Assert.assertEquals(100, compiler.getX());

        // test top boundary
        Assert.assertTrue(schema.moveSelection(0, -75 + Schema.MIN_TOP_MARGIN + 1));
        Assert.assertEquals(25 + Schema.MIN_TOP_MARGIN + 1, compiler.getY());
        Assert.assertTrue(schema.moveSelection(0, 75 - Schema.MIN_TOP_MARGIN - 1));
        Assert.assertEquals(100, compiler.getY());
        Assert.assertTrue(schema.moveSelection(0, -75 + Schema.MIN_TOP_MARGIN));
        Assert.assertEquals(25 + Schema.MIN_TOP_MARGIN, compiler.getY());
        Assert.assertTrue(schema.moveSelection(0, 75 - Schema.MIN_TOP_MARGIN));
        Assert.assertEquals(100, compiler.getY());
        Assert.assertFalse(schema.moveSelection(0, -75 + Schema.MIN_TOP_MARGIN - 1));
        Assert.assertEquals(100, compiler.getY());

        /*             l
         *             i                 width  = 100
         * +---------+ n  +---------+    height = 30
         * |   [A]   | e1 |   [B]   |
         * |    x    |<-->|    x    |
         * | 100,100 |    | 250,100 |
         * +---------+    +---------+   AC = C - A = (50, 100) = (1,2)
         *       ^.[P]         ^         P = A + AC/2 = [125, 150]; AC' = (-150, 125) = (-2,1)
         *   line0 v           |         P'= P + AC' = [123, 151]
         *      +---------+    |         d(P,P') = sqrt(4 + 1) = sqrt(5) < 5 (tolerance)
         *      |   [C]   |    |
         *      |    x    |<---+ line2
         *      | 150,200 | 250,200
         *      +---------+
         */

        // now test elem3

        // selection
        x = 80;
        y = 180;
        width = 100;
        height = 50;

        schema.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertTrue(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        // down
        Assert.assertTrue(schema.moveSelection(0, 100));
        Assert.assertEquals(300, memory.getY());

        // up
        Assert.assertTrue(schema.moveSelection(0, -100));
        Assert.assertEquals(200, memory.getY());

        // right
        Assert.assertTrue(schema.moveSelection(59, 0));
        Assert.assertEquals(209, memory.getX());

        Assert.assertFalse(schema.moveSelection(1, 0));
        Assert.assertEquals(209, memory.getX()); // move failed
        Assert.assertFalse(schema.moveSelection(20, 0));
        Assert.assertEquals(209, memory.getX()); // move failed
        Assert.assertTrue(schema.moveSelection(-1, 0));
        Assert.assertEquals(208, memory.getX());
        Assert.assertTrue(schema.moveSelection(-48, 0));
        Assert.assertEquals(160, memory.getX());

        // up
        Assert.assertFalse(schema.moveSelection(0, -100));
        Assert.assertEquals(200, memory.getY()); // move failed
        Assert.assertFalse(schema.moveSelection(0, -80));
        Assert.assertEquals(200, memory.getY()); // move failed
        Assert.assertFalse(schema.moveSelection(100, 0));
        Assert.assertEquals(160, memory.getX()); // move failed
        Assert.assertTrue(schema.moveSelection(1, 0));
        Assert.assertEquals(161, memory.getX());
        Assert.assertTrue(schema.moveSelection(-1, 0));
        Assert.assertEquals(160, memory.getX());

        // test multiple selection
        x = 0;
        y = 0;
        width = 149;
        height = 200;
        schema.selectElements(x, y, width, height);
        Assert.assertTrue(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertTrue(memory.isSelected());
        Assert.assertTrue(line0.isSelected());
        Assert.assertTrue(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        Assert.assertTrue(schema.moveSelection(10, 0));
        Assert.assertEquals(110, compiler.getX());
        Assert.assertEquals(170, memory.getX());

        Assert.assertTrue(schema.moveSelection(-10, 0));
        Assert.assertEquals(100, compiler.getX());
        Assert.assertEquals(160, memory.getX());

        Assert.assertTrue(schema.moveSelection(-50 + Schema.MIN_LEFT_MARGIN, 0));
        Assert.assertEquals(50 + Schema.MIN_LEFT_MARGIN, compiler.getX());
        Assert.assertEquals(110 + Schema.MIN_LEFT_MARGIN, memory.getX());

        Assert.assertFalse(schema.moveSelection(-11, 0));
        Assert.assertEquals(50 + Schema.MIN_LEFT_MARGIN, compiler.getX());
        Assert.assertEquals(110 + Schema.MIN_LEFT_MARGIN, memory.getX());
    }

    @Test
    public void testDeleteSelected() {
        Schema schema = new Schema();
        CompilerElement compiler = mockCompiler(schema, 100, 100, 100, 30);
        DeviceElement device = mockDevice(schema, 250, 100, 100, 30);
        MemoryElement memory = mockMemory(schema, 150, 200, 100, 30);

        ConnectionLine line0 = mockConnectionLine(compiler, memory, schema);
        ConnectionLine line1 = mockConnectionLine(compiler, device, schema);
        ConnectionLine line2 = mockConnectionLine(device, memory, schema, new Point(250, 200));

        int x = 150;
        int y = 200;
        int width = 10;
        int height = 10;
        schema.selectElements(x, y, width, height);
        Assert.assertFalse(compiler.isSelected());
        Assert.assertFalse(device.isSelected());
        Assert.assertTrue(memory.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        schema.deleteSelected();
        Assert.assertEquals(1, schema.getConnectionLines().size());
        Assert.assertEquals(2, schema.getAllElements().size());
        Assert.assertSame(line1, schema.getConnectionLines().get(0));
        Assert.assertSame(compiler, schema.getCompilerElement());
        Assert.assertNull(schema.getMemoryElement());
        Assert.assertSame(device, schema.getDeviceElements().get(0));
    }

    @Test
    public void testSaveAndGetSettings() {
        Schema schema = new Schema();
        Properties result = schema.getSettings();
        Assert.assertNotNull(result);

        Properties compilerProps = new Properties();
        compilerProps.setProperty("compiler", "compiler");
        compilerProps.setProperty("point.x", "100");
        compilerProps.setProperty("point.y", "100");
        compilerProps.setProperty("width", "100");
        compilerProps.setProperty("height", "30");
        CompilerElement elem = new CompilerElement("compiler", compilerProps, schema);
        schema.setCompilerElement(elem);

        Assert.assertNull(schema.getSettings().getProperty("compiler"));
        schema.save();

        Properties settings = schema.getSettings();
        Assert.assertEquals("compiler", settings.getProperty("compiler"));
        Assert.assertEquals("100", settings.getProperty("compiler.point.x"));
        Assert.assertEquals("100", settings.getProperty("compiler.point.y"));
        Assert.assertEquals("100", settings.getProperty("compiler.width"));
        Assert.assertEquals("30", settings.getProperty("compiler.height"));
    }

    private CompilerElement mockCompiler(Schema instance, int x, int y, int width, int height) throws NumberFormatException {
        Properties props = mockProperties("compiler", x, y, width, height);
        CompilerElement compiler = new CompilerElement("compiler", props, instance);
        compiler.measure(graphicsMock);
        instance.setCompilerElement(compiler);

        return compiler;
    }


    private MemoryElement mockMemory(Schema instance, int x, int y, int width, int height) throws NumberFormatException {
        Properties props = mockProperties("memory", x, y, width, height);
        MemoryElement memory = new MemoryElement("memory", props, instance);
        memory.measure(graphicsMock);
        instance.setMemoryElement(memory);
        return memory;
    }

    private ConnectionLine mockConnectionLine(Element elem2, Element elem3, Schema instance, Point... points) {
        ConnectionLine line = new ConnectionLine(elem2, elem3, Arrays.asList(points), instance);
        instance.addConnectionLine(line);
        return line;
    }

    private DeviceElement mockDevice(Schema instance, int x, int y, int width, int height) throws NumberFormatException {
        Properties props = mockProperties("dev-0", x, y, width, height);
        DeviceElement device = new DeviceElement("dev-0", props, instance);
        device.measure(graphicsMock);
        instance.addDeviceElement(device);
        return device;
    }

    private Properties mockProperties(String name, int x, int y, int width, int height) {
        Properties props = new Properties();
        props.setProperty(name, name);
        props.setProperty("point.x", String.valueOf(x));
        props.setProperty("point.y", String.valueOf(y));
        props.setProperty("width", String.valueOf(width));
        props.setProperty("height", String.valueOf(height));
        return props;
    }

}
