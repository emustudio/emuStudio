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

/**
 * This class represents a compiler element. It is used in the abstract schema
 * editor. It corresponds to a compiler object that will be used in the emulated
 * computer.
 */
public class CompilerElement extends Element {
    private final static Color BACK_COLOR = new Color(0xeeefff);

    public CompilerElement(P schemaPoint, String pluginName, String pluginFileName) {
        super(
                BACK_COLOR, schemaPoint, UUID.randomUUID().toString(), PLUGIN_TYPE.COMPILER, pluginName, pluginFileName,
                Config.inMemory()
        );
    }

    public CompilerElement(PluginConfig config, Function<P, P> searchGridPoint) {
        super(
                BACK_COLOR, searchGridPoint.apply(P.of(config.getSchemaPoint())), config.getPluginId(), PLUGIN_TYPE.COMPILER,
                config.getPluginName(), config.getPluginFile(), config.getPluginSettings()
        );
    }
}
