/*
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
 */
package emustudio.architecture;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.Plugin;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emulib.runtime.interfaces.PluginConnections;
import emustudio.architecture.ArchitectureLoader.PluginInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Computer implements PluginConnections {
    private final static Logger LOGGER = LoggerFactory.getLogger(Computer.class);
    private final CPU cpu;
    private final Compiler compiler;
    private final Memory memory;
    private final Device[] devices;

    private final Map<Long, List<Long>> connections;
    private final Map<Long, Plugin> plugins;
    private final Collection<PluginInfo> pluginsInfo;

    public Computer(CPU cpu, Memory memory, Compiler compiler, Device[] devices, Collection<PluginInfo> plugins,
            Map<Long, List<Long>> connections) {
        this.cpu = cpu;
        this.memory = memory;
        this.compiler = compiler;
        this.devices = devices;
        this.connections = connections;
        this.plugins = new HashMap<>();

        this.pluginsInfo = plugins;
        for (PluginInfo plugin : plugins) {
            this.plugins.put(plugin.pluginId, plugin.plugin);
        }
    }

    public Plugin getPlugin(long pluginID) {
        return plugins.get(pluginID);
    }

    public Collection<PluginInfo> getPluginsInfo() {
        return pluginsInfo;
    }

    public CPU getCPU() {
        return cpu;
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public Memory getMemory() {
        return memory;
    }

    public Device[] getDevices() {
        return devices;
    }

    public Device getDevice(int index) {
        return devices[index];
    }

    public int getDeviceCount() {
        return devices.length;
    }

    public void resetPlugins() {
        Collection<Plugin> pluginObjects = plugins.values();
        Iterator<Plugin> iterator = pluginObjects.iterator();
        while (iterator.hasNext()) {
            Plugin plugin = iterator.next();
            plugin.reset();
        }
    }

    public void destroy() {
        if (compiler != null) {
            try {
                compiler.destroy();
            } catch (Exception e) {
                LOGGER.error("Could not destroy compiler.", e);
            }
        }
        int size = devices.length;
        for (int i = 0; i < size; i++) {
            try {
                devices[i].destroy();
            } catch (Exception e) {
                LOGGER.error("Could not destroy device.", e);
            }
        }
        try {
            cpu.destroy();
        } catch (Exception e) {
            LOGGER.error("Could not destroy CPU.", e);
        }
        if (memory != null) {
            try {
                memory.destroy();
            } catch (Exception e) {
                LOGGER.error("Could not destroy memory.", e);
            }
        }

        plugins.clear();
        connections.clear();
    }

    public boolean initialize(SettingsManager settings) {
        if ((compiler != null) && (!compiler.initialize(settings))) {
            LOGGER.error("Could not initialize compiler.");
            return false;
        }

        if ((memory != null) && (!memory.initialize(settings))) {
            LOGGER.error("Could not initialize memory.");
            return false;
        }

        if (!cpu.initialize(settings)) {
            LOGGER.error("Could not initialize CPU.");
            return false;
        }

        int size = devices.length;
        for (int i = 0; i < size; i++) {
            if (!devices[i].initialize(settings)) {
                LOGGER.error("Could not initialize device[" + i + "]: " + devices[i].getTitle());
                return false;
            }
        }

        // the last operation - reset of all plugins
        resetPlugins();
        return true;
    }

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
            LOGGER.debug("Could not find connection between pluginID=" + pluginID + " and " + toPluginID);
            return false;
        }
        LOGGER.debug("connection(" + pluginID + ").contains(" + toPluginID+ ") =" + connection.contains(toPluginID));
        return connection.contains(toPluginID);
    }
}
