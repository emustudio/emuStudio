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
package net.emustudio.plugins.device.zxspectrum.bus;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * ZX Spectrum Bus
 * <p>
 * - controls access to memory
 * - provides access to cycle counter (TimeEventProcessor)
 * - provides memory and I/O contention
 * <p>
 * Example usage:
 * a cassette player plays a tape = puts data on the bus in given time intervals. Z80 CPU reads the data from the bus
 * when convenient.
 */
@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "ZX Spectrum48K Bus")
public class DeviceImpl extends AbstractDevice {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);
    private final ZxSpectrumBusImpl bus = new ZxSpectrumBusImpl();

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        try {
            ContextPool contextPool = applicationApi.getContextPool();
            contextPool.register(pluginID, bus, ZxSpectrumBus.class);
            contextPool.register(pluginID, bus, MemoryContext.class);
            contextPool.register(pluginID, bus, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register zx-spectrum bus context", e);
            applicationApi.getDialogs().showError(
                    "Could not register zx-spectrum bus. Please see log file for more details", getTitle()
            );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        MemoryContext<Byte> memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        Class<?> cellTypeClass = memory.getCellTypeClass();
        if (cellTypeClass != Byte.class) {
            throw new PluginInitializationException("Could not find Byte-cell memory");
        }
        ContextZ80 cpu = applicationApi.getContextPool().getCPUContext(pluginID, ContextZ80.class);
        this.bus.initialize(cpu, memory);
    }

    @Override
    public void showGUI(JFrame parent) {

    }

    @Override
    public boolean isGuiSupported() {
        return false;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void showSettings(JFrame jFrame) {

    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
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
        return "ZX Spectrum48K Bus";
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.zxspectrum.bus.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
