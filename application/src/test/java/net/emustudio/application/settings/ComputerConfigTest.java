/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.P;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                "someId", PLUGIN_TYPE.CPU, "cpu baby", "emptyfile.jar", P.of(10, 10), Config.inMemory()
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
}
