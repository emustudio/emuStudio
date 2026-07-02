/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
    private final AutoCloseable pluginClassLoader;

    private final Map<Long, PluginMeta> pluginsById = new HashMap<>();
    private final Map<PLUGIN_TYPE, List<PluginMeta>> pluginsByType = new HashMap<>();
    private boolean destroyed;

    public VirtualComputer(ComputerConfig computerConfig, Map<Long, PluginMeta> plugins) {
        this(computerConfig, plugins, null);
    }

    public VirtualComputer(ComputerConfig computerConfig, Map<Long, PluginMeta> plugins, AutoCloseable pluginClassLoader) {
        this.computerConfig = Objects.requireNonNull(computerConfig);
        this.pluginClassLoader = pluginClassLoader;
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
        LoadedPlugins loadedPlugins = loadPlugins(computerConfig, applicationApi, appSettings);
        return new VirtualComputer(computerConfig, loadedPlugins.plugins, loadedPlugins.pluginClassLoader);
    }

    private static LoadedPlugins loadPlugins(
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
        PluginLoader.LoadResult loadResult = pluginLoader.loadPlugins(filesToLoad);

        Map<Long, PluginMeta> plugins = constructPlugins(
                loadResult.getPluginClasses(), pluginConfigs, applicationApi, appSettings, computerConfig.getConfig()::save
        );
        return new LoadedPlugins(loadResult.getClassLoader(), plugins);
    }

    // package-private for testing
    static Map<Long, PluginMeta> constructPlugins(
            Map<File, Class<Plugin>> pluginClassesByFile,
            List<PluginConfig> pluginConfigs,
            ApplicationApi applicationApi,
            AppSettings appSettings,
            Runnable save
    ) throws InvalidPluginException {
        if (pluginClassesByFile.size() != pluginConfigs.size()) {
            throw new InvalidPluginException("Plugin class count does not match plugin configuration count");
        }

        Map<Long, PluginMeta> plugins = new LinkedHashMap<>();
        AtomicLong pluginIdCounter = new AtomicLong(1); // 0 is reserved for emuStudio

        for (PluginConfig pluginConfig : pluginConfigs) {
            Class<Plugin> pluginClass = pluginClassesByFile.get(pluginConfig.getPluginPath().toFile());
            if (pluginClass == null) {
                throw new InvalidPluginException("Missing loaded plugin class for " + pluginConfig.getPluginPath());
            }
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

    // package-private for testing
    static Plugin createPluginInstance(long pluginID, Class<? extends Plugin> mainClass, ApplicationApi applicationApi,
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
        if (destroyed) {
            return;
        }
        destroyed = true;
        List<PluginMeta> devices = new ArrayList<>(pluginsByType.getOrDefault(PLUGIN_TYPE.DEVICE, Collections.emptyList()));
        Collections.reverse(devices);
        devices.forEach(meta -> safeDestroy(meta.pluginInstance));
        getCPU().ifPresent(this::safeDestroy);
        getMemory().ifPresent(this::safeDestroy);
        getCompiler().ifPresent(this::safeDestroy);
        safeCloseClassLoader();
        computerConfig.close();
    }

    private void safeDestroy(Plugin plugin) {
        try {
            plugin.destroy();
        } catch (Exception e) {
            LOGGER.error("Plugin {} failed to destroy", plugin, e);
        }
    }

    private void safeCloseClassLoader() {
        if (pluginClassLoader == null) {
            return;
        }
        try {
            pluginClassLoader.close();
        } catch (Exception e) {
            LOGGER.error("Plugin class loader failed to close", e);
        }
    }

    private static final class LoadedPlugins {
        private final AutoCloseable pluginClassLoader;
        private final Map<Long, PluginMeta> plugins;

        private LoadedPlugins(AutoCloseable pluginClassLoader, Map<Long, PluginMeta> plugins) {
            this.pluginClassLoader = pluginClassLoader;
            this.plugins = plugins;
        }
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
