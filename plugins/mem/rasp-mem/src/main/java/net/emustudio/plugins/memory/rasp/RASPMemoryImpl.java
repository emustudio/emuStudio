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

package net.emustudio.plugins.memory.rasp;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.memory.AbstractMemory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;
import net.emustudio.plugins.memory.rasp.gui.MemoryWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@SuppressWarnings("unused")
@PluginRoot(
    type = PLUGIN_TYPE.MEMORY,
    title = "RASP Memory"
)
public class RASPMemoryImpl extends AbstractMemory {
    private final static Logger LOGGER = LoggerFactory.getLogger(RASPMemoryImpl.class);

    private final RASPMemoryContextImpl context;
    private MemoryWindow gui;

    public RASPMemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.context = new RASPMemoryContextImpl();

        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                pool.register(pluginID, context, RASPMemoryContext.class);
                pool.register(pluginID, context, MemoryContext.class);
            } catch (InvalidContextException | ContextAlreadyRegisteredException ex) {
                LOGGER.error("Could not register RASP memory context", ex);
                applicationApi.getDialogs().showError(
                    "Could not register RASP memory context. Please see log file for details.", super.getTitle()
                );
            }
        });
    }

    @Override
    public int getSize() {
        return context.getSize();
    }

    @Override
    public void destroy() {
        context.destroy();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    @Override
    public void showSettings() {
        if (gui == null) {
            gui = new MemoryWindow(context, applicationApi.getDialogs());
        }
        gui.setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.emustudio.plugins.memory.rasp.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2016, Michal Šipoš";
    }

    @Override
    public String getDescription() {
        return "RASP memory containing the program as well as the data";
    }

    @Override
    public int getProgramLocation() {
        return context.getProgramLocation();
    }
}
