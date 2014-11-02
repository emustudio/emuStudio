/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2010-2014, Peter Jakubčo
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
import emulib.plugins.PluginInitializationException;
import emulib.runtime.InvalidPasswordException;
import emulib.runtime.StaticDialogs;
import emustudio.architecture.ArchitectureLoader.PluginInfo;
import emustudio.drawing.Schema;
import emustudio.main.Main;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class holds actual computer configuration - plugins and settings.
 *
 */
public class ArchitectureManager implements SettingsManager {
    private final static Logger logger = LoggerFactory.getLogger(ArchitectureManager.class);

    private final Computer computer;
    private final Properties settings;
    private final Schema schema;
    private final Map<Long, String> pluginNames;
    private ConfigurationManager configurationManager = null;

    /**
     * Creates new virtual computer architecture and initializes all plug-ins.
     *
     * @param computer Virtual computer, handling the structure of plug-ins
     * @param settings ArchitectureManager settings (Properties)
     * @param schema   Abstract schema of the architecture
     * @param configurationManager
     *
     * @throws PluginInitializationException if initialization of the architecture failed.
     */
    public ArchitectureManager(Computer computer, Properties settings, Schema schema,
            ConfigurationManager configurationManager) throws PluginInitializationException {
        this.computer = computer;
        this.settings = settings;
        this.schema = schema;
        this.pluginNames = new HashMap<>();
        this.configurationManager = configurationManager;

        for (PluginInfo plugin : computer.getPluginsInfo()) {
            pluginNames.put(plugin.pluginId, plugin.pluginSettingsName);
        }
        initialize();
    }

    /**
     * Initialize all plugins. The method is called by
     * constructor. Also provides necessary connections.
     *
     * @return true If the initialization succeeded, false otherwise
     */
    private void initialize() throws PluginInitializationException {
        if (Main.commandLine.autoWanted()) {
           // Set "auto" setting to "true" to all plugins
           writeSetting(SettingsManager.AUTO, "true");
        }
        if (Main.commandLine.noGUIWanted()) {
           writeSetting(SettingsManager.NO_GUI, "true");
           try {
               StaticDialogs.setGUISupported(false, Main.password);
           } catch (InvalidPasswordException e) {
               // does not happen
           }
        }
        computer.initialize(this);
    }

    /**
     * Method destroys current architecture
     */
    public void destroy() {
        computer.destroy();
        pluginNames.clear();
    }

    public Schema getSchema() {
        return schema;
    }

    public String getComputerName() {
        return (schema == null) ? "unknown" : schema.getConfigName();
    }

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
        String prop = pluginNames.get(pluginID);

        if ((prop == null) || prop.isEmpty()) {
            return null;
        }

        if ((settingName != null) && !settingName.isEmpty()) {
            prop += "." + settingName;
        }

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

    public String getCompilerName() {
        return settings.getProperty("compiler", null);
    }

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
     * @param pluginID  plugin ID, identification of a plugin
     * @param settingName name of wanted setting
     * @param val new value of the setting
     * @return true if the setting was successfully saved. It returns false if pluginID wasn't recognized or some error
     *         happened.
     */
    @Override
    public boolean writeSetting(long pluginID, String settingName, String val) {
        String prop = pluginNames.get(pluginID);

        if ((prop == null) || prop.isEmpty()) {
            return false;
        }

        if ((settingName != null) && !settingName.isEmpty()) {
            prop += "." + settingName;
        }

        settings.setProperty(prop, val);
        if (configurationManager != null) {
            try {
                configurationManager.writeConfiguration(schema.getConfigName(), settings);
            } catch (WriteConfigurationException e) {
                logger.error("[pluginID=" + pluginID + "; " + settingName + "=" + val + "] Could not write setting", e);
                return false;
            }
        } else {
            logger.debug("ConfigurationManager is null. Setting is not saved.");
        }
        return true;
    }

    /**
     * Method removes value of specified setting from Properties for
     * specified plugin. Setting has to be fully specified.
     *
     * @param pluginID    plugin ID, identification of a plugin
     * @param settingName name of wanted setting
     * @return true if the setting was removed, or it didn't exist so far. It returns false if pluginID wasn't recognized
     *         or some error happened.
     */
    @Override
    public boolean removeSetting(long pluginID, String settingName) {
        String prop = pluginNames.get(pluginID);

        if ((prop == null) || prop.isEmpty()) {
            return false;
        }

        if ((settingName != null) && !settingName.isEmpty()) {
            prop += "." + settingName;
        }

        settings.remove(prop);
        if (configurationManager != null) {
            try {
                configurationManager.writeConfiguration(schema.getConfigName(), settings);
            } catch (WriteConfigurationException e) {
                logger.error("[pluginID=" + pluginID + "; " + settingName + "] Could not remove setting", e);
                return false;
            }
        } else {
            logger.debug("ConfigurationManager is null. Setting removal is not saved.");
        }
        return true;
    }

    /**
     * Set a setting for all plugins.
     *
     * This method should be used only by the emuStudio. It does not override any of the SettingsManager method.
     *
     * @param settingName name of the setting
     * @param value value of the setting
     * @return true if at least one setting was successfully saved; false otherwise.
     */
    public boolean writeSetting(String settingName, String value) {
        boolean result = true;
        for (Long pluginID : pluginNames.keySet()) {
            result = result && writeSetting(pluginID, settingName, value);
        }
        return result;
    }

}
