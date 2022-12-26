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
import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ComputerConfig implements Closeable {
    private final FileConfig config;

    public ComputerConfig(FileConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public static ComputerConfig load(Path configurationFile) {
        FileConfig config = FileConfig.builder(configurationFile).concurrent().sync().autosave().build();
        config.load();

        return new ComputerConfig(config);
    }

    public static ComputerConfig create(String computerName, Path configurationFile) throws IOException {
        if (Files.exists(configurationFile)) {
            throw new IllegalArgumentException("Configuration already exists");
        }
        Files.createFile(configurationFile);
        FileConfig config = FileConfig.builder(configurationFile).concurrent().sync().autosave().build();
        config.set("name", computerName);

        return new ComputerConfig(config);
    }

    public FileConfig getConfig() {
        return config;
    }

    public void copyTo(ComputerConfig other) {
        getCompiler().ifPresent(other::setCompiler);
        getCPU().ifPresent(other::setCPU);
        getMemory().ifPresent(other::setMemory);
        other.setDevices(getDevices());
        other.setConnections(getConnections());
    }

    public Path getPath() {
        return config.getNioPath();
    }

    public String getName() {
        return config.get("name");
    }

    public void setName(String name) {
        config.set("name", Objects.requireNonNull(name));
    }

    public Optional<PluginConfig> getCompiler() {
        Optional<Config> pluginConfigOpt = config.getOptional(PLUGIN_TYPE.COMPILER.name());
        return pluginConfigOpt.map(PluginConfig::new);
    }

    public void setCompiler(PluginConfig compiler) {
        if (compiler == null) {
            config.remove(PLUGIN_TYPE.COMPILER.name());
        } else {
            Config sub = config.createSubConfig(); // to inherit autosave
            sub.addAll(compiler.getConfig());
            config.set(PLUGIN_TYPE.COMPILER.name(), sub);
        }
    }

    public Optional<PluginConfig> getCPU() {
        Optional<Config> pluginConfigOpt = config.getOptional(PLUGIN_TYPE.CPU.name());
        return pluginConfigOpt.map(PluginConfig::new);
    }

    public void setCPU(PluginConfig cpu) {
        if (cpu == null) {
            config.remove(PLUGIN_TYPE.CPU.name());
        } else {
            Config sub = config.createSubConfig();
            sub.addAll(cpu.getConfig());
            config.set(PLUGIN_TYPE.CPU.name(), sub);
        }
    }

    public Optional<PluginConfig> getMemory() {
        Optional<Config> pluginConfigOpt = config.getOptional(PLUGIN_TYPE.MEMORY.name());
        return pluginConfigOpt.map(PluginConfig::new);
    }

    public void setMemory(PluginConfig memory) {
        if (memory == null) {
            config.remove(PLUGIN_TYPE.MEMORY.name());
        } else {
            Config sub = config.createSubConfig();
            sub.addAll(memory.getConfig());
            config.set(PLUGIN_TYPE.MEMORY.name(), sub);
        }
    }

    public List<PluginConfig> getDevices() {
        Optional<List<Config>> devicesConfig = config.getOptional(PLUGIN_TYPE.DEVICE.name());
        if (devicesConfig.isEmpty()) {
            return Collections.emptyList();
        } else {
            return devicesConfig.get().stream().map(PluginConfig::new).collect(toList());
        }
    }

    public void setDevices(List<PluginConfig> devices) {
        List<Config> devicesConfig = devices.stream().map(d -> {
            Config sub = config.createSubConfig();
            sub.addAll(d.getConfig());
            return sub;
        }).collect(toList());
        config.set(PLUGIN_TYPE.DEVICE.name(), devicesConfig);
    }

    public List<PluginConnection> getConnections() {
        Optional<List<Config>> rawConnections = config.getOptional("connections");

        return rawConnections
                .map(connections -> connections.stream().map(PluginConnection::new).collect(toList()))
                .orElse(Collections.emptyList());
    }

    public void setConnections(List<PluginConnection> connections) {
        List<Config> configs = connections.stream().map(c -> {
            Config sub = config.createSubConfig();
            sub.addAll(c.getConfig());
            return sub;
        }).collect(toList());
        config.set("connections", configs);
    }

    @Override
    public void close() {
        config.close();
    }

    @Override
    public String toString() {
        return getName();
    }
}
