/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.application.virtualcomputer;

import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.settings.PluginSettingsImpl;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.helpers.Unchecked;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.emustudio.application.internal.Reflection.doesImplement;

public class VirtualComputer implements PluginConnections, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualComputer.class);

    private static final Map<PLUGIN_TYPE, Class<? extends Plugin>> pluginInterfaces = Map.of(
            PLUGIN_TYPE.COMPILER, Compiler.class,
            PLUGIN_TYPE.CPU, CPU.class,
            PLUGIN_TYPE.MEMORY, Memory.class,
            PLUGIN_TYPE.DEVICE, Device.class
    );
    // The first parameter of constructor is plug-in ID
    private static final Class<?>[] PLUGIN_CONSTRUCTOR_PARAMS = {
            long.class, ApplicationApi.class, PluginSettings.class
    };


    private final ComputerConfig computerConfig;

    private final Map<Long, PluginMeta> pluginsById = new HashMap<>();
    private final Map<PLUGIN_TYPE, List<PluginMeta>> pluginsByType = new HashMap<>();

    public VirtualComputer(ComputerConfig computerConfig, Map<Long, PluginMeta> plugins) {
        this.computerConfig = Objects.requireNonNull(computerConfig);
        plugins.forEach((pluginId, pluginMeta) -> {
            pluginsById.put(pluginId, pluginMeta);

            PLUGIN_TYPE pluginType = pluginMeta.pluginConfig.getPluginType();
            if (!pluginsByType.containsKey(pluginType)) {
                pluginsByType.put(pluginType, new ArrayList<>());
            }
            List<PluginMeta> metas = pluginsByType.get(pluginType);
            metas.add(pluginMeta);
        });
    }

    public static VirtualComputer create(ComputerConfig computerConfig, ApplicationApi applicationApi,
                                         AppSettings appSettings) throws IOException, InvalidPluginException {
        Map<Long, PluginMeta> plugins = loadPlugins(computerConfig, applicationApi, appSettings);
        return new VirtualComputer(computerConfig, plugins);
    }

    private static Map<Long, PluginMeta> loadPlugins(
            ComputerConfig computerConfig,
            ApplicationApi applicationApi,
            AppSettings appSettings
    ) throws IOException, InvalidPluginException {
        List<PluginConfig> pluginConfigs = Stream.of(
                        computerConfig.getCompiler(),
                        computerConfig.getCPU(),
                        computerConfig.getMemory()
                ).map(opt -> opt.map(List::of).orElse(Collections.emptyList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        pluginConfigs.addAll(computerConfig.getDevices());

        List<File> filesToLoad = pluginConfigs.stream()
                .map(c -> c.getPluginPath().toFile())
                .collect(Collectors.toList());

        LOGGER.debug("Loading plugin files: {}", filesToLoad);

        PluginLoader pluginLoader = new PluginLoader();
        List<Class<Plugin>> pluginClasses = pluginLoader.loadPlugins(filesToLoad);

        return constructPlugins(pluginClasses, pluginConfigs, applicationApi, appSettings, computerConfig.getConfig()::save);
    }

    private static Map<Long, PluginMeta> constructPlugins(
            List<Class<Plugin>> pluginClasses,
            List<PluginConfig> pluginConfigs,
            ApplicationApi applicationApi,
            AppSettings appSettings,
            Runnable save
    ) throws InvalidPluginException {

        Map<Long, PluginMeta> plugins = new HashMap<>();
        AtomicLong pluginIdCounter = new AtomicLong(1); // 0 is reserved for emuStudio

        for (int i = 0; i < Math.min(pluginClasses.size(), pluginConfigs.size()); i++) {
            Class<Plugin> pluginClass = pluginClasses.get(i);
            PluginConfig pluginConfig = pluginConfigs.get(i);
            PluginSettings pluginSettings = new PluginSettingsImpl(
                    pluginConfig.getPluginSettings(), appSettings, save
            );

            if (!doesImplement(pluginClass, pluginInterfaces.get(pluginConfig.getPluginType()))) {
                throw new InvalidPluginException(
                        "Plugin" + pluginConfig.getPluginName() + " does not implement interface " + pluginClass.getName()
                );
            }

            long pluginId = pluginIdCounter.getAndIncrement();
            Plugin pluginInstance = Unchecked.call(
                    () -> createPluginInstance(pluginId, pluginClass, applicationApi, pluginSettings)
            );

            PluginMeta pluginMeta = new PluginMeta(pluginSettings, pluginInstance, pluginConfig);
            plugins.put(pluginId, pluginMeta);
        }

        return plugins;
    }

    private static Plugin createPluginInstance(long pluginID, Class<? extends Plugin> mainClass, ApplicationApi applicationApi,
                                               PluginSettings pluginSettings) throws InvalidPluginException {
        Objects.requireNonNull(mainClass);
        Objects.requireNonNull(applicationApi);
        
        try {
            Constructor<?> constructor = mainClass.getDeclaredConstructor(PLUGIN_CONSTRUCTOR_PARAMS);
            return (Plugin) constructor.newInstance(pluginID, applicationApi, pluginSettings);
        } catch (Exception | NoClassDefFoundError e) {
            throw new InvalidPluginException("Plug-in main class does not have proper constructor", e);
        }
    }

    public ComputerConfig getComputerConfig() {
        return computerConfig;
    }

    public void initialize(ContextPoolImpl contextPool) throws PluginInitializationException {
        contextPool.setComputer(this);
        List<PluginMeta> pluginsToInitialize = Stream.of(
                pluginsByType.getOrDefault(PLUGIN_TYPE.COMPILER, Collections.emptyList()),
                pluginsByType.getOrDefault(PLUGIN_TYPE.MEMORY, Collections.emptyList()),
                pluginsByType.getOrDefault(PLUGIN_TYPE.CPU, Collections.emptyList()),
                pluginsByType.getOrDefault(PLUGIN_TYPE.DEVICE, Collections.emptyList())
        ).flatMap(Collection::stream).collect(Collectors.toList());

        for (PluginMeta pluginMeta : pluginsToInitialize) {
            pluginMeta.pluginInstance.initialize();
        }
    }

    public void reset() {
        getCompiler().ifPresent(Compiler::reset);
        getMemory().ifPresent(Plugin::reset);
        getCPU().ifPresent(Plugin::reset);
        getDevices().forEach(Device::reset);
    }

    @Override
    public boolean isConnected(long pluginA, long pluginB) {
        String fst = pluginsById.get(pluginA).pluginConfig.getPluginId();
        String snd = pluginsById.get(pluginB).pluginConfig.getPluginId();

        return computerConfig.getConnections().stream().anyMatch(connection -> {
            boolean oneWay = connection.getFromPluginId().equals(fst) && connection.getToPluginId().equals(snd);
            boolean otherWay = connection.getFromPluginId().equals(snd) && connection.getToPluginId().equals(fst);

            return oneWay || (connection.isBidirectional() && otherWay);
        });
    }

    public Optional<Compiler> getCompiler() {
        List<PluginMeta> meta = Optional.ofNullable(pluginsByType.get(PLUGIN_TYPE.COMPILER)).orElse(Collections.emptyList());
        return meta.stream().map(m -> (Compiler) m.pluginInstance).findFirst();
    }

    public Optional<CPU> getCPU() {
        List<PluginMeta> meta = Optional.ofNullable(pluginsByType.get(PLUGIN_TYPE.CPU)).orElse(Collections.emptyList());
        return meta.stream().map(m -> (CPU) m.pluginInstance).findFirst();
    }

    public Optional<Memory> getMemory() {
        List<PluginMeta> meta = Optional.ofNullable(pluginsByType.get(PLUGIN_TYPE.MEMORY)).orElse(Collections.emptyList());
        return meta.stream().map(m -> (Memory) m.pluginInstance).findFirst();
    }

    public List<Device> getDevices() {
        List<PluginMeta> meta = Optional.ofNullable(pluginsByType.get(PLUGIN_TYPE.DEVICE)).orElse(Collections.emptyList());
        return meta.stream().map(m -> (Device) m.pluginInstance).collect(Collectors.toList());
    }

    @Override
    public void close() {
        computerConfig.close();
    }

    static class PluginMeta {
        final PluginSettings pluginSettings;
        final Plugin pluginInstance;
        final PluginConfig pluginConfig;

        public PluginMeta(PluginSettings pluginSettings, Plugin pluginInstance, PluginConfig pluginConfig) {
            this.pluginSettings = Objects.requireNonNull(pluginSettings);
            this.pluginInstance = Objects.requireNonNull(pluginInstance);
            this.pluginConfig = Objects.requireNonNull(pluginConfig);
        }
    }
}
