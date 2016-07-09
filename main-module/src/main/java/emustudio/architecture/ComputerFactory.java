/*
 * Copyright (C) 2006-2016, Peter Jakubčo
 *
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package emustudio.architecture;

import emulib.plugins.Plugin;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emulib.runtime.ContextPool;
import emulib.runtime.PluginLoader;
import emulib.runtime.exceptions.InvalidPasswordException;
import emulib.runtime.exceptions.InvalidPluginException;
import emustudio.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static emulib.runtime.PluginLoader.doesImplement;
import static java.util.stream.Collectors.toList;

public class ComputerFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(ComputerFactory.class);
    private final static AtomicInteger NEXT_PLUGIN_ID = new AtomicInteger(0);

    private final PluginLoader pluginLoader;

    final static class PluginInfo<T extends Plugin> {
        final String pluginName;
        final String pluginConfigName;
        final long pluginId;
        final File pluginFile;
        final Class<T> pluginInterface;

        Optional<T> plugin = Optional.empty();
        Optional<Class<T>> mainClass = Optional.empty();

        PluginInfo(String pluginConfigName, String pluginName, long pluginId, File pluginFile, Class<T> pluginInterface) {
            this.pluginConfigName = Objects.requireNonNull(pluginConfigName);
            this.pluginName = Objects.requireNonNull(pluginName);
            this.pluginId = pluginId;
            this.pluginFile = Objects.requireNonNull(pluginFile);
            this.pluginInterface = Objects.requireNonNull(pluginInterface);
        }

        Class<T> getMainClass() {
            return mainClass.orElse(null);
        }

        T getPlugin() {
            return plugin.orElse(null);
        }

        @Override
        public String toString() {
            return pluginName;
        }
    }

    public ComputerFactory(PluginLoader pluginLoader) {
        this.pluginLoader = Objects.requireNonNull(pluginLoader);
    }

    public Computer createComputer(String configName, ContextPool contextPool)
        throws ReadConfigurationException, InvalidPluginException {

        Configuration configuration = ComputerConfig.read(configName);

        Map<String, PluginInfo> pluginsToLoad = findPluginsToLoad(configuration);
        loadPlugins(pluginsToLoad.values());
        createPluginInstances(contextPool, pluginsToLoad.values());

        List<PluginInfo> notLoaded = pluginsToLoad.values().stream()
            .filter(p -> !p.plugin.isPresent())
            .collect(toList());
        if (!notLoaded.isEmpty()) {
            LOGGER.error("Not all plugins were loaded. List: {}", notLoaded);
            throw new InvalidPluginException("Not all plugins were loaded.");
        }
        LOGGER.info("All plugins were loaded successfully.");

        Map<Long, List<Long>> connections = preparePluginConnections(configuration, pluginsToLoad);

        return new Computer(configName, pluginsToLoad.values(), connections);
    }

    private Map<String, PluginInfo> findPluginsToLoad(Configuration configuration) {
        Map<String, PluginInfo> pluginsToLoad = new HashMap<>();

        createPluginInfo(configuration, ComputerConfig.COMPILER, Compiler.class).ifPresent(
            p -> pluginsToLoad.put(ComputerConfig.COMPILER, p)
        );
        createPluginInfo(configuration, ComputerConfig.CPU, CPU.class).ifPresent(
            p -> pluginsToLoad.put(ComputerConfig.CPU, p)
        );
        createPluginInfo(configuration, ComputerConfig.MEMORY, Memory.class).ifPresent(
            p -> pluginsToLoad.put(ComputerConfig.MEMORY, p)
        );

        for (int i = 0; configuration.contains(ComputerConfig.DEVICE + i); i++) {
            final int j = i;
            createPluginInfo(configuration, ComputerConfig.DEVICE + i, Device.class).ifPresent(
                p -> pluginsToLoad.put(ComputerConfig.DEVICE + j, p)
            );
        }
        return pluginsToLoad;
    }

    private <T extends Plugin> Optional<PluginInfo> createPluginInfo(Configuration configuration,
                                                                     String pluginConfigName, Class<T> classs) {
        String pluginName = configuration.get(pluginConfigName);
        if (pluginName != null) {
            long pluginID = NEXT_PLUGIN_ID.incrementAndGet();
            LOGGER.debug("[{}] Assigned pluginID={}", pluginConfigName, pluginID);

            File pluginFile = ComputerConfig.getPluginDir(classs).resolve(pluginName + ".jar")
                .toFile();

            return Optional.of(new PluginInfo<>(pluginConfigName, pluginName, pluginID, pluginFile, classs));
        }
        return Optional.empty();
    }

    private void loadPlugins(Collection<PluginInfo> pluginsToLoad) throws InvalidPluginException {
        File[] pluginsFiles = pluginsToLoad.stream()
            .map(p -> p.pluginFile)
            .collect(toList())
            .toArray(new File[pluginsToLoad.size()]);

        try {
            List<Class<Plugin>> mainClasses = pluginLoader.loadPlugins(Main.password, pluginsFiles);
            mainClasses.stream().forEach(
                mainClass -> pluginsToLoad.stream()
                    .filter(p -> !p.mainClass.isPresent())
                    .filter(p -> doesImplement(mainClass, p.pluginInterface))
                    .findFirst()
                    .ifPresent(p -> p.mainClass = Optional.of(mainClass))
            );
        } catch (InvalidPasswordException | IOException e) {
            throw new InvalidPluginException("Could not load plugins", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void createPluginInstances(ContextPool contextPool, Collection<PluginInfo> pluginsToLoad)
        throws InvalidPluginException {

        for (PluginInfo plugin : pluginsToLoad) {
            Class<Plugin> mainClass = plugin.getMainClass();
            plugin.plugin = Optional.of(newPlugin(plugin.pluginId, mainClass, plugin.pluginInterface, contextPool));
        }
    }

    private Map<Long, List<Long>> preparePluginConnections(Configuration configuration, Map<String, PluginInfo> pluginsToLoad) {
        Map<Long, List<Long>> connections = new HashMap<>();
        for (int i = 0; configuration.contains("connection" + i + ".junc0"); i++) {
            // get i-th connection from settings
            String j0 = configuration.get("connection" + i + ".junc0", "");
            String j1 = configuration.get("connection" + i + ".junc1", "");
            boolean bidi = Boolean.parseBoolean(configuration.get("bidirectional", "true"));

            if (j0.equals("") || j1.equals("")) {
                continue;
            }

            // map the connection elements to plug-ins: p1 and p2
            // note the connection: p1 -> p2  (p1 wants to use p2)
            long pID1, pID2;

            PluginInfo pluginInfo = pluginsToLoad.get(j0);
            if (pluginInfo == null) {
                LOGGER.error("Invalid connection, j0=" + j0);
                continue; // invalid connection
            }
            pID1 = pluginInfo.pluginId;

            pluginInfo = pluginsToLoad.get(j1);
            if (pluginInfo == null) {
                LOGGER.error("Invalid connection, j1=" + j1);
                continue; // invalid connection
            }
            pID2 = pluginInfo.pluginId;

            // the first direction
            addConnection(connections, pID1, pID2);
            if (bidi) {
                // if bidirectional, then also the other connection
                addConnection(connections, pID2, pID1);
            }
        }
        return connections;
    }

    private void addConnection(Map<Long, List<Long>> connections, long fromPID, long toPID) {
        if (!connections.containsKey(fromPID)) {
            connections.put(fromPID, new ArrayList<>());
        }
        connections.get(fromPID).add(toPID);
    }

    private Plugin newPlugin(long pluginID, Class<? extends Plugin> mainClass, Class<? extends Plugin> pluginInterface,
                             ContextPool contextPool) throws InvalidPluginException {
        Objects.requireNonNull(mainClass);
        Objects.requireNonNull(pluginInterface);
        Objects.requireNonNull(contextPool);

        // First parameter of constructor is plug-in ID
        Class<?>[] conParameters = {Long.class, ContextPool.class};

        try {
            Constructor<?> con = mainClass.getDeclaredConstructor(conParameters);
            if (con != null) {
                return (Plugin) con.newInstance(pluginID, contextPool);
            } else {
                throw new InvalidPluginException("Constructor of the plug-in is null.");
            }
        } catch (InvalidPluginException e) {
            throw e;
        } catch (Exception | NoClassDefFoundError e) {
            throw new InvalidPluginException("Plug-in main class does not have proper constructor", e);
        }
    }
}
