/*
 * ArchHandler.java
 * 
 * Created on Friday, 28.1.2008 22:31
 * 
 * KEEP IT SIMPLE STUPID
 * sometimes just... YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

import emustudio.architecture.drawing.Schema;

import java.util.HashMap;
import java.util.Properties;
import emuLib8.plugins.memory.IMemory;
import emuLib8.plugins.cpu.ICPU;
import emuLib8.plugins.device.IDevice;
import emuLib8.plugins.IPlugin;
import emuLib8.plugins.ISettingsHandler;
import emuLib8.plugins.compiler.ICompiler;
import java.util.Iterator;
import java.util.Set;

/**
 * Class holds actual computer configuration - plugins and settings.
 *
 * @author vbmacher
 */
public class ArchHandler implements ISettingsHandler {

    private Computer computer;
    private Properties settings;
    private Schema schema;
    private HashMap<Long, String> pluginNames;

    private static final String EMPTY_STRING = "";

    /**
     * Creates new virtual computer architecture and initializes all plug-ins.
     * 
     * @param arch         Virtual computer, handling the structure of plug-ins
     * @param settings     Architecture settings (Properties)
     * @param schema       Abstract schema of the architecture
     * @param pluginNames  Names of all plug-ins
     * @param auto         If the emuStudio is runned in automatization mode
     * @param nogui        If the "--nogui" parameter was given to emuStudio
     *  
     * @throws Error if initialization of the architecture failed.
     */
    public ArchHandler(Computer arch, Properties settings,
            Schema schema, HashMap<Long, String> pluginNames, boolean auto,
            boolean nogui) throws Error {
        this.computer = arch;
        this.settings = settings;
        this.schema = schema;
        this.pluginNames = pluginNames;

        if (initialize(auto, nogui) == false) {
            throw new Error("Initialization of plugins failed");
        }
    }

    /**
     * Initialize all plugins. The method is called by
     * constructor. Also provides necessary connections.
     * 
     * @return true If the initialization succeeded, false otherwise
     */
    private boolean initialize(boolean auto, boolean nogui) {
        if (auto)
           // Set "auto" setting to "true" to all plugins
           writeSettingToAll("auto", "true");
        if (nogui)
           writeSettingToAll("nogui", "true");

        return computer.initialize(this);
    }

    /**
     * Method destroys current architecture
     */
    public void destroy() {
        computer.destroy();
        pluginNames.clear();
    }

    /**
     * Get schema of this virtual architecture
     * 
     * @return Abstract schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Return the name of the computer (the configuration name).
     *
     * @return name of the virtual computer
     */
    public String getComputerName() {
        return (schema == null) ? "unknown" : schema.getConfigName();
    }

    /**
     * Return Computer object
     *
     * @return virtual computer structure
     */
    public Computer getComputer() {
        return computer;
    }

    /**
     * Method reads value of specified setting from Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param pluginID  identification number of a plugin
     * @param settingName  name of wanted setting
     * @return setting value if exists, or null if not
     */
    @Override
    public String readSetting(long pluginID, String settingName) {
        IPlugin plug = computer.getPlugin(pluginID);
        
        if (plug == null)
            return null;

        String prop = pluginNames.get(pluginID);

        if ((prop == null) || prop.equals(EMPTY_STRING))
            return null;
        
        if ((settingName != null) && (!settingName.equals(EMPTY_STRING)))
            prop += "." + settingName;

        return settings.getProperty(prop, null);
    }

    /**
     * Get device name (file name without extension)
     * 
     * @param index  Index of the device
     * @return device file name without extension, or null
     *         if device is unknown
     */
    public String getDeviceName(int index) {
        return settings.getProperty("device" + index, null);
    }

    /**
     * Get compiler file name, without file extension.
     * 
     * @return compiler name or null
     */
    public String getCompilerName() {
        return settings.getProperty("compiler", null);
    }

    /**
     * Get CPU file name, without file extension.
     * 
     * @return CPU name or null
     */
    public String getCPUName() {
        return settings.getProperty("cpu", null);
    }

    /**
     * Get memory file name, without file extension.
     * 
     * @return memory name or null
     */
    public String getMemoryName() {
        return settings.getProperty("memory", null);
    }

    /**
     * Method writes a value of specified setting to Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param hash         plugin hash, identification of a plugin
     * @param settingName name of wanted setting
     */
    @Override
    public void writeSetting(long hash, String settingName, String val) {
        if (settingName == null || settingName.equals(EMPTY_STRING)) {
            return;
        }

        IPlugin plug = computer.getPlugin(hash);
        if (plug == null) {
            return;
        }

        String prop = EMPTY_STRING;
        if (plug instanceof IDevice) {
            // search for device
            prop = pluginNames.get(hash);
        } else if (plug instanceof ICPU) {
            prop = "cpu";
        } else if (plug instanceof IMemory) {
            prop = "memory";
        } else if (plug instanceof ICompiler) {
            prop = "compiler";
        }

        if (prop.equals(EMPTY_STRING)) {
            return;
        }
        prop += "." + settingName;

        settings.setProperty(prop, val);
        ArchLoader.writeConfig(schema.getConfigName(), settings);
    }

    /**
     * Method removes value of specified setting from Properties for 
     * specified plugin. Setting has to be fully specified.
     * 
     * @param hash         plugin hash, identification of a plugin
     * @param settingName name of wanted setting
     */
    @Override
    public void removeSetting(long hash, String settingName) {
        if (settingName == null || settingName.equals(EMPTY_STRING)) {
            return;
        }

        IPlugin plug = computer.getPlugin(hash);
        if (plug == null) {
            return;
        }

        String prop = EMPTY_STRING;

        if (plug instanceof IDevice) {
            // search for device
            prop = pluginNames.get(hash);
        } else if (plug instanceof ICPU) {
            prop = "cpu";
        } else if (plug instanceof IMemory) {
            prop = "memory";
        } else if (plug instanceof ICompiler) {
            prop = "compiler";
        }

        if (prop.equals(EMPTY_STRING)) {
            return;
        }
        prop += "." + settingName;

        settings.remove(prop);
        ArchLoader.writeConfig(schema.getConfigName(), settings);
    }

    /**
     * This method is used only by the emuStudio to set some common settings
     * that should be set for all plugins.
     *
     * @param hash
     * @param settingName
     * @param val
     */
    public void writeSettingToAll(String settingName, String val) {
        long id;
        Set<Long> set = pluginNames.keySet();
        for (Iterator e = set.iterator(); e.hasNext();) {
            id = (Long)e.next();
            this.writeSetting(id, settingName, val);
        }
    }

}
