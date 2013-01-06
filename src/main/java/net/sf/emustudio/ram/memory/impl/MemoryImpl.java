/*
 * MemoryImpl.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
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
package net.sf.emustudio.ram.memory.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.memory.AbstractMemory;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;
import net.sf.emustudio.ram.memory.gui.MemoryWindow;

@PluginType(type=PLUGIN_TYPE.MEMORY,
        title="RAM Program Tape",
        copyright="\u00A9 Copyright 2009-2012, Peter Jakubčo",
        description="Read-only program tape for abstract RAM machine.")
public class MemoryImpl extends AbstractMemory {
    private RAMMemoryContextImpl context;
    private MemoryWindow gui = null;

    public MemoryImpl(Long pluginID) {
        super(pluginID);
        context = new RAMMemoryContextImpl();
        try {
            ContextPool.getInstance().register(pluginID, context, RAMMemoryContext.class);
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Could not register Program tape context", 
                    MemoryImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ram.memory.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);
        try {
            if (ContextPool.getInstance().getCompilerContext(pluginID,
                    RAMInstruction.class) == null) {
                StaticDialogs.showErrorMessage("Error: Could not access to the compiler");
                return false;
            }
            return true;
        } catch (InvalidContextException e) {
            StaticDialogs.showErrorMessage("Error: Could not access to the compiler",
                    MemoryImpl.class.getAnnotation(PluginType.class).title());
            return false;
        }
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
    public void setProgramStart(int pos) {
        // Program start is always 0
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
