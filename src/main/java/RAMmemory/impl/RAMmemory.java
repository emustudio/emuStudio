/**
 * RAMmemory.java
 * 
 *  KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
package RAMmemory.impl;

import RAMmemory.gui.MemoryWindow;
import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;
import interfaces.C8E258161A30C508D5E8ED07CE943EEF7408CA508;
import emulib.plugins.ISettingsHandler;
import emulib.plugins.memory.SimpleMemory;
import emulib.runtime.Context;
import emulib.runtime.StaticDialogs;

public class RAMmemory extends SimpleMemory {

    private RAMContext context;
    private MemoryWindow gui = null;

    public RAMmemory(Long pluginID) {
        super(pluginID);
        context = new RAMContext();
        if (!Context.getInstance().register(pluginID, context,
                C8E258161A30C508D5E8ED07CE943EEF7408CA508.class))
            StaticDialogs.showErrorMessage("Error: Could not register this memory");
    }

    @Override
    public String getTitle() {
        return "RAM Memory";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2012, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Operating memory for RAM machine. Contains program -"
                + "tape. User can not manually edit the tape - it is"
                + "a read only memory (filled once).";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);
        if (Context.getInstance().getCompilerContext(pluginID,
                C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.class) == null) {
            StaticDialogs.showErrorMessage("Error: Could not access to the compiler");
            return false;
        }
        return true;
    }

    @Override
    public int getProgramStart() {
        return 0;
    }

    @Override
    public int getSize() {
        return context.getSize();
    }

    @Override
    // Program start is always 0
    public void setProgramStart(int pos) {
    }

    @Override
    public void showSettings() {
        if (gui == null) {
            gui = new MemoryWindow(context);
        }
        gui.setVisible(true);
    }

    @Override
    public void destroy() {
        context.destroy();
        context = null;
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

}
