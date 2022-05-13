/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
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
import net.emustudio.plugins.memory.rasp.gui.MemoryDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@SuppressWarnings("unused")
@PluginRoot(
    type = PLUGIN_TYPE.MEMORY,
    title = "RASP Memory"
)
public class MemoryImpl extends AbstractMemory {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl context;
    private MemoryDialog gui;
    private final boolean guiNotSupported;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.context = new MemoryContextImpl();

        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
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
    public void showSettings(JFrame parent) {
        if (!guiNotSupported) {
            if (gui == null) {
                gui = new MemoryDialog(parent, context, applicationApi.getDialogs());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "RASP memory containing the program as well as the data";
    }

    @Override
    public int getProgramLocation() {
        return context.getProgramLocation();
    }

    @Override
    public void setProgramLocation(int programLocation) {
        context.setProgramLocation(programLocation);
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.memory.rasp.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
