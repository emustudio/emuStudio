/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.configuration;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class PluginConfig {
    private final Config config;

    public PluginConfig(Config config) {
        this.config = Objects.requireNonNull(config);
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

    public Path getPluginFile() {
        return Path.of(config.<String>get("path"));
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
            config.set("settings", Config.inMemory());
        }
        return config.get("settings");
    }

    public Config getConfig() {
        return config;
    }



    public static PluginConfig create(PLUGIN_TYPE pluginType, String pluginName, Path pluginPath, Point schemaLocation) {
        Config config = Config.inMemory();
        config.set("id", UUID.randomUUID().toString());
        config.set("type", pluginType.toString());
        config.set("name", pluginName);
        config.set("path", pluginPath.toString());
        config.set("schemaPoint", schemaLocation.x + "," + schemaLocation.y);

        return new PluginConfig(config);
    }
}
