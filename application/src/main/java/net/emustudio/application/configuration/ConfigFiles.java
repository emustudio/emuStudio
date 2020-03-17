/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.configuration;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigFiles {
    private final static String DIR_CONFIG = "config";
    private final static String DIR_CPU = "cpu";
    private final static String DIR_COMPILER = "compiler";
    private final static String DIR_MEMORY = "memory";
    private final static String DIR_DEVICE = "device";

    private final static Map<PLUGIN_TYPE, String> PLUGIN_SUBDIRS = Map.of(
        PLUGIN_TYPE.COMPILER, DIR_COMPILER,
        PLUGIN_TYPE.CPU, DIR_CPU,
        PLUGIN_TYPE.MEMORY, DIR_MEMORY,
        PLUGIN_TYPE.DEVICE, DIR_DEVICE
    );
    private final String baseDirectory;


    public ConfigFiles() {
        this.baseDirectory = System.getProperty("user.dir");
    }

    public ConfigFiles(String baseDirectory) {
        this.baseDirectory = Objects.requireNonNull(baseDirectory);
    }

    public Optional<ComputerConfig> loadConfiguration(String computerName) throws IOException {
        return loadConfigurations().stream().filter(config -> config.getName().equals(computerName)).findAny();
    }

    public List<ComputerConfig> loadConfigurations() throws IOException {
        return Files.list(Path.of(baseDirectory, DIR_CONFIG))
            .filter(p -> !Files.isDirectory(p) && Files.isReadable(p))
            .map(p -> {
                try {
                    return Optional.of(ComputerConfig.load(p));
                } catch (Exception e) {
                    return Optional.<ComputerConfig>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    public List<Path> listPluginFiles(PLUGIN_TYPE pluginType) throws IOException {
        return Files.list(Path.of(baseDirectory, PLUGIN_SUBDIRS.get(pluginType)))
            .filter(p -> !Files.isDirectory(p) && Files.isReadable(p))
            .collect(Collectors.toList());
    }

    public ComputerConfig createConfiguration(String computerName) {
        Path configPath = Path.of(baseDirectory, DIR_CONFIG, encodeToFileName(computerName) + ".toml");
        return ComputerConfig.create(computerName, configPath);
    }

    public void removeConfiguration(String computerName) throws IOException {
        Path configPath = Path.of(baseDirectory, DIR_CONFIG, encodeToFileName(computerName) + ".toml");
        Files.deleteIfExists(configPath);
    }

    private String encodeToFileName(String name) {
        return name.replaceAll("\\W+", "");
    }
}
