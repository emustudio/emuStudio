/*
 * Copyright (C) 2012-2015, Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.drawing;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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
        Schema instance = new Schema("test", new Properties());
        String expResult = "test";
        String result = instance.getConfigName();
        Assert.assertEquals(expResult, result);
    }

    @Test
    public void testSetConfigName() {
        String cName = "test";
        Schema instance = new Schema();
        instance.setConfigName(cName);
        Assert.assertEquals(cName, instance.getConfigName());
    }

    @Test
    public void testGetCompilerElement() {
        Schema instance = new Schema();
        CompilerElement expResult = new CompilerElement("compiler", new Properties(), instance);
        instance.setCompilerElement(expResult);
        CompilerElement result = instance.getCompilerElement();
        Assert.assertSame(expResult, result);
    }

    @Test
    public void testGetCpuElement() {
        Schema instance = new Schema();
        CpuElement expResult = new CpuElement("cpu", new Properties(), instance);
        instance.setCpuElement(expResult);
        CpuElement result = instance.getCpuElement();
        Assert.assertSame(expResult, result);
    }

    @Test
    public void testGetMemoryElement() {
        Schema instance = new Schema();
        MemoryElement expResult = new MemoryElement("mem", new Properties(), instance);
        instance.setMemoryElement(expResult);
        MemoryElement result = instance.getMemoryElement();
        Assert.assertSame(expResult, result);
    }

    @Test
    public void testAddDeviceElement() {
        DeviceElement deviceElement = null;
        Schema instance = new Schema();
        instance.addDeviceElement(deviceElement);
        Assert.assertEquals(0, instance.getDeviceElements().size());

        for (int i = 0; i < 10; i++) {
            deviceElement = new DeviceElement("dev-" + i, new Properties(), instance);
            instance.addDeviceElement(deviceElement);
            Assert.assertEquals(i + 1, instance.getDeviceElements().size());
        }
    }

    @Test
    public void testGetDeviceElements() {
        Schema instance = new Schema();
        DeviceElement deviceElement = new DeviceElement("dev-0", new Properties(), instance);

        instance.addDeviceElement(deviceElement);
        deviceElement = new DeviceElement("dev-1", new Properties(), instance);
        instance.addDeviceElement(deviceElement);
        deviceElement = new DeviceElement("dev-2", new Properties(), instance);
        instance.addDeviceElement(deviceElement);

        List<DeviceElement> result = instance.getDeviceElements();
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("dev-0", result.get(0).getPluginName());
        Assert.assertEquals("dev-1", result.get(1).getPluginName());
        Assert.assertEquals("dev-2", result.get(2).getPluginName());
    }

    @Test
    public void testRemoveDeviceElement() {
        Schema instance = new Schema();
        DeviceElement device = new DeviceElement("dev-0", new Properties(), instance);
        instance.addDeviceElement(device);
        Assert.assertEquals(1, instance.getDeviceElements().size());
        instance.removeDeviceElement(device);
        Assert.assertEquals(0, instance.getDeviceElements().size());
    }

    @Test
    public void testRemoveElement() {
        Schema instance = new Schema();
        CompilerElement elem = new CompilerElement("compiler", new Properties(), instance);
        DeviceElement elem2 = new DeviceElement("dev-0", new Properties(), instance);
        instance.setCompilerElement(elem);
        Assert.assertSame(elem, instance.getCompilerElement());
        instance.addDeviceElement(elem2);
        Assert.assertEquals(1, instance.getDeviceElements().size());
        Assert.assertSame(elem2, instance.getDeviceElements().get(0));
        instance.removeElement(elem);
        Assert.assertNull(instance.getCompilerElement());
        Assert.assertEquals(1, instance.getDeviceElements().size());
        instance.removeElement(elem2);
        Assert.assertEquals(0, instance.getDeviceElements().size());
    }

    @Test
    public void testGetAllElements() {
        Schema instance = new Schema();
        CompilerElement elem = new CompilerElement("compiler", new Properties(), instance);
        DeviceElement elem2 = new DeviceElement("dev-0", new Properties(), instance);
        instance.setCompilerElement(elem);
        instance.addDeviceElement(elem2);

        List<Element> result = instance.getAllElements();
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(elem));
        Assert.assertTrue(result.contains(elem2));
    }

    @Test
    public void testAddGetRemoveConnectionLines() {
        Schema instance = new Schema();
        CompilerElement elem = new CompilerElement("compiler", new Properties(), instance);
        DeviceElement elem2 = new DeviceElement("dev-0", new Properties(), instance);
        MemoryElement elem3 = new MemoryElement("memory", new Properties(), instance);
        instance.setCompilerElement(elem);
        instance.addDeviceElement(elem2);
        instance.setMemoryElement(elem3);
        ConnectionLine lin0 = new ConnectionLine(elem, elem2, new ArrayList<Point>(), instance);
        ConnectionLine lin1 = new ConnectionLine(elem, elem3, new ArrayList<Point>(), instance);
        instance.addConnectionLine(lin0);
        instance.addConnectionLine(lin1);
        List<ConnectionLine> result = instance.getConnectionLines();
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(lin0));
        Assert.assertTrue(result.contains(lin1));

        instance.removeConnectionLine(lin1);
        Assert.assertTrue(instance.getConnectionLines().contains(lin0));
        Assert.assertFalse(instance.getConnectionLines().contains(lin1));

        instance.removeConnectionLine(0);
        Assert.assertFalse(instance.getConnectionLines().contains(lin0));

        Assert.assertEquals(0, instance.getConnectionLines().size());
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

        Point p = null;
        Assert.assertNull(schema.getCrossingElement(p));

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
        Schema instance = new Schema();
        Properties props = new Properties();
        props.setProperty("compiler", "compiler");
        props.setProperty("point.x", "100");
        props.setProperty("point.y", "100");
        props.setProperty("width", "100");
        props.setProperty("height", "30");

        CompilerElement elem = new CompilerElement("compiler", props, instance);
        elem.measure(graphicsMock);
        instance.setCompilerElement(elem);
        Assert.assertSame(elem, instance.getCompilerElement());

        Point p = null;
        Element result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(50, 100); // left border
        Element expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(50 - Element.MOUSE_TOLERANCE, 100); // left border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(50 + Element.MOUSE_TOLERANCE, 100); // left border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(50 + Element.MOUSE_TOLERANCE + 1, 100); // left border with tolerance
        result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(150 + Element.MOUSE_TOLERANCE, 100); // right border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(150 - Element.MOUSE_TOLERANCE, 100); // right border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(150 + Element.MOUSE_TOLERANCE + 1, 100); // right border with tolerance
        result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(150 - Element.MOUSE_TOLERANCE - 1, 100); // right border with tolerance
        result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 85 - Element.MOUSE_TOLERANCE); // upper border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(100, 85 + Element.MOUSE_TOLERANCE); // upper border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(100, 85 - Element.MOUSE_TOLERANCE - 1); // upper border with tolerance
        result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 85 + Element.MOUSE_TOLERANCE + 1); // upper border with tolerance
        result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 115 - Element.MOUSE_TOLERANCE); // bottom border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(100, 115 + Element.MOUSE_TOLERANCE); // bottom border with tolerance
        expResult = elem;
        result = instance.getElementByBorderPoint(p);
        Assert.assertSame(expResult, result);

        p = new Point(100, 115 - Element.MOUSE_TOLERANCE - 1); // bottom border with tolerance
        result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);

        p = new Point(100, 115 + Element.MOUSE_TOLERANCE + 1); // bottom border with tolerance
        result = instance.getElementByBorderPoint(p);
        Assert.assertNull(result);
    }

    @Test
    public void testGetSetUseGrid() {
        Schema instance = new Schema();
        boolean expResult = true;
        boolean result = instance.isGridUsed();
        Assert.assertEquals(expResult, result);

        instance.setUsingGrid(false);
        Assert.assertEquals(false, instance.isGridUsed());
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
        Schema instance = new Schema();
        Properties compilerProps = new Properties();
        compilerProps.setProperty("compiler", "compiler");
        compilerProps.setProperty("point.x", "100");
        compilerProps.setProperty("point.y", "100");
        compilerProps.setProperty("width", "100");
        compilerProps.setProperty("height", "30");
        CompilerElement elem = new CompilerElement("compiler", compilerProps, instance);
        elem.measure(graphicsMock);
        instance.setCompilerElement(elem);

        Properties deviceProps = new Properties();
        deviceProps.setProperty("dev-0", "dev-0");
        deviceProps.setProperty("point.x", "250");
        deviceProps.setProperty("point.y", "100");
        deviceProps.setProperty("width", "100");
        deviceProps.setProperty("height", "30");

        DeviceElement elem2 = new DeviceElement("dev-0", deviceProps, instance);
        elem2.measure(graphicsMock);
        instance.addDeviceElement(elem2);

        Properties memProps = new Properties();
        memProps.setProperty("memory", "memory");
        memProps.setProperty("point.x", "150");
        memProps.setProperty("point.y", "200");
        memProps.setProperty("width", "100");
        memProps.setProperty("height", "30");
        MemoryElement elem3 = new MemoryElement("memory", memProps, instance);
        elem3.measure(graphicsMock);
        instance.setMemoryElement(elem3);

        ConnectionLine line0 = new ConnectionLine(elem, elem3, new ArrayList(), instance);
        instance.addConnectionLine(line0);
        ConnectionLine line1 = new ConnectionLine(elem, elem2, new ArrayList(), instance);
        instance.addConnectionLine(line1);

        List<Point> points = new ArrayList<Point>();
        points.add(new Point(250,200));
        ConnectionLine line2 = new ConnectionLine(elem2, elem3, points, instance);
        instance.addConnectionLine(line2);

        Point p = null;
        ConnectionLine expResult = null;
        ConnectionLine result = instance.getCrossingLine(p);
        Assert.assertSame(expResult, result);


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
        expResult = line0;
        result = instance.getCrossingLine(p);
        Assert.assertSame(expResult, result);

        p = new Point(123, 151);
        expResult = line0;
        result = instance.getCrossingLine(p);
        Assert.assertSame(expResult, result);

        /*
         * toleranceFactor = TOLERANCE / sqrt(x^2 + y^2); where AC' = (x,y)
         */
        int maxT = (int)(ConnectionLine.TOLERANCE / Math.sqrt(5)+20);
        for (int toleranceFactor = -20; toleranceFactor < maxT; toleranceFactor++) {
            p = new Point((int) (125 + toleranceFactor * (-2)), (int) (150 + toleranceFactor * 1));

            double a = 200 - 100;
            double b = 100 - 150;
            double c = -a * 100 - b * 100;
            double d = Math.abs(a * p.x + b * p.y + c) / Math.hypot(a, b);

            if (d <= ConnectionLine.TOLERANCE) {
                expResult = line0;
            } else {
                expResult = null;
            }
            result = instance.getCrossingLine(p);
            Assert.assertEquals(expResult, result);
        }

        p = new Point(175, 100);
        expResult = line1;
        result = instance.getCrossingLine(p);
        Assert.assertSame(expResult, result);

        p = new Point(175, 100 + ConnectionLine.TOLERANCE);
        expResult = line1;
        result = instance.getCrossingLine(p);
        Assert.assertSame(expResult, result);

        p = new Point(175, 100 - ConnectionLine.TOLERANCE);
        expResult = line1;
        result = instance.getCrossingLine(p);
        Assert.assertSame(expResult, result);

        p = new Point(250 + ConnectionLine.TOLERANCE, 200);
        expResult = line2;
        result = instance.getCrossingLine(p);
        Assert.assertSame(expResult, result);

        p = new Point(250 + ConnectionLine.TOLERANCE + 1, 200);
        result = instance.getCrossingLine(p);
        Assert.assertNull(result);

        p = new Point(250 + ConnectionLine.TOLERANCE + 20, 200);
        result = instance.getCrossingLine(p);
        Assert.assertNull(result);

        instance.destroy();
        Assert.assertEquals(0, instance.getAllElements().size());
        Assert.assertNull(instance.getCompilerElement());
        Assert.assertNull(instance.getMemoryElement());
        Assert.assertNull(instance.getCpuElement());
        Assert.assertEquals(0, instance.getConnectionLines().size());

    }

    @Test
    public void testSelectElements() {
        Schema instance = new Schema();
        Properties compilerProps = new Properties();
        compilerProps.setProperty("compiler", "compiler");
        compilerProps.setProperty("point.x", "100");
        compilerProps.setProperty("point.y", "100");
        compilerProps.setProperty("width", "100");
        compilerProps.setProperty("height", "30");
        CompilerElement elem = new CompilerElement("compiler", compilerProps, instance);
        elem.measure(graphicsMock);
        instance.setCompilerElement(elem);

        Properties deviceProps = new Properties();
        deviceProps.setProperty("dev-0", "dev-0");
        deviceProps.setProperty("point.x", "250");
        deviceProps.setProperty("point.y", "100");
        deviceProps.setProperty("width", "100");
        deviceProps.setProperty("height", "30");

        DeviceElement elem2 = new DeviceElement("dev-0", deviceProps, instance);
        elem2.measure(graphicsMock);
        instance.addDeviceElement(elem2);

        Properties memProps = new Properties();
        memProps.setProperty("memory", "memory");
        memProps.setProperty("point.x", "150");
        memProps.setProperty("point.y", "200");
        memProps.setProperty("width", "100");
        memProps.setProperty("height", "30");
        MemoryElement elem3 = new MemoryElement("memory", memProps, instance);
        elem3.measure(graphicsMock);
        instance.setMemoryElement(elem3);

        ConnectionLine line0 = new ConnectionLine(elem, elem3, new ArrayList(), instance);
        instance.addConnectionLine(line0);
        ConnectionLine line1 = new ConnectionLine(elem, elem2, new ArrayList(), instance);
        instance.addConnectionLine(line1);

        List<Point> points = new ArrayList<Point>();
        points.add(new Point(250,200));
        ConnectionLine line2 = new ConnectionLine(elem2, elem3, points, instance);
        instance.addConnectionLine(line2);

        int x = 50;
        int y = 20;
        int width = 300;
        int height = 60;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 50;
        y = 20;
        width = 300;
        height = 65;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertTrue(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 49;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 50;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 99;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 100;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertTrue(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 0;
        y = 100;
        width = 100;
        height = 100;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertTrue(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        x = 230;
        y = 190;
        width = 25;
        height = 20;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 230;
        y = 190;
        width = 19;
        height = 20;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 230;
        y = 190;
        width = 20;
        height = 20;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 0;
        y = 0;
        width = 300;
        height = 200;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertTrue(elem2.isSelected());
        Assert.assertTrue(elem3.isSelected());
        Assert.assertTrue(line0.isSelected());
        Assert.assertTrue(line1.isSelected());
        Assert.assertTrue(line2.isSelected());

        x = 160;
        y = 90;
        width = 10;
        height = 11;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertTrue(line1.isSelected());
        Assert.assertFalse(line2.isSelected());
    }

    @Test
    public void testMoveSelection() {
        Schema instance = new Schema();
        Properties compilerProps = new Properties();
        compilerProps.setProperty("compiler", "compiler");
        compilerProps.setProperty("point.x", "100");
        compilerProps.setProperty("point.y", "100");
        compilerProps.setProperty("width", "100");
        compilerProps.setProperty("height", "30");
        CompilerElement elem = new CompilerElement("compiler", compilerProps, instance);
        elem.measure(graphicsMock);
        instance.setCompilerElement(elem);

        Properties deviceProps = new Properties();
        deviceProps.setProperty("dev-0", "dev-0");
        deviceProps.setProperty("point.x", "250");
        deviceProps.setProperty("point.y", "100");
        deviceProps.setProperty("width", "100");
        deviceProps.setProperty("height", "30");

        DeviceElement elem2 = new DeviceElement("dev-0", deviceProps, instance);
        elem2.measure(graphicsMock);
        instance.addDeviceElement(elem2);

        Properties memProps = new Properties();
        memProps.setProperty("memory", "memory");
        memProps.setProperty("point.x", "150");
        memProps.setProperty("point.y", "200");
        memProps.setProperty("width", "100");
        memProps.setProperty("height", "30");
        MemoryElement elem3 = new MemoryElement("memory", memProps, instance);
        elem3.measure(graphicsMock);
        instance.setMemoryElement(elem3);

        ConnectionLine line0 = new ConnectionLine(elem, elem3, new ArrayList(), instance);
        instance.addConnectionLine(line0);
        ConnectionLine line1 = new ConnectionLine(elem, elem2, new ArrayList(), instance);
        instance.addConnectionLine(line1);

        List<Point> points = new ArrayList<Point>();
        points.add(new Point(250,200));
        ConnectionLine line2 = new ConnectionLine(elem2, elem3, points, instance);
        instance.addConnectionLine(line2);

        int x = 0;
        int y = 90;
        int width = 100;
        int height = 30;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertFalse(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        // left
        instance.moveSelection(-10, 0);
        Assert.assertEquals(90, elem.getX());

        // right
        Assert.assertTrue(instance.moveSelection(10, 0));
        Assert.assertEquals(100, elem.getX());
        Assert.assertFalse(instance.moveSelection(51, 0));
        Assert.assertEquals(100, elem.getX()); // move fails
        Assert.assertFalse(instance.moveSelection(50, 0));
        Assert.assertEquals(100, elem.getX()); // move fails
        Assert.assertTrue(instance.moveSelection(49, 0));
        Assert.assertEquals(149, elem.getX()); // move succeeds

        // down
        Assert.assertFalse(instance.moveSelection(0, 70));
        Assert.assertEquals(100, elem.getY());
        Assert.assertTrue(instance.moveSelection(0, 69));
        Assert.assertEquals(169, elem.getY());

        // up
        Assert.assertTrue(instance.moveSelection(0, -69));
        Assert.assertEquals(100, elem.getY());

        // back left
        Assert.assertTrue(instance.moveSelection(-49, 0));
        Assert.assertEquals(100, elem.getX());

        // test left boundary
        Assert.assertTrue(instance.moveSelection(-50 + Schema.MIN_LEFT_MARGIN + 1, 0));
        Assert.assertEquals(50 + Schema.MIN_LEFT_MARGIN + 1, elem.getX());
        Assert.assertTrue(instance.moveSelection(50 - Schema.MIN_LEFT_MARGIN - 1, 0));
        Assert.assertEquals(100, elem.getX());
        Assert.assertTrue(instance.moveSelection(-50 + Schema.MIN_LEFT_MARGIN, 0));
        Assert.assertEquals(50 + Schema.MIN_LEFT_MARGIN, elem.getX());
        Assert.assertTrue(instance.moveSelection(50 - Schema.MIN_LEFT_MARGIN, 0));
        Assert.assertEquals(100, elem.getX());
        Assert.assertFalse(instance.moveSelection(-50 + Schema.MIN_LEFT_MARGIN - 1, 0));
        Assert.assertEquals(100, elem.getX());

        // test top boundary
        Assert.assertTrue(instance.moveSelection(0, -85 + Schema.MIN_TOP_MARGIN + 1));
        Assert.assertEquals(15 + Schema.MIN_TOP_MARGIN + 1, elem.getY());
        Assert.assertTrue(instance.moveSelection(0, 85 - Schema.MIN_TOP_MARGIN - 1));
        Assert.assertEquals(100, elem.getY());
        Assert.assertTrue(instance.moveSelection(0, -85 + Schema.MIN_TOP_MARGIN));
        Assert.assertEquals(15 + Schema.MIN_TOP_MARGIN, elem.getY());
        Assert.assertTrue(instance.moveSelection(0, 85 - Schema.MIN_TOP_MARGIN));
        Assert.assertEquals(100, elem.getY());
        Assert.assertFalse(instance.moveSelection(0, -85 + Schema.MIN_TOP_MARGIN - 1));
        Assert.assertEquals(100, elem.getY());

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
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertTrue(elem3.isSelected());
        Assert.assertTrue(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        // down
        Assert.assertTrue(instance.moveSelection(0, 100));
        Assert.assertEquals(300, elem3.getY());

        // up
        Assert.assertTrue(instance.moveSelection(0, -100));
        Assert.assertEquals(200, elem3.getY());

        // right
        Assert.assertTrue(instance.moveSelection(49, 0));
        Assert.assertEquals(199, elem3.getX());
        Assert.assertFalse(instance.moveSelection(1, 0));
        Assert.assertEquals(199, elem3.getX()); // move failed
        Assert.assertFalse(instance.moveSelection(20, 0));
        Assert.assertEquals(199, elem3.getX()); // move failed
        Assert.assertTrue(instance.moveSelection(-1, 0));
        Assert.assertEquals(198, elem3.getX());
        Assert.assertTrue(instance.moveSelection(-48, 0));
        Assert.assertEquals(150, elem3.getX());

        // up
        Assert.assertFalse(instance.moveSelection(0, -100));
        Assert.assertEquals(200, elem3.getY()); // move failed
        Assert.assertFalse(instance.moveSelection(0, -80));
        Assert.assertEquals(200, elem3.getY()); // move failed
        Assert.assertFalse(instance.moveSelection(100, 0));
        Assert.assertEquals(150, elem3.getX()); // move failed
        Assert.assertTrue(instance.moveSelection(1, 0));
        Assert.assertEquals(151, elem3.getX());
        Assert.assertTrue(instance.moveSelection(-1, 0));
        Assert.assertEquals(150, elem3.getX());

        // test multiple selection
        x = 0;
        y = 0;
        width = 149;
        height = 200;
        instance.selectElements(x, y, width, height);
        Assert.assertTrue(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertTrue(elem3.isSelected());
        Assert.assertTrue(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        Assert.assertTrue(instance.moveSelection(10, 0));
        Assert.assertEquals(110, elem.getX());
        Assert.assertEquals(160, elem3.getX());

        Assert.assertTrue(instance.moveSelection(-10, 0));
        Assert.assertEquals(100, elem.getX());
        Assert.assertEquals(150, elem3.getX());

        Assert.assertTrue(instance.moveSelection(-50 + Schema.MIN_LEFT_MARGIN, 0));
        Assert.assertEquals(50 + Schema.MIN_LEFT_MARGIN, elem.getX());
        Assert.assertEquals(100 + Schema.MIN_LEFT_MARGIN, elem3.getX());

        Assert.assertFalse(instance.moveSelection(-1, 0));
        Assert.assertEquals(50 + Schema.MIN_LEFT_MARGIN, elem.getX());
        Assert.assertEquals(100 + Schema.MIN_LEFT_MARGIN, elem3.getX());

    }

    @Test
    public void testDeleteSelected() {
        Schema instance = new Schema();
        Properties compilerProps = new Properties();
        compilerProps.setProperty("compiler", "compiler");
        compilerProps.setProperty("point.x", "100");
        compilerProps.setProperty("point.y", "100");
        compilerProps.setProperty("width", "100");
        compilerProps.setProperty("height", "30");
        CompilerElement elem = new CompilerElement("compiler", compilerProps, instance);
        elem.measure(graphicsMock);
        instance.setCompilerElement(elem);

        Properties deviceProps = new Properties();
        deviceProps.setProperty("dev-0", "dev-0");
        deviceProps.setProperty("point.x", "250");
        deviceProps.setProperty("point.y", "100");
        deviceProps.setProperty("width", "100");
        deviceProps.setProperty("height", "30");

        DeviceElement elem2 = new DeviceElement("dev-0", deviceProps, instance);
        elem2.measure(graphicsMock);
        instance.addDeviceElement(elem2);

        Properties memProps = new Properties();
        memProps.setProperty("memory", "memory");
        memProps.setProperty("point.x", "150");
        memProps.setProperty("point.y", "200");
        memProps.setProperty("width", "100");
        memProps.setProperty("height", "30");
        MemoryElement elem3 = new MemoryElement("memory", memProps, instance);
        elem3.measure(graphicsMock);
        instance.setMemoryElement(elem3);

        ConnectionLine line0 = new ConnectionLine(elem, elem3, new ArrayList(), instance);
        instance.addConnectionLine(line0);
        ConnectionLine line1 = new ConnectionLine(elem, elem2, new ArrayList(), instance);
        instance.addConnectionLine(line1);

        List<Point> points = new ArrayList<Point>();
        points.add(new Point(250, 200));
        ConnectionLine line2 = new ConnectionLine(elem2, elem3, points, instance);
        instance.addConnectionLine(line2);

        int x = 150;
        int y = 200;
        int width = 10;
        int height = 10;
        instance.selectElements(x, y, width, height);
        Assert.assertFalse(elem.isSelected());
        Assert.assertFalse(elem2.isSelected());
        Assert.assertTrue(elem3.isSelected());
        Assert.assertFalse(line0.isSelected());
        Assert.assertFalse(line1.isSelected());
        Assert.assertFalse(line2.isSelected());

        instance.deleteSelected();
        Assert.assertEquals(1, instance.getConnectionLines().size());
        Assert.assertEquals(2, instance.getAllElements().size());
        Assert.assertSame(line1, instance.getConnectionLines().get(0));
        Assert.assertSame(elem, instance.getCompilerElement());
        Assert.assertNull(instance.getMemoryElement());
        Assert.assertSame(elem2, instance.getDeviceElements().get(0));
    }

    @Test
    public void testSaveAndGetSettings() {
        Schema instance = new Schema();
        Properties result = instance.getSettings();
        Assert.assertNotNull(result);

        Properties compilerProps = new Properties();
        compilerProps.setProperty("compiler", "compiler");
        compilerProps.setProperty("point.x", "100");
        compilerProps.setProperty("point.y", "100");
        compilerProps.setProperty("width", "100");
        compilerProps.setProperty("height", "30");
        CompilerElement elem = new CompilerElement("compiler", compilerProps, instance);
        instance.setCompilerElement(elem);

        Assert.assertNull(instance.getSettings().getProperty("compiler"));
        instance.save();
        Assert.assertEquals("compiler", instance.getSettings().getProperty("compiler"));
        Assert.assertEquals("100", instance.getSettings().getProperty("compiler.point.x"));
        Assert.assertEquals("100", instance.getSettings().getProperty("compiler.point.y"));
        Assert.assertEquals("100", instance.getSettings().getProperty("compiler.width"));
        Assert.assertEquals("30", instance.getSettings().getProperty("compiler.height"));
    }
}
