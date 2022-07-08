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
package net.emustudio.application.configuration;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.runtime.CannotUpdateSettingException;

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
    private final Path basePath;

    public final static ConfigFiles DEFAULT = new ConfigFiles(Path.of(System.getProperty("user.dir")));

    public ConfigFiles(String basePath) {
        this(Path.of(basePath));
    }

    public ConfigFiles(Path basePath) {
        this.basePath = Objects.requireNonNull(basePath);
    }

    public Optional<ComputerConfig> loadConfiguration(String computerName) throws IOException {
        return loadConfigurations().stream().filter(config -> config.getName().equals(computerName)).findAny();
    }

    public ComputerConfig loadConfiguration(Path computerPath) {
        return ComputerConfig.load(computerPath);
    }

    public List<ComputerConfig> loadConfigurations() throws IOException {
        if (!Files.exists(basePath.resolve(DIR_CONFIG))) {
            return Collections.emptyList();
        }
        return Files.list(basePath.resolve(DIR_CONFIG))
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
            .filter(c -> c.getName() != null)
            .sorted(Comparator.comparing(ComputerConfig::getName))
            .collect(Collectors.toList());
    }

    public Path getAbsolutePluginPath(String relativePluginPath, PLUGIN_TYPE pluginType) {
        Path basicPath = Path.of(relativePluginPath);
        if (basicPath.isAbsolute()) {
            return basicPath;
        } else {
            Path pluginBasePath = basePath.resolve(PLUGIN_SUBDIRS.get(pluginType));
            return pluginBasePath.resolve(relativePluginPath);
        }
    }

    public List<String> listPluginFiles(PLUGIN_TYPE pluginType) throws IOException {
        Path pluginBasePath = basePath.resolve(PLUGIN_SUBDIRS.get(pluginType));
        return Files.list(pluginBasePath)
            .filter(p -> !Files.isDirectory(p) && Files.isReadable(p))
            .map(p -> p.getFileName().toString())
            .sorted()
            .collect(Collectors.toList());
    }

    public ComputerConfig createConfiguration(String computerName) throws IOException {
        Path configPath = basePath.resolve(DIR_CONFIG).resolve(encodeToFileName(computerName) + ".toml");
        return ComputerConfig.create(computerName, configPath);
    }

    public void removeConfiguration(String computerName) throws IOException {
        Path configPath = basePath.resolve(DIR_CONFIG).resolve(encodeToFileName(computerName) + ".toml");
        Files.deleteIfExists(configPath);
    }

    public void renameConfiguration(ComputerConfig originalConfiguration, String newName) throws IOException, CannotUpdateSettingException {
        ComputerConfig newConfig = createConfiguration(newName);
        originalConfiguration.copyTo(newConfig);
        newConfig.save();

        originalConfiguration.close();
        removeConfiguration(originalConfiguration.getName());
    }

    private String encodeToFileName(String name) {
        return name.replaceAll("\\W+", "");
    }
}
