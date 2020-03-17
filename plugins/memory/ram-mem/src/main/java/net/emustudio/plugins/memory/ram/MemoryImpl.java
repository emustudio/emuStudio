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
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.memory.AbstractMemory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.*;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.gui.MemoryDialog;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.MEMORY,
    title = "RAM Program Tape"
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractMemory {
    private final MemoryContextImpl context;
    private MemoryDialog gui;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        ContextPool contextPool = applicationApi.getContextPool();

        context = new MemoryContextImpl();
        try {
            contextPool.register(pluginID, context, RAMMemoryContext.class);
            contextPool.register(pluginID, context, MemoryContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            applicationApi.getDialogs().showError("Could not register Program tape context", super.getTitle());
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
        return "Read-only program tape for abstract RAM machine.";
    }

    @Override
    public int getProgramLocation() {
        return 0;
    }

    @Override
    public int getSize() {
        return context.getSize();
    }

    @Override
    public void setProgramLocation(int location) {
        // Program start is always 0
    }

    @Override
    public void showSettings() {
        if (gui == null) {
            gui = new MemoryDialog(context, applicationApi.getDialogs());
        }
        gui.setVisible(true);
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
    public void reset() {
        context.clearInputs();
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.memory.ram.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
