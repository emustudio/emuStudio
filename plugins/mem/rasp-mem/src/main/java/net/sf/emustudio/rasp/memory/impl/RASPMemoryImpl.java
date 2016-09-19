/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.memory.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.memory.AbstractMemory;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.StaticDialogs;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import net.sf.emustudio.rasp.memory.RASPMemoryContext;
import net.sf.emustudio.rasp.memory.gui.MemoryWindow;

@PluginType(
        type = PLUGIN_TYPE.MEMORY,
        title = "RASP Memory",
        description = "RASP memory containing the program as well as the data",
        copyright = "\u00A9 Copyright 2016, Michal Šipoš"
)

/**
 * Class representing memory plugin for RASP.
 */
public class RASPMemoryImpl extends AbstractMemory {

    private final RASPMemoryContextImpl context;
    private final ContextPool contextPool;
    private MemoryWindow gui;

    /**
     * Constructor.
     *
     * @param pluginID ID of the plugin
     * @param contextPool the contextPool to register this plugin to
     */
    public RASPMemoryImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        this.context = new RASPMemoryContextImpl();

        try {
            contextPool.register(pluginID, context, RASPMemoryContext.class);
            contextPool.register(pluginID, context, MemoryContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException ex) {
            StaticDialogs.showErrorMessage("Could not register RASP Memory context",
                    RASPMemoryImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    /**
     * Get number of items in the memory.
     *
     * @return the number of items in the memory.
     */
    @Override
    public int getSize() {
        return context.getSize();
    }

    /**
     * Clears the memory and distroys the GUI windows.
     */
    @Override
    public void destroy() {
        context.destroy();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    /**
     * Shows memory window.
     */
    @Override
    public void showSettings() {
        if (gui == null) {
            gui = new MemoryWindow(context);
        }
        gui.setVisible(true);
    }

    /**
     * This plugin has GUI window implemented, so true is returned.
     *
     * @return always true
     */
    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.rasp.memory.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public int getProgramStart() {
        return context.getProgramStart();
    }

}
