/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh;

import net.emustudio.emulib.plugin.AbstractPlugin;
import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

/**
 * SIMH emulator's pseudo device.
 */
@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "SIMH pseudo device",
        description = "Re-implementation of simh pseudo device, used in simh emulator. Version is SIMH004."
)
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractPlugin implements Device {
    private final PseudoContext context = new PseudoContext();
    private Context8080 cpu;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        ContextPool contextPool = applicationApi.getContextPool();

        cpu = contextPool.get(pluginID, Context8080.class);
        ByteMemoryContext mem = contextPool.get(pluginID, ByteMemoryContext.class);
        PaperTapeContext paperTape = contextPool.get(pluginID, PaperTapeContext.class, 0);

        context.setMemory(mem);
        context.setCpu(cpu);
        context.setPaperTape(paperTape);

        // attach IO port
        if (!cpu.attachDevice(0xFE, context)) {
            throw new PluginInitializationException(
                    "SIMH device cannot be attached to CPU (maybe there is a hardware conflict?)"
            );
        }
        reset();
    }

    @Override
    public void destroy() {
        if (cpu != null) {
            cpu.detachDevice(0xFE);
            cpu = null;
        }
    }

    @Override
    public void reset() {
        context.reset();
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }
}
