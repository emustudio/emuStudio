/*
 * Computer.java
 * 
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012, Peter Jakubčo
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
 */
package emustudio.architecture;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.Plugin;
import emulib.emustudio.SettingsManager;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emulib.runtime.interfaces.PluginConnections;
import emustudio.architecture.ArchitectureLoader.PluginInfo;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements virtual computer architecture.
 * 
 * @author vbmacher
 */
public class Computer implements PluginConnections {
    private final static Logger logger = LoggerFactory.getLogger(Computer.class);
    private CPU cpu;
    private Compiler compiler;
    private Memory memory;
    private Device[] devices;

    private Map<Long, List<Long>> connections;
    private Map<Long, Plugin> plugins;
    private Collection<PluginInfo> pluginsInfo;

    /**
     * Creates new Computer instance.
     *
     * @param cpu CPU object
     * @param memory Memory object
     * @param compiler Compiler object
     * @param devices array of Device objects
     * @param plugins collection of all plug-ins` information
     * @param connections hashtable with all connections. Keys and values are plug-in IDs.
     */
    public Computer(CPU cpu, Memory memory, Compiler compiler, Device[] devices, Collection<PluginInfo> plugins,
            Map<Long, List<Long>> connections) {
        this.cpu = cpu;
        this.memory = memory;
        this.compiler = compiler;
        this.devices = devices;
        this.connections = connections;
        this.plugins = new HashMap<Long, Plugin>();

        this.pluginsInfo = plugins;
        for (PluginInfo plugin : plugins) {
            this.plugins.put(plugin.pluginId, plugin.plugin);
        }
    }

    /**
     * Get a plug-in by given ID.
     *
     * @param pluginID ID of requested plug-in
     * @return plug-in object
     */
    public Plugin getPlugin(long pluginID) {
        return plugins.get(pluginID);
    }
    
    /**
     * Get all plugins information
     * 
     * @return collection of PluginInfo objects
     */
    public Collection<PluginInfo> getPluginsInfo() {
        return pluginsInfo;
    }

    /**
     * Get CPU plug-in.
     *
     * @return CPU plug-in object
     */
    public CPU getCPU() {
        return cpu;
    }

    /**
     * Get compiler plug-in.
     *
     * @return Compiler plug-in object
     */
    public Compiler getCompiler() {
        return compiler;
    }

    /**
     * Get memory plug-in.
     *
     * @return Memory plug-in object
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * Get array of device plug-ins.
     *
     * @return array of Device plug-in object
     */
    public Device[] getDevices() {
        return devices;
    }

    /**
     * Get a device plug-in by specific position.
     *
     * @param index position of the device in the devices array
     * @return Device plug-in object
     */
    public Device getDevice(int index) {
        return devices[index];
    }

    /**
     * Get devices count.
     *
     * @return devices count
     */
    public int getDeviceCount() {
        return devices.length;
    }

    /**
     * Perform reset of all plugins 
     */
    public void resetPlugins() {
        Collection<Plugin> pluginObjects = plugins.values();
        Iterator<Plugin> iterator = pluginObjects.iterator();
        while (iterator.hasNext()) {
            Plugin plugin = iterator.next();
            plugin.reset();
        }
    }

    /**
     * Destroys this computer
     */
    public void destroy() {
        if (compiler != null) {
            try {
                compiler.destroy();
            } catch (Exception e) {
                logger.error("Could not destroy compiler.", e);
            }
        }
        int size = devices.length;
        for (int i = 0; i < size; i++) {
            try {
                devices[i].destroy();
            } catch (Exception e) {
                logger.error("Could not destroy device.", e);
            }
        }
        try {
            cpu.destroy();
        } catch (Exception e) {
            logger.error("Could not destroy CPU.", e);
        }
        if (memory != null) {
            try {
                memory.destroy();
            } catch (Exception e) {
                logger.error("Could not destroy memory.", e);
            }
        }

        plugins.clear();
        connections.clear();
    }

    /**
     * This method initializes all plug-ins
     *
     * @param settings settings manipulation object
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize(SettingsManager settings) {
        if ((compiler != null) && (!compiler.initialize(settings))) {
            return false;
        }

        if ((memory != null) && (!memory.initialize(settings))) {
            return false;
        }

        if (!cpu.initialize(settings)) {
            return false;
        }

        int size = devices.length;
        for (int i = 0; i < size; i++) {
            if (!devices[i].initialize(settings)) {
                return false;
            }
        }

        // the last operation - reset of all plugins
        resetPlugins();
        return true;
    }

    /**
     * Get plug-in type.
     *
     * @param pluginID plugin ID
     * @return plug-in type. If the plug-in doesn't exist, return null.
     */
    @Override
    public PLUGIN_TYPE getPluginType(long pluginID) {
        Plugin plugin = plugins.get(pluginID);
        if (plugin == null) {
            return null;
        }
        PluginType pluginType = plugin.getClass().getAnnotation(PluginType.class);
        return pluginType.type();
    }

    /**
     * Method determine if plugin <code>plugin1</code>
     * is connected to <code>plugin2</code>.
     *
     * This method is used for determining connections between cpu,memory
     * and devices.
     *
     * @param pluginID  Plugin1
     * @param toPluginID  Plugin2
     * @return true if plugin1 is connected to plugin2; false otherwise
     */
    @Override
    public boolean isConnected(long pluginID, long toPluginID) {
        List<Long> connection = connections.get(pluginID);

        if ((connection == null) || connection.isEmpty()) {
            return false;
        }
        return connection.contains(toPluginID);
    }
}
