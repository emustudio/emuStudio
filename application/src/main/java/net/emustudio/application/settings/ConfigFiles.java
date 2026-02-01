/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotThreadSafe
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
    private static final Path basePath = Path.of(System.getProperty("user.dir"));

    public static Optional<ComputerConfig> loadConfiguration(String computerName) throws IOException {
        return loadConfigurations().stream().filter(config -> config.getName().equals(computerName)).findAny();
    }

    public static Optional<ComputerConfig> loadConfiguration(int computerIndex) throws IOException {
        return Optional.ofNullable(loadConfigurations().get(computerIndex));
    }

    public static Optional<ComputerConfig> loadConfiguration(Path computerPath) {
        return Optional.of(ComputerConfig.load(computerPath));
    }

    public static List<String> listConfigurationNames() throws IOException {
        return loadConfigurations().stream().map(ComputerConfig::getName).collect(Collectors.toList());
    }

    public static List<ComputerConfig> loadConfigurations() throws IOException {
        return loadConfigurations(basePath);
    }

    public static List<ComputerConfig> loadConfigurations(Path basePath) throws IOException {
        if (!Files.exists(basePath.resolve(DIR_CONFIG))) {
            return Collections.emptyList();
        }
        try (Stream<Path> configFiles = Files.list(basePath.resolve(DIR_CONFIG))) {
            return configFiles
                    .filter(p -> !Files.isDirectory(p) && Files.isReadable(p))
                    .flatMap(p -> {
                        try {
                            return Stream.of(ComputerConfig.load(p));
                        } catch (Exception e) {
                            return Stream.empty();
                        }
                    })
                    .filter(c -> c.getName() != null)
                    .sorted(Comparator.comparing(ComputerConfig::getName))
                    .collect(Collectors.toList());
        }
    }

    public static Path getAbsolutePluginPath(String relativePluginPath, PLUGIN_TYPE pluginType) {
        Path basicPath = Path.of(relativePluginPath);
        if (basicPath.isAbsolute()) {
            return basicPath;
        } else {
            Path pluginBasePath = basePath.resolve(PLUGIN_SUBDIRS.get(pluginType));
            return pluginBasePath.resolve(relativePluginPath);
        }
    }

    public static List<String> listPluginFiles(PLUGIN_TYPE pluginType) throws IOException {
        Path pluginBasePath = basePath.resolve(PLUGIN_SUBDIRS.get(pluginType));
        try (Stream<Path> paths = Files.list(pluginBasePath)) {
            return paths
                    .filter(p -> !Files.isDirectory(p) && Files.isReadable(p))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public static ComputerConfig createConfiguration(String computerName) throws IOException {
        Path configPath = basePath.resolve(DIR_CONFIG).resolve(encodeToFileName(computerName) + ".toml");
        return ComputerConfig.create(computerName, configPath);
    }

    public static void removeConfiguration(String computerName) throws IOException {
        Path configPath = basePath.resolve(DIR_CONFIG).resolve(encodeToFileName(computerName) + ".toml");
        Files.deleteIfExists(configPath);
    }

    public static void renameConfiguration(ComputerConfig originalConfiguration, String newName) throws IOException {
        try (ComputerConfig newConfig = createConfiguration(newName)) {
            originalConfiguration.copyTo(newConfig);
            originalConfiguration.close();
            removeConfiguration(originalConfiguration.getName());
        }
    }

    private static String encodeToFileName(String name) {
        return name.replaceAll("\\W+", "");
    }
}
