/**
 * Computer.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import plugins.IPlugin;
import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import plugins.memory.IMemory;
import runtime.interfaces.IConnections;

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

    private Hashtable<Long, ArrayList<Long>> connections;
    private Hashtable<Long, IPlugin> plugins;
    private Hashtable<IPlugin, Long> pluginsReverse;


    public Computer(ICPU cpu, IMemory memory, ICompiler compiler,
        IDevice[] devices, Hashtable<Long, IPlugin> plugins,
        Hashtable<IPlugin, Long> pluginsReverse,
        Hashtable<Long, ArrayList<Long>> connections) {
        this.cpu = cpu;
        this.memory = memory;
        this.compiler = compiler;
        this.devices = devices;
        this.connections = connections;
        this.plugins = plugins;
        this.pluginsReverse = pluginsReverse;
    }

    public IPlugin getPlugin(long pluginID) {
        return plugins.get(pluginID);
    }

    public ICPU getCPU() {
        return cpu;
    }

    public ICompiler getCompiler() {
        return compiler;
    }

    public IMemory getMemory() {
        return memory;
    }

    public IDevice[] getDevices() {
        return devices;
    }

    public IDevice getDevice(int index) {
        return devices[index];
    }

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
            (!compiler.initialize(pluginsReverse.get(compiler),
                    settings)))
            return false;

        if ((memory != null) &&
            (!memory.initialize(pluginsReverse.get(memory), settings)))
            return false;

        if (!cpu.initialize(pluginsReverse.get(cpu), settings))
            return false;

        int size = devices.length;
        for (int i = 0; i < size; i++)
            if (!devices[i].initialize(pluginsReverse.get(devices[i]), settings))
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

        if ((ar == null) || ar.isEmpty())
            return false;
        if (ar.contains(toPluginID))
            return true;
        return false;
    }
}
