/*
 * Copyright (C) 2009-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.brainduck.memory.impl;

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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@PluginType(type = PLUGIN_TYPE.MEMORY,
title = "BrainDuck memory",
copyright = "\u00A9 Copyright 2009-2014, Peter Jakubčo",
description = "Operating memory for abstract BrainDuck architecture")
public class MemoryImpl extends AbstractMemory {
    private MemoryContextImpl memContext;
    private int size;

    public MemoryImpl(Long pluginID) {
        super(pluginID);
        memContext = new MemoryContextImpl();
        try {
            ContextPool.getInstance().register(pluginID, memContext, MemoryContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register Brainduck memory",
                    MemoryImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.brainduck.memory.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        this.size = 65536;
        memContext.init(size);
    }

    @Override
    public void destroy() {
        memContext.destroy();
        memContext = null;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void showSettings() {
        // we don't have any gui
        StaticDialogs.showMessage("BrainDuck memory doesn't support GUI.");
    }


    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

}
