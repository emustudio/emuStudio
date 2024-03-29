/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.application.gui.schema.elements;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.P;
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
