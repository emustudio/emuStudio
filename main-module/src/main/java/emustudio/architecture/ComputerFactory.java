/*
 * Copyright (C) 2014, Peter Jakubčo
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
import emulib.runtime.InvalidPasswordException;
import emulib.runtime.InvalidPluginException;
import emulib.runtime.PluginLoader;
import emustudio.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ComputerFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(ComputerFactory.class);

    private static long nextPluginID = 0;
    private final PluginLoader pluginLoader;

    private final ConfigurationFactory configurationManager;

    public static class PluginInfo {
        public final String pluginSettingsName;
        public final String pluginName;
        public final Class<?> pluginInterface;
        public final long pluginId;
        public Plugin plugin;
        public final String dirName;
        public Class<Plugin> mainClass;

        public PluginInfo(String pluginSettingsName, String dirName,
                String pluginName, Class<?> pluginInterface, long pluginId) {
            this.dirName = dirName;
            this.pluginId = pluginId;
            this.pluginInterface = pluginInterface;
            this.pluginName = pluginName;
            this.pluginSettingsName = pluginSettingsName;
        }
    }

    public ComputerFactory(ConfigurationFactory configurationManager, PluginLoader pluginLoader) {
        this.configurationManager = Objects.requireNonNull(configurationManager);
        this.pluginLoader = Objects.requireNonNull(pluginLoader);
    }

    private Map<String, PluginInfo> preparePluginsToLoad(Configuration configuration) {
        Map<String, PluginInfo> pluginsToLoad = new HashMap<>();

        String tmp = configuration.get("compiler");
        if (tmp != null) {
            long id = createPluginID();
            LOGGER.debug("Assigned compiler pluginID=" + id);
            pluginsToLoad.put("compiler", new PluginInfo("compiler", ConfigurationFactory.COMPILERS_DIR, tmp, Compiler.class, id));
        }
        tmp = configuration.get("cpu");
        if (tmp != null) {
            long id = createPluginID();
            LOGGER.debug("Assigned CPU pluginID=" + id);
            pluginsToLoad.put("cpu", new PluginInfo("cpu", ConfigurationFactory.CPUS_DIR, tmp, CPU.class, id));
        }
        tmp = configuration.get("memory");
        if (tmp != null) {
            long id = createPluginID();
            LOGGER.debug("Assigned memory pluginID=" + id);
            pluginsToLoad.put("memory", new PluginInfo("memory", ConfigurationFactory.MEMORIES_DIR, tmp, Memory.class, id));
        }
        for (int i = 0; configuration.contains("device" + i); i++) {
            tmp = configuration.get("device" + i);
            if (tmp != null) {
                long id = createPluginID();
                LOGGER.debug("Assigned device[" + i + "] pluginID=" + id);
                pluginsToLoad.put("device" + i, new PluginInfo("device" + i, ConfigurationFactory.DEVICES_DIR, tmp, Device.class, id));
            }
        }
        return pluginsToLoad;
    }

    private void loadPlugins(Map<String, PluginInfo> pluginsToLoad) throws InvalidPluginException {
        for (PluginInfo plugin : pluginsToLoad.values()) {
            Class<Plugin> mainClass = loadPlugin(plugin.dirName, plugin.pluginName);
            plugin.mainClass = mainClass;
        }
        LOGGER.info("All plugins are loaded and resolved.");
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
            if (connections.containsKey(pID1)) {
                connections.get(pID1).add(pID2);
            } else {
                List<Long> ar = new ArrayList<>();
                ar.add(pID2);
                connections.put(pID1, ar);
            }
            if (bidi) {
                // if bidirectional, then also the other connection
                if (connections.containsKey(pID2)) {
                    connections.get(pID2).add(pID1);
                } else {
                    List<Long> ar = new ArrayList<>();
                    ar.add(pID1);
                    connections.put(pID2, ar);
                }
            }
        }
        return connections;
    }

    public Computer createComputer(String configName, ContextPool contextPool) throws ReadConfigurationException,
            InvalidPluginException {
        Configuration configuration = configurationManager.read(configName);
        Map<String, PluginInfo> pluginsToLoad = preparePluginsToLoad(configuration);
        loadPlugins(pluginsToLoad);

        Compiler compiler = null;
        CPU cpu = null;
        Memory mem = null;
        List<Device> devList = new ArrayList<>();
        for (PluginInfo plugin : pluginsToLoad.values()) {
            plugin.plugin = newPlugin(plugin.pluginId, plugin.mainClass, plugin.pluginInterface, contextPool);
            if (plugin.plugin instanceof Compiler) {
                compiler = (Compiler) plugin.plugin;
            } else if (plugin.plugin instanceof CPU) {
                cpu = (CPU) plugin.plugin;
            } else if (plugin.plugin instanceof Memory) {
                mem = (Memory) plugin.plugin;
            } else if (plugin.plugin instanceof Device) {
                devList.add((Device) plugin.plugin);
            }
        }

        Map<Long, List<Long>> connections = preparePluginConnections(configuration, pluginsToLoad);
        Collections.reverse(devList);
        Device[] devices = (Device[]) devList.toArray(new Device[0]);

        Computer computer = new Computer(configName, cpu, mem, compiler, devices, pluginsToLoad.values(), connections);
        return computer;
    }

    private long createPluginID() {
    	return nextPluginID++;
    }

    private Class<Plugin> loadPlugin(String directory, String pluginName) throws InvalidPluginException {
        try {
            File file = new File(
                    ConfigurationFactory.getConfigurationBaseDirectory() + File.separator + directory
                            + File.separator + pluginName + ".jar"
            );
            return pluginLoader.loadPlugin(file, Main.password);
        } catch (InvalidPasswordException e) {
            throw new InvalidPluginException("Could not load plugin " + pluginName, e);
        }
    }

    private Plugin newPlugin(long pluginID, Class<Plugin> mainClass, Class<?> pluginInterface, ContextPool contextPool)
            throws InvalidPluginException {
        Objects.requireNonNull(mainClass);
        Objects.requireNonNull(pluginInterface);
        Objects.requireNonNull(contextPool);

        if (!PluginLoader.doesImplement(mainClass, pluginInterface)) {
            throw new InvalidPluginException("Plug-in main class does not implement specified interface");
        }

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
