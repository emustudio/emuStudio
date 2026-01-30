/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.elements;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.framework.P;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

import java.awt.*;
import java.util.UUID;
import java.util.function.Function;

public class MemoryElement extends Element {
    private final static Color BACK_COLOR = new Color(0xeeeeee);

    public MemoryElement(P schemaPoint, String pluginName, String pluginFileName) {
        super(
                BACK_COLOR, schemaPoint, UUID.randomUUID().toString(), PLUGIN_TYPE.MEMORY, pluginName, pluginFileName,
                Config.inMemory()
        );
    }

    public MemoryElement(PluginConfig config, Function<P, P> searchGridPoint) {
        super(
                BACK_COLOR, searchGridPoint.apply(P.of(config.getSchemaPoint())), config.getPluginId(), PLUGIN_TYPE.MEMORY,
                config.getPluginName(), config.getPluginFile(), config.getPluginSettings()
        );
    }
}
