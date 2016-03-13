/*
 * KISS, YAGNI, DRY

 * Copyright (C) 2016 Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.memory.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.memory.AbstractMemory;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.ssem.memory.gui.MemoryGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.MEMORY,
        title = "SSEM memory",
        copyright = "\u00A9 Copyright 2016, Peter Jakubčo",
        description = "Main store for SSEM machine"
)
public class MemoryImpl extends AbstractMemory {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl memContext = new MemoryContextImpl();
    private MemoryGUI memoryGUI;

    public MemoryImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        try {
            contextPool.register(pluginID, memContext, MemoryContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register SSEM memory", getTitle());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ssem.memory.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            LOGGER.error("Could not load resource file", e);
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);

        if (!Boolean.parseBoolean(settings.readSetting(pluginID, SettingsManager.NO_GUI))) {
            memoryGUI = new MemoryGUI(memContext);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public int getSize() {
        return MemoryContextImpl.NUMBER_OF_CELLS;
    }

    @Override
    public void showSettings() {
        if (memoryGUI != null) {
            memoryGUI.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

}
