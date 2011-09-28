/**
 * BrainDuckMem.java
 *
 * KISS, YAGNI
 * 
 * Copyright (C) 2009-2011 Peter Jakubčo <pjakubco at gmail.com>
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
package brainduckmem.impl;

import emuLib8.plugins.ISettingsHandler;
import emuLib8.plugins.memory.IMemoryContext;
import emuLib8.plugins.memory.SimpleMemory;
import emuLib8.runtime.Context;
import emuLib8.runtime.StaticDialogs;

public class BrainDuckMem extends SimpleMemory {

    private BrainMemContext memContext;
    private int size;

    public BrainDuckMem(Long pluginID) {
        super(pluginID);
        memContext = new BrainMemContext();
        boolean b = Context.getInstance().register(pluginID, memContext,
                IMemoryContext.class);
        if (!b)
            StaticDialogs.showErrorMessage("Could not register memory context");
    }

    @Override
    public String getTitle() {
        return "BrainDuck OM";
    }

    @Override
    public String getVersion() {
        return "0.11b";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2010, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "BrainDuck operating memory.";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);
        this.size = 65536;
        memContext.init(size);
        return true;
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
        // my nemáme GUI
        StaticDialogs.showMessage("BrainDuck memory doesn't support GUI.");
    }


    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
