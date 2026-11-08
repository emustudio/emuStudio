/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus;

import net.emustudio.emulib.plugins.AbstractPlugin;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.device.zxspectrum.bus.api.ModelProfile;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;

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
@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "ZX Spectrum Bus")
public class DeviceImpl extends AbstractPlugin implements Device {
    private final ZxSpectrumBusImpl bus;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.bus = new ZxSpectrumBusImpl(ModelProfile.valueOf(settings.getString("model").orElse(ModelProfile.DEFAULT.name())));

        ContextPool contextPool = applicationApi.getContextPool();
        contextPool.register(pluginID, bus, ZxSpectrumBus.class);
        contextPool.register(pluginID, bus, MemoryContext.class);
        contextPool.register(pluginID, bus, DeviceContext.class);
        contextPool.register(pluginID, bus, ContextZ80.class);
        contextPool.register(pluginID, bus, CPUContext.class);
        contextPool.register(pluginID, bus, Context8080.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        MemoryContext<Byte> memory = applicationApi.getContextPool().getMemory(pluginID, Byte.class);
        ContextZ80 cpu = applicationApi.getContextPool().get(pluginID, ContextZ80.class);
        this.bus.initialize(cpu, memory);
    }

    @Override
    public void reset() {
        bus.reset();
    }

    @Override
    public void destroy() {
        this.bus.destroy();
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public String getDescription() {
        return bus.getProfile().displayName + " Bus";
    }

}
