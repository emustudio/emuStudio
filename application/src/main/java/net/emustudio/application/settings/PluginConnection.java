/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PluginConnection {

    private final Config config;

    public PluginConnection(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    public static PluginConnection create(String fromPluginId, String toPluginId, boolean bidirectional, List<SchemaPoint> schemaPoints) {
        Config config = Config.inMemory();
        config.set("from", Objects.requireNonNull(fromPluginId));
        config.set("to", Objects.requireNonNull(toPluginId));
        config.set("bidirectional", bidirectional);
        config.set("points", schemaPoints.stream().map(SchemaPoint::toString).collect(Collectors.toList()));

        return new PluginConnection(config);
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
}
