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
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.memory.AbstractMemory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.ssem.gui.MemoryGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.MEMORY,
        title = "SSEM memory (Williams–Kilburn Tube)"
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractMemory {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl context;
    private final boolean guiNotSupported;
    private MemoryGui memoryGUI;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.context = new MemoryContextImpl(getAnnotations());
        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        try {
            applicationApi.getContextPool().register(pluginID, context, MemoryContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register SSEM memory context", e);
            applicationApi.getDialogs().showError(
                    "Could not register SSEM memory. Please see log file for more details.", getTitle()
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
        return "Main store for SSEM machine";
    }

    @Override
    public void destroy() {
    }

    @Override
    public void showSettings(JFrame parent) {
        if (!guiNotSupported) {
            if (memoryGUI == null) {
                memoryGUI = new MemoryGui(parent, context, applicationApi);
            }
            memoryGUI.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.memory.ssem.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    @Override
    public int getSize() {
        return context.getSize();
    }
}
