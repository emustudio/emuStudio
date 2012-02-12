/**
 * Computer.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco@gmail.com>
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

import emulib.plugins.IPlugin;
import emulib.plugins.ISettingsHandler;
import emulib.plugins.compiler.ICompiler;
import emulib.plugins.cpu.ICPU;
import emulib.plugins.device.IDevice;
import emulib.plugins.memory.IMemory;
import emulib.runtime.interfaces.IConnections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * This class implements virtual computer architecture.
 * 
 * @author vbmacher
 */
public class Computer implements IConnections {
    private ICPU cpu;
    private ICompiler compiler;
    private IMemory memory;
    private IDevice[] devices;

    private Map<Long, ArrayList<Long>> connections;
    private Map<Long, IPlugin> plugins;
    private Map<IPlugin, Long> pluginsReverse;


    /**
     * Creates new Computer instance.
     *
     * @param cpu ICPU object
     * @param memory IMemory object
     * @param compiler ICompiler object
     * @param devices array of IDevice objects
     * @param plugins hashtable with all plug-ins, the keys are plug-in IDs and
     * values are plug-in objects.
     * @param pluginsReverse hashtable with all plug-ins, the keys are plug-in
     * objects, and values are plug-in IDs.
     * @param connections hashtable with all connections. Keys and values are
     * plug-in IDs.
     */
    public Computer(ICPU cpu, IMemory memory, ICompiler compiler,
        IDevice[] devices, Map<Long, IPlugin> plugins,
        Map<IPlugin, Long> pluginsReverse,
        Map<Long, ArrayList<Long>> connections) {
        this.cpu = cpu;
        this.memory = memory;
        this.compiler = compiler;
        this.devices = devices;
        this.connections = connections;
        this.plugins = plugins;
        this.pluginsReverse = pluginsReverse;
    }

    /**
     * Get a plug-in by given ID.
     *
     * @param pluginID ID of requested plug-in
     * @return plug-in object
     */
    public IPlugin getPlugin(long pluginID) {
        return plugins.get(pluginID);
    }

    /**
     * Get CPU plug-in.
     *
     * @return ICPU plug-in object
     */
    public ICPU getCPU() {
        return cpu;
    }

    /**
     * Get compiler plug-in.
     *
     * @return ICompiler plug-in object
     */
    public ICompiler getCompiler() {
        return compiler;
    }

    /**
     * Get memory plug-in.
     *
     * @return IMemory plug-in object
     */
    public IMemory getMemory() {
        return memory;
    }

    /**
     * Get array of device plug-ins.
     *
     * @return array of IDevice plug-in object
     */
    public IDevice[] getDevices() {
        return devices;
    }

    /**
     * Get a device plug-in by specific position.
     *
     * @param index position of the device in the devices array
     * @return IDevice plug-in object
     */
    public IDevice getDevice(int index) {
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
        Collection<IPlugin> p = plugins.values();
        Iterator<IPlugin> i = p.iterator();
        while (i.hasNext()) {
            IPlugin pl = i.next();
            pl.reset();
        }
    }

    /**
     * Destroys this computer
     */
    public void destroy() {
        try {
            if (compiler != null)
                compiler.destroy();

            int size = devices.length;
            for (int i = 0; i < size; i++)
                devices[i].destroy();
            cpu.destroy();
            
            if (memory != null)
                memory.destroy();

            plugins.clear();
            pluginsReverse.clear();
            connections.clear();
        } catch (Exception e) {}
    }
    /**
     * This method initializes all plug-ins
     *
     * @param settings settings manipulation object
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize(ISettingsHandler settings) {
        if ((compiler != null) &&
            (!compiler.initialize(settings)))
            return false;

        if ((memory != null) &&
            (!memory.initialize(settings)))
            return false;

        if (!cpu.initialize(settings))
            return false;

        int size = devices.length;
        for (int i = 0; i < size; i++)
            if (!devices[i].initialize(settings))
                return false;

        // the last operation - reset of all plugins
        resetPlugins();
        return true;
    }

    /**
     * Get plug-in type.
     *
     * @param pluginID plugin ID
     * @return plug-in type (TYPE_CPU, TYPE_MEMORY, TYPE_DEVICE, TYPE_COMPILER).
     *         If the plug-in is of unknown type, return TYPE_UNKNOWN.
     */
    @Override
    public int getPluginType(long pluginID) {
        IPlugin p = plugins.get(pluginID);
        if (p == null)
            return TYPE_UNKNOWN;
        if (p instanceof ICPU)
            return TYPE_CPU;
        else if (p instanceof IMemory)
            return TYPE_MEMORY;
        else if (p instanceof IDevice)
            return TYPE_DEVICE;
        else if (p instanceof ICompiler)
            return TYPE_COMPILER;
        return TYPE_UNKNOWN;
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
        ArrayList<Long> ar = connections.get(pluginID);

        if ((ar == null) || ar.isEmpty()) {
            return false;
        }
        if (ar.contains(toPluginID)) {
            return true;
        }
        return false;
    }
}
