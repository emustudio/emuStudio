/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.elements;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.framework.P;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

import java.awt.*;
import java.util.UUID;
import java.util.function.Function;

public class DeviceElement extends Element {
    private final static Color BACK_COLOR = new Color(0xe0e0e0);

    public DeviceElement(P schemaPoint, String pluginName, String pluginFileName) {
        super(
                BACK_COLOR, schemaPoint, UUID.randomUUID().toString(), PLUGIN_TYPE.DEVICE, pluginName, pluginFileName,
                Config.inMemory()
        );
    }

    public DeviceElement(PluginConfig config, Function<P, P> searchGridPoint) {
        super(
                BACK_COLOR, searchGridPoint.apply(P.of(config.getSchemaPoint())), config.getPluginId(), PLUGIN_TYPE.DEVICE,
                config.getPluginName(), config.getPluginFile(), config.getPluginSettings()
        );
    }
}
