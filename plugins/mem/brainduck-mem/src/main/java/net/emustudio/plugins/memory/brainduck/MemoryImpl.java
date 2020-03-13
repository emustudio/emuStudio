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
package net.emustudio.plugins.memory.brainduck;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.memory.AbstractMemory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.memory.brainduck.api.RawMemoryContext;
import net.emustudio.plugins.memory.brainduck.gui.MemoryGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.MEMORY,
    title = "BrainDuck memory"
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractMemory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl memContext = new MemoryContextImpl();
    private MemoryGUI memoryGUI;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        try {
            applicationApi.getContextPool().register(pluginID, memContext, RawMemoryContext.class);
            applicationApi.getContextPool().register(pluginID, memContext, MemoryContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register memory context", e);
            applicationApi.getDialogs().showError(
                "Could not register BrainDuck memory. Please see log file for details.", getTitle()
            );
        }
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
        return "Operating memory for abstract BrainDuck architecture";
    }

    @Override
    public void initialize() {
        settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).ifPresent(nogui -> {
            if (!nogui) {
                memoryGUI = new MemoryGUI(memContext);
            }
        });
    }

    @Override
    public void destroy() {
    }

    @Override
    public int getSize() {
        return memContext.getSize();
    }

    @Override
    public void showSettings() {
        if (memoryGUI != null) {
            memoryGUI.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return memoryGUI != null;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.memory.brainduck.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
