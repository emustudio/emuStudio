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
import emulib.runtime.InvalidPasswordException;
import emulib.runtime.StaticDialogs;
import emustudio.architecture.ComputerFactory.PluginInfo;
import emustudio.drawing.Schema;
import emustudio.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsManagerImpl implements SettingsManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsManagerImpl.class);

    private final Schema schema;
    private final Map<Long, String> pluginNames = new HashMap<>();
    private final Configuration configuration;

    public SettingsManagerImpl(Computer computer, Configuration configuration) {
        this.configuration = Objects.requireNonNull(configuration);
        this.schema = configuration.loadSchema();

        for (PluginInfo plugin : computer.getPluginsInfo()) {
            pluginNames.put(plugin.pluginId, plugin.pluginSettingsName);
        }
        initialize();
    }

    private void initialize()  {
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
    }

    public void destroy() {
        pluginNames.clear();
    }

    public Schema getSchema() {
        return schema;
    }

    @Override
    public String readSetting(long pluginID, String settingName) {
        String pluginName = pluginNames.get(pluginID);

        if ((pluginName == null) || pluginName.isEmpty()) {
            return null;
        }

        if ((settingName != null) && !settingName.isEmpty()) {
            pluginName += "." + settingName;
        }

        return configuration.get(pluginName);
    }

    /**
     * Get device name (file name without extension)
     *
     * @param index  Index of the device
     * @return device file name without extension, or null
     *         if device is unknown
     */
    public String getDeviceName(int index) {
        return configuration.get("device" + index);
    }

    public String getCompilerName() {
        return configuration.get("compiler");
    }

    public String getCPUName() {
        return configuration.get("cpu");
    }

    public String getMemoryName() {
        return configuration.get("memory");
    }

    @Override
    public boolean writeSetting(long pluginID, String settingName, String val) {
        String prop = pluginNames.get(pluginID);

        if ((prop == null) || prop.isEmpty()) {
            return false;
        }

        if ((settingName != null) && !settingName.isEmpty()) {
            prop += "." + settingName;
        }

        configuration.set(prop, val);
        try {
            configuration.write();
        } catch (WriteConfigurationException e) {
            LOGGER.error("[pluginID=" + pluginID + "; " + settingName + "=" + val + "] Could not write setting", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean removeSetting(long pluginID, String settingName) {
        String prop = pluginNames.get(pluginID);

        if ((prop == null) || prop.isEmpty()) {
            return false;
        }

        if ((settingName != null) && !settingName.isEmpty()) {
            prop += "." + settingName;
        }

        configuration.remove(prop);
        try {
            configuration.write();
        } catch (WriteConfigurationException e) {
            LOGGER.error("[pluginID=" + pluginID + "; " + settingName + "] Could not remove setting", e);
            return false;
        }
        return true;
    }

    public boolean writeSetting(String settingName, String value) {
        boolean result = true;
        for (Long pluginID : pluginNames.keySet()) {
            result = result && writeSetting(pluginID, settingName, value);
        }
        return result;
    }

}
