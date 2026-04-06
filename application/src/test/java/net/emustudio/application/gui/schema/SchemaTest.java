/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema;

import net.emustudio.application.gui.components.P;
import net.emustudio.application.gui.schema.elements.CompilerElement;
import net.emustudio.application.gui.schema.elements.ConnectionLine;
import net.emustudio.application.gui.schema.elements.CpuElement;
import net.emustudio.application.gui.schema.elements.DeviceElement;
import net.emustudio.application.gui.schema.elements.Element;
import net.emustudio.application.gui.schema.elements.MemoryElement;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.settings.PluginConnection;
import net.emustudio.application.settings.SchemaPoint;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SchemaTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savePersistsElementsConnectionsAndGridSnapping() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-save")) {
            AppSettings appSettings = SchemaTestSupport.createAppSettings(true, 10);
            Schema schema = new Schema(config, appSettings);

            schema.setCompilerElement(new Point(13, 17), "compiler.jar");
            schema.setCpuElement(new Point(56, 14), "cpu.jar");
            schema.setMemoryElement(new Point(84, 39), "memory.jar");
            schema.addDeviceElement(new Point(117, 84), "device.jar");

            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            schema.addConnectionLine(compiler, cpu, List.of(P.of(33, 44)), true);
            schema.save();

            PluginConfig compilerConfig = config.getCompiler().get();
            PluginConfig cpuConfig = config.getCPU().get();
            PluginConfig memoryConfig = config.getMemory().get();
            PluginConfig deviceConfig = config.getDevices().get(0);
            PluginConnection connection = config.getConnections().get(0);

            assertEquals(SchemaPoint.of(10, 20), compilerConfig.getSchemaPoint());
            assertEquals(SchemaPoint.of(60, 10), cpuConfig.getSchemaPoint());
            assertEquals(SchemaPoint.of(80, 40), memoryConfig.getSchemaPoint());
            assertEquals(SchemaPoint.of(120, 80), deviceConfig.getSchemaPoint());
            assertEquals(List.of(SchemaPoint.of(30, 40)), connection.getSchemaPoints());
        }
    }

    @Test
    public void selectAllAndDeleteSelectedRemovesAllElementsAndConnections() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-delete")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            schema.setCompilerElement(new Point(10, 10), "compiler.jar");
            schema.setCpuElement(new Point(100, 10), "cpu.jar");
            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            schema.addConnectionLine(compiler, cpu, List.of(P.of(50, 10)), true);

            schema.selectAll();
            schema.deleteSelected();

            assertTrue(schema.getAllElements().isEmpty());
            assertTrue(schema.getConnectionLines().isEmpty());
        }
    }

    @Test
    public void crossingLookupMovementAndLineLookupUseSchemaGeometry() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-geometry")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            schema.addDeviceElement(new Point(82, 82), "device.jar");
            DeviceElement device = findElement(schema.getAllElements(), DeviceElement.class);
            Graphics2D graphics = SchemaTestSupport.createGraphics();
            try {
                device.measure(graphics);
            } finally {
                graphics.dispose();
            }
            assertSame(device, schema.getCrossingElement(new Point(device.getX(), device.getY())));

            device.setSelected(true);
            schema.moveSelection(8, 17);
            assertEquals(90, device.getX());
            assertEquals(100, device.getY());

            schema.moveElement(device, new Point(0, 0));
            assertEquals(90, device.getX());
            assertEquals(100, device.getY());

            schema.setCompilerElement(new Point(10, 90), "compiler.jar");
            schema.setCpuElement(new Point(110, 90), "cpu.jar");
            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            schema.addConnectionLine(compiler, cpu, List.of(P.of(60, 90)), false);

            List<ConnectionLine> lines = schema.getConnectionLines();
            assertTrue(schema.isConnected(compiler, cpu));
            assertSame(lines.get(0), schema.findCrossingLine(new Point(60, 90)));
        }
    }

    @Test
    public void loadRestoresConfiguredElementsAndSkipsConnectionsToMissingPlugins() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-load")) {
            config.setCompiler(SchemaTestSupport.pluginConfig("compiler-id", PLUGIN_TYPE.COMPILER, "compiler.jar", 13, 17));
            config.setCPU(SchemaTestSupport.pluginConfig("cpu-id", PLUGIN_TYPE.CPU, "cpu.jar", 56, 14));
            config.setMemory(SchemaTestSupport.pluginConfig("memory-id", PLUGIN_TYPE.MEMORY, "memory.jar", 84, 39));
            config.setDevices(List.of(
                    SchemaTestSupport.pluginConfig("device-id", PLUGIN_TYPE.DEVICE, "device.jar", 117, 84)
            ));
            config.setConnections(Arrays.asList(
                    PluginConnection.create("compiler-id", "cpu-id", true, List.of(SchemaPoint.of(23, 27))),
                    PluginConnection.create("compiler-id", "missing-id", false, List.of(SchemaPoint.of(99, 99)))
            ));

            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            MemoryElement memory = findElement(schema.getAllElements(), MemoryElement.class);
            DeviceElement device = findElement(schema.getAllElements(), DeviceElement.class);

            assertEquals(4, schema.getAllElements().size());
            assertPoint(compiler.getSchemaPoint(), 10, 20);
            assertPoint(cpu.getSchemaPoint(), 60, 10);
            assertPoint(memory.getSchemaPoint(), 80, 40);
            assertPoint(device.getSchemaPoint(), 120, 80);
            assertEquals(1, schema.getConnectionLines().size());
            assertTrue(schema.isConnected(compiler, cpu));
            assertFalse(schema.isConnected(compiler, memory));
            assertEquals(SchemaPoint.of(23, 27), schema.getConnectionLines().get(0).toPluginConnection().getSchemaPoints().get(0));
        }
    }

    @Test
    public void selectionBorderLookupAndLineEditingUseMeasuredGeometryAndGrid() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-selection")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            schema.setCompilerElement(new Point(40, 40), "compiler.jar");
            schema.setCpuElement(new Point(140, 40), "cpu.jar");
            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            measureAllElements(schema);

            schema.addConnectionLine(compiler, cpu, List.of(P.of(90, 40)), false);
            ConnectionLine line = schema.getConnectionLines().get(0);

            schema.select(0, 0, 80, 80);
            assertTrue(compiler.isSelected());
            assertFalse(cpu.isSelected());

            schema.deselectAll();
            schema.select(85, 35, 10, 10);
            assertTrue(line.isSelected());
            assertFalse(compiler.isSelected());

            Rectangle bounds = compiler.getRectangle();
            assertSame(compiler, schema.getElementByBorderPoint(new Point(bounds.x, compiler.getY())));
            assertNull(schema.getElementByBorderPoint(new Point(250, 250)));
            assertNull(schema.getCrossingElement(null));

            P linePoint = line.getPoints().get(0);
            schema.moveLinePoint(line, linePoint, new Point(95, 55));
            assertPoint(linePoint, 100, 60);

            schema.moveLinePoint(line, linePoint, new Point(cpu.getX(), cpu.getY()));
            assertPoint(linePoint, 100, 60);

            schema.moveLinePoint(line, linePoint, new Point(0, 0));
            assertPoint(linePoint, 100, 60);

            schema.addLinePoint(line, 0, new Point(67, 23));
            assertEquals(2, line.getPoints().size());
            assertPoint(line.getPoints().get(0), 70, 20);

            schema.removeConnectionLine(line);
            assertTrue(schema.getConnectionLines().isEmpty());
        }
    }

    @Test
    public void moveSelectionMovesSelectedLinePointsAndDeleteSelectedRemovesOnlySelectedLines() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-line-move")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            schema.setCompilerElement(new Point(40, 40), "compiler.jar");
            schema.setCpuElement(new Point(160, 40), "cpu.jar");
            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            schema.addConnectionLine(compiler, cpu, List.of(P.of(100, 40)), false);

            ConnectionLine line = schema.getConnectionLines().get(0);
            P point = line.getPoints().get(0);
            line.setSelected(true);

            schema.moveSelection(15, 15);

            assertPoint(point, 120, 60);
            assertEquals(2, schema.getAllElements().size());

            schema.deleteSelected();

            assertEquals(2, schema.getAllElements().size());
            assertTrue(schema.getConnectionLines().isEmpty());
        }
    }

    @Test
    public void replacingAndRemovingElementsUpdatesIncidentLinesAndClearsSavedConfig() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-replace")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            schema.setCompilerElement(new Point(20, 20), "compiler.jar");
            schema.setCpuElement(new Point(120, 20), "cpu.jar");
            schema.setMemoryElement(new Point(220, 20), "memory.jar");
            schema.addDeviceElement(new Point(320, 20), "device.jar");

            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            MemoryElement memory = findElement(schema.getAllElements(), MemoryElement.class);
            DeviceElement device = findElement(schema.getAllElements(), DeviceElement.class);

            schema.addConnectionLine(compiler, cpu, List.of(P.of(70, 20)), true);
            schema.addConnectionLine(cpu, memory, List.of(P.of(170, 20)), false);

            schema.setCompilerElement(new Point(30, 30), "compiler-v2.jar");
            CompilerElement newCompiler = findElement(schema.getAllElements(), CompilerElement.class);
            assertNotSame(compiler, newCompiler);
            assertTrue(schema.isConnected(newCompiler, cpu));
            assertFalse(schema.isConnected(compiler, cpu));

            schema.setCpuElement(new Point(130, 30), "cpu-v2.jar");
            CpuElement newCpu = findElement(schema.getAllElements(), CpuElement.class);
            assertNotSame(cpu, newCpu);
            assertTrue(schema.isConnected(newCompiler, newCpu));
            assertTrue(schema.isConnected(newCpu, memory));
            assertFalse(schema.isConnected(newCompiler, cpu));
            assertFalse(schema.isConnected(cpu, memory));

            schema.setMemoryElement(new Point(230, 30), "memory-v2.jar");
            MemoryElement newMemory = findElement(schema.getAllElements(), MemoryElement.class);
            assertNotSame(memory, newMemory);
            assertTrue(schema.isConnected(newCpu, newMemory));
            assertFalse(schema.isConnected(newCpu, memory));

            schema.removeElement(newCompiler);
            assertEquals(3, schema.getAllElements().size());
            assertEquals(1, schema.getConnectionLines().size());
            assertFalse(schema.getAllElements().stream().anyMatch(CompilerElement.class::isInstance));

            schema.removeElement(newCpu);
            assertEquals(2, schema.getAllElements().size());
            assertTrue(schema.getConnectionLines().isEmpty());
            assertFalse(schema.getAllElements().stream().anyMatch(CpuElement.class::isInstance));

            schema.removeElement(newMemory);
            schema.removeElement(device);
            assertTrue(schema.getAllElements().isEmpty());

            schema.save();

            assertFalse(config.getCompiler().isPresent());
            assertFalse(config.getCPU().isPresent());
            assertFalse(config.getMemory().isPresent());
            assertTrue(config.getDevices().isEmpty());
            assertTrue(config.getConnections().isEmpty());
        }
    }

    @Test
    public void disablingGridLeavesNewElementCoordinatesUntouched() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-grid")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            schema.setUseSchemaGrid(false);
            schema.addDeviceElement(new Point(117, 84), "device.jar");

            assertFalse(schema.useSchemaGrid());
            assertPoint(findElement(schema.getAllElements(), DeviceElement.class).getSchemaPoint(), 117, 84);
        }
    }

    @Test
    public void moveElementHonorsGridGapAndRejectsOverlapsWithLinePointsOrOtherElements() throws Exception {
        try (ComputerConfig config = SchemaTestSupport.createComputerConfig(temporaryFolder, "schema-move-element")) {
            Schema schema = new Schema(config, SchemaTestSupport.createAppSettings(true, 10));

            assertSame(config, schema.getComputerConfig());
            schema.setSchemaGridGap(5);
            assertEquals(5, schema.getSchemaGridGap());

            schema.setCompilerElement(new Point(30, 30), "compiler.jar");
            schema.setCpuElement(new Point(150, 30), "cpu.jar");
            schema.addDeviceElement(new Point(90, 100), "device.jar");

            CompilerElement compiler = findElement(schema.getAllElements(), CompilerElement.class);
            CpuElement cpu = findElement(schema.getAllElements(), CpuElement.class);
            DeviceElement device = findElement(schema.getAllElements(), DeviceElement.class);
            measureAllElements(schema);

            schema.addConnectionLine(compiler, cpu, List.of(P.of(90, 30)), false);

            schema.moveElement(device, new Point(90, 30));
            assertPoint(device.getSchemaPoint(), 90, 100);

            schema.moveElement(device, new Point(30, 30));
            assertPoint(device.getSchemaPoint(), 90, 100);

            schema.moveElement(device, new Point(242, 93));
            assertPoint(device.getSchemaPoint(), 240, 95);
        }
    }

    private <T extends Element> T findElement(List<Element> elements, Class<T> type) {
        for (Element element : elements) {
            if (type.isInstance(element)) {
                return type.cast(element);
            }
        }
        throw new AssertionError("Missing element of type " + type.getSimpleName());
    }

    private void measureAllElements(Schema schema) {
        Graphics2D graphics = SchemaTestSupport.createGraphics();
        try {
            for (Element element : schema.getAllElements()) {
                element.measure(graphics);
            }
        } finally {
            graphics.dispose();
        }
    }

    private void assertPoint(P point, int x, int y) {
        assertEquals(x, point.ix());
        assertEquals(y, point.iy());
    }
}
