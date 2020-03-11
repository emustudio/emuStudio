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
    private final static String CONFIGS_DIR = "config";
    private final static String CPUS_DIR = "cpu";
    private final static String COMPILERS_DIR = "compilers";
    private final static String MEMORIES_DIR = "mem";
    private final static String DEVICES_DIR = "devices";

    private final static Map<PLUGIN_TYPE, String> PLUGIN_SUBDIRS = new HashMap<>();
    static {
        PLUGIN_SUBDIRS.put(PLUGIN_TYPE.COMPILER, COMPILERS_DIR);
        PLUGIN_SUBDIRS.put(PLUGIN_TYPE.CPU, CPUS_DIR);
        PLUGIN_SUBDIRS.put(PLUGIN_TYPE.MEMORY, MEMORIES_DIR);
        PLUGIN_SUBDIRS.put(PLUGIN_TYPE.DEVICE, DEVICES_DIR);
    }

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
        return Files.list(Path.of(baseDirectory, CONFIGS_DIR))
            .filter(p -> !Files.isDirectory(p) && Files.isReadable(p))
            .map(ComputerConfig::load)
            .collect(Collectors.toList());
    }

    public List<Path> listPluginFiles(PLUGIN_TYPE pluginType) throws IOException {
        return Files.list(Path.of(baseDirectory, PLUGIN_SUBDIRS.get(pluginType)))
            .filter(p -> !Files.isDirectory(p) && Files.isReadable(p))
            .collect(Collectors.toList());
    }

    public ComputerConfig createConfiguration(String computerName) {
        Path configPath = Path.of(baseDirectory, CONFIGS_DIR, encodeToFileName(computerName) + ".conf");
        return ComputerConfig.create(computerName, configPath);
    }

    public void removeConfiguration(String computerName) throws IOException {
        Path configPath = Path.of(baseDirectory, CONFIGS_DIR, encodeToFileName(computerName) + ".conf");
        Files.deleteIfExists(configPath);
    }

    private String encodeToFileName(String name) {
        return name.replaceAll("\\W+", "");
    }
}
