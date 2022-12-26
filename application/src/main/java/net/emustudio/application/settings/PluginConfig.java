/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.P;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

import java.nio.file.Path;
import java.util.Objects;

import static net.emustudio.application.settings.ConfigFiles.getAbsolutePluginPath;

public class PluginConfig {
    private final Config config;

    public PluginConfig(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    public static PluginConfig create(String id, PLUGIN_TYPE pluginType, String pluginName, String pluginFile,
                                      P schemaLocation, Config pluginSettings) {
        Config config = Config.inMemory();
        config.set("id", id);
        config.set("type", pluginType.toString());
        config.set("name", pluginName);
        config.set("path", pluginFile);
        config.set("schemaPoint", schemaLocation.toSchemaPoint().toString());
        config.set("settings", pluginSettings);

        return new PluginConfig(config);
    }

    public String getPluginId() {
        return config.get("id");
    }

    public PLUGIN_TYPE getPluginType() {
        String pluginType = config.get("type");
        return PLUGIN_TYPE.valueOf(pluginType);
    }

    public String getPluginName() {
        return config.get("name");
    }

    public String getPluginFile() {
        return config.get("path");
    }

    public Path getPluginPath() {
        return getAbsolutePluginPath(getPluginFile(), getPluginType());
    }

    public SchemaPoint getSchemaPoint() throws NumberFormatException {
        if (!config.contains("schemaPoint")) {
            config.set("schemaPoint", "0,0");
        }

        String schemaPointStr = config.get("schemaPoint");
        return SchemaPoint.parse(schemaPointStr);
    }

    public void setSchemaPoint(SchemaPoint point) {
        config.set("schemaPoint", point.toString());
    }

    public Config getPluginSettings() {
        if (!config.contains("settings")) {
            config.set("settings", config.createSubConfig());
        }
        return config.get("settings");
    }

    public Config getConfig() {
        return config;
    }
}
