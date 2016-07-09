/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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

import emulib.emustudio.SettingsManager;
import emulib.plugins.Plugin;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emulib.runtime.exceptions.PluginInitializationException;
import emulib.runtime.interfaces.PluginConnections;
import emulib.runtime.internal.Unchecked;
import emustudio.architecture.ComputerFactory.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Computer implements PluginConnections {
    private final static Logger LOGGER = LoggerFactory.getLogger(Computer.class);

    private final String name;
    private final List<Device> devices;

    private final Map<Long, List<Long>> connections;
    private final Map<Class<? extends Plugin>, Plugin> plugins;
    private final Collection<PluginInfo> pluginInfos;

    @SuppressWarnings("unchecked")
    public Computer(String name, Collection<PluginInfo> pluginInfos, Map<Long, List<Long>> connections) {
        this.name = Objects.requireNonNull(name);
        this.connections = Collections.unmodifiableMap(new HashMap<>(connections));
        this.pluginInfos = Collections.unmodifiableCollection(new ArrayList<>(pluginInfos));

        List<Device> tmpDevices = new ArrayList<>();
        Map<Class<? extends Plugin>, Plugin> tmpPlugins = new HashMap<>();
        for (PluginInfo plugin : pluginInfos) {
            tmpPlugins.put(plugin.pluginInterface, plugin.getPlugin());
            if (plugin.pluginInterface == Device.class) {
                tmpDevices.add((Device) plugin.getPlugin());
            }
        }

        this.plugins = Collections.unmodifiableMap(tmpPlugins);
        this.devices = Collections.unmodifiableList(tmpDevices);
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public Optional<CPU> getCPU() {
        return Optional.ofNullable((CPU) plugins.get(CPU.class));
    }

    @SuppressWarnings("unchecked")
    public Optional<Compiler> getCompiler() {
        return Optional.ofNullable((Compiler) plugins.get(Compiler.class));
    }

    @SuppressWarnings("unchecked")
    public Optional<Memory> getMemory() {
        return Optional.ofNullable((Memory) plugins.get(Memory.class));
    }

    public Optional<Device> getDevice(int index) {
        if (index < 0 || index >= devices.size()) {
            return Optional.empty();
        }
        return Optional.of(devices.get(index));
    }

    public int getDeviceCount() {
        return devices.size();
    }

    public Iterable<Device> getDevices() {
        return Collections.unmodifiableList(new ArrayList<>(devices));
    }

    public Collection<PluginInfo> getPluginInfos() {
        return pluginInfos;
    }

    public void destroy() {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.destroy();
            } catch (Exception e) {
                LOGGER.error("Could not destroy plugin {}", plugin, e);
            }
        }
    }

    public void initialize(SettingsManager settings) throws PluginInitializationException {
        getCompiler().ifPresent(c -> Unchecked.run(() -> c.initialize(settings)));
        getMemory().ifPresent(m -> Unchecked.run(() -> m.initialize(settings)));
        getCPU().orElseThrow(() -> new PluginInitializationException("CPU is not set")).initialize(settings);

        for (Device device : devices) {
            device.initialize(settings);
        }

        // the last operation - reset of all plugins
        resetPlugins();
    }

    private void resetPlugins() {
        for (Plugin plugin : plugins.values()) {
            plugin.reset();
        }
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
            LOGGER.debug("Could not find connection between pluginID {} and {}", pluginID, toPluginID);
            return false;
        }
        LOGGER.debug("connection({}).contains({}) = {}", pluginID, toPluginID, connection.contains(toPluginID));
        return connection.contains(toPluginID);
    }
}
