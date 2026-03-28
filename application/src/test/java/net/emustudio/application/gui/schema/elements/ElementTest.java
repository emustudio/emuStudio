/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.elements;

import net.emustudio.application.gui.framework.P;
import net.emustudio.application.gui.schema.SchemaTestSupport;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.settings.SchemaPoint;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class ElementTest {

    @Test
    public void measureMoveSizeAndBorderChecksUseElementGeometry() {
        CompilerElement element = new CompilerElement(P.of(100, 100), "compiler", "compiler.jar");
        Graphics2D graphics = SchemaTestSupport.createGraphics();
        try {
            element.setSize(10, 20);
            element.measure(graphics);
        } finally {
            graphics.dispose();
        }

        Rectangle rect = element.getRectangle();
        assertTrue(element.getWidth() >= 80);
        assertTrue(element.getHeight() >= 50);
        assertTrue(element.crossesArea(new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height)));
        assertTrue(element.crossesTopBorder(new Point(element.getX(), rect.y)));
        assertTrue(element.crossesBottomBorder(new Point(element.getX(), rect.y + rect.height)));
        assertTrue(element.crossesLeftBorder(new Point(rect.x, element.getY())));
        assertTrue(element.crossesRightBorder(new Point(rect.x + rect.width, element.getY())));

        element.move(P.of(130, 140));

        assertEquals(130, element.getX());
        assertEquals(140, element.getY());
        assertEquals(130 - element.getWidth() / 2, element.getRectangle().x);
    }

    @Test
    public void saveAndConfigConstructorsPreservePluginMetadata() {
        DeviceElement direct = new DeviceElement(P.of(40, 50), "device", "device.jar");
        PluginConfig saved = direct.save();
        PluginConfig config = SchemaTestSupport.pluginConfig("cpu-id", PLUGIN_TYPE.CPU, "cpu.jar", 13, 17);
        CpuElement fromConfig = new CpuElement(config, new java.util.function.Function<P, P>() {
            @Override
            public P apply(P p) {
                return P.of(20, 30);
            }
        });
        MemoryElement memory = new MemoryElement(
                SchemaTestSupport.pluginConfig("mem-id", PLUGIN_TYPE.MEMORY, "memory.jar", 1, 2),
                new java.util.function.Function<P, P>() {
                    @Override
                    public P apply(P p) {
                        return p;
                    }
                }
        );
        CompilerElement compiler = new CompilerElement(
                SchemaTestSupport.pluginConfig("comp-id", PLUGIN_TYPE.COMPILER, "compiler.jar", 3, 4),
                new java.util.function.Function<P, P>() {
                    @Override
                    public P apply(P p) {
                        return p;
                    }
                }
        );

        assertEquals("device", saved.getPluginName());
        assertEquals("device.jar", saved.getPluginFile());
        assertEquals(SchemaPoint.of(40, 50), saved.getSchemaPoint());
        assertEquals("cpu-id", fromConfig.save().getPluginId());
        assertEquals(20, fromConfig.getX());
        assertEquals(30, fromConfig.getY());
        assertEquals(1, memory.getX());
        assertEquals(2, memory.getY());
        assertEquals(3, compiler.getX());
        assertEquals(4, compiler.getY());
        assertTrue(direct.toString().contains("device"));
    }
}
