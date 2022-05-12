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
package net.emustudio.application.configuration;

import com.electronwill.nightconfig.core.Config;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PluginConnection {

    private final Config config;

    public PluginConnection(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    public String getFromPluginId() {
        return config.get("from");
    }

    public String getToPluginId() {
        return config.get("to");
    }

    public boolean isBidirectional() {
        return config.get("bidirectional");
    }

    public List<SchemaPoint> getSchemaPoints() {
        List<String> points = config.get("points");
        return points.stream().map(SchemaPoint::parse).collect(Collectors.toList());
    }

    public Config getConfig() {
        return config;
    }

    public static PluginConnection create(String fromPluginId, String toPluginId, boolean bidirectional, List<SchemaPoint> schemaPoints) {
        Config config = Config.inMemory();
        config.set("from", Objects.requireNonNull(fromPluginId));
        config.set("to", Objects.requireNonNull(toPluginId));
        config.set("bidirectional", bidirectional);
        config.set("points", schemaPoints.stream().map(SchemaPoint::toString).collect(Collectors.toList()));

        return new PluginConnection(config);
    }
}
