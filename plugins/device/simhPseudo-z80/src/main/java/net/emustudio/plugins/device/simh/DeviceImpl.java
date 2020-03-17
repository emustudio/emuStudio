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
package net.emustudio.plugins.device.simh;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import net.emustudio.plugins.memory.standard.api.StandardMemoryContext;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * SIMH emulator's pseudo device.
 */
@PluginRoot(
    type = PLUGIN_TYPE.DEVICE,
    title = "SIMH pseudo device"
)
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractDevice {
    private final PseudoContext context = new PseudoContext();

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        ContextPool contextPool = applicationApi.getContextPool();

        ExtendedContext cpu = contextPool.getCPUContext(pluginID, ExtendedContext.class);
        StandardMemoryContext mem = contextPool.getMemoryContext(pluginID, StandardMemoryContext.class);

        context.setMemory(mem);

        // attach IO port
        if (!cpu.attachDevice(context, 0xFE)) {
            throw new PluginInitializationException(
                this, "SIMH device cannot be attached to CPU (maybe there is a hardware conflict?)"
            );
        }
        reset();
    }

    @Override
    public void destroy() {

    }

    @Override
    public void showGUI() {
        applicationApi.getDialogs().showInfo("GUI is not supported");
    }

    @Override
    public void reset() {
        context.reset();
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
        return "Re-implementation of simh pseudo device, used in simh emulator. Version is SIMH003.";
    }

    @Override
    public void showSettings() {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.simh.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
