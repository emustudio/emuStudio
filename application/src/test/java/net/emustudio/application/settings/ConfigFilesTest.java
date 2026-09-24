/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import org.junit.After;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Objects;

import static net.emustudio.application.settings.ConfigFiles.loadConfigurations;
import static org.junit.Assert.*;

public class ConfigFilesTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void restoreDefaultPaths() {
        Path currentDirectory = Path.of(System.getProperty("user.dir"));
        ConfigFiles.setBasePaths(currentDirectory, currentDirectory);
    }

    @Test
    public void testInvalidConfigsAreIgnored() throws IOException, URISyntaxException {
        ClassLoader classLoader = getClass().getClassLoader();
        Path configDir = Path.of(Objects.requireNonNull(classLoader.getResource(".")).toURI());
        assertTrue(loadConfigurations(configDir).isEmpty());
    }

    @Test
    public void configuredRootsResolveConfigsAndPluginsIndependently() throws Exception {
        Path configRoot = temporaryFolder.newFolder("settings").toPath();
        Path pluginsRoot = temporaryFolder.newFolder("plugins").toPath();
        Files.createDirectories(configRoot.resolve("config"));
        Files.createDirectories(pluginsRoot.resolve("device"));
        Files.writeString(pluginsRoot.resolve("device/display.jar"), "");
        ConfigFiles.setBasePaths(configRoot, pluginsRoot);

        try (ComputerConfig ignored = ConfigFiles.createConfiguration("Test computer");
             ComputerConfig loaded = ConfigFiles.loadConfiguration("Test computer").orElseThrow()) {
            assertEquals("Test computer", loaded.getName());
        }
        assertEquals(pluginsRoot.resolve("device/display.jar").toAbsolutePath(),
                ConfigFiles.getAbsolutePluginPath("display.jar", net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE.DEVICE));
        assertEquals(java.util.List.of("display.jar"),
                ConfigFiles.listPluginFiles(net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE.DEVICE));
    }
}
