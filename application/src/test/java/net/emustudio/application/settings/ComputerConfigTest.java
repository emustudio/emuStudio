/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ComputerConfigTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private ComputerConfig config;

    @Before
    public void setup() throws IOException {
        Path configFile = temporaryFolder.newFile("computer.toml").toPath();
        Files.deleteIfExists(configFile);
        config = ComputerConfig.create(
                "some nice computer!! baby@#$%^&*()./<>?\"'", configFile
        );
    }

    @After
    public void tearDown() {
        config.close();
    }

    @Test
    public void testNewConfigHasEverythingEmpty() {
        assertEquals(config.getName(), "some nice computer!! baby@#$%^&*()./<>?\"'");
        assertTrue(config.getDevices().isEmpty());
        assertTrue(config.getMemory().isEmpty());
        assertTrue(config.getCompiler().isEmpty());
        assertTrue(config.getCPU().isEmpty());
        assertTrue(config.getConnections().isEmpty());
    }

    @Test
    public void testChangePluginConfig() {
        PluginConfig cpu = PluginConfig.create(
                "someId", PLUGIN_TYPE.CPU, "cpu baby", "emptyfile.jar", SchemaPoint.of(10, 10), Config.inMemory()
        );
        config.setCPU(cpu);

        assertEquals(cpu.getPluginId(), "someId");
        assertEquals(cpu.getPluginName(), "cpu baby");
        assertEquals(cpu.getPluginType(), PLUGIN_TYPE.CPU);
        assertEquals(cpu.getPluginFile(), "emptyfile.jar");
        assertEquals(cpu.getSchemaPoint(), SchemaPoint.of(10, 10));

        SchemaPoint newSchemaPoint = SchemaPoint.of(20, 30);
        cpu.setSchemaPoint(newSchemaPoint);
        assertEquals(cpu.getSchemaPoint(), newSchemaPoint);
    }

    @Test
    public void copyToPreservesDebuggerSettings() throws IOException {
        config.getConfig().set("debugger.watches", List.of("A + 1"));
        Path target = config.getPath().resolveSibling("renamed.toml");
        try (ComputerConfig copy = ComputerConfig.create("Renamed", target)) {
            config.copyTo(copy);
        }
        try (ComputerConfig copy = ComputerConfig.load(target)) {
            assertEquals("Renamed", copy.getName());
            assertEquals(List.of("A + 1"), copy.getConfig().get("debugger.watches"));
        }
    }
}
