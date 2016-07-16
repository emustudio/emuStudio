/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.devices.simh.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import net.sf.emustudio.memory.standard.StandardMemoryContext;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * SIMH emulator's pseudo device.
 */
@PluginType(
        type=PLUGIN_TYPE.DEVICE,
        title="SIMH pseudo device",
        copyright="Copyright (c) 2002-2007, Peter Schorn\n\u00A9 Copyright 2006-2016, Peter Jakubčo",
        description="Re-implementation of simh pseudo device, used in simh emulator. Version is SIMH003."
)
@SuppressWarnings("unused")
public class SIMHpseudo extends AbstractDevice {
    private PseudoContext context;
    private final ContextPool contextPool;

    public SIMHpseudo(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        context = new PseudoContext();
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        ExtendedContext cpu = contextPool.getCPUContext(pluginID, ExtendedContext.class);
        StandardMemoryContext mem = contextPool.getMemoryContext(pluginID, StandardMemoryContext.class);

        context.setMemory(mem);

        // attach IO port
        if (!cpu.attachDevice(context, 0xFE)) {
            throw new PluginInitializationException(
                    this, "SIMH device can't be attached to CPU (maybe there is a hardware conflict)"
            );
        }
        reset();
    }

    @Override
    public void showGUI() {
        StaticDialogs.showMessage("GUI not supported");
    }

    @Override
    public void reset() {
        context.reset();
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.devices.simh.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void destroy() {
        this.context = null;
    }

    @Override
    public void showSettings() {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
