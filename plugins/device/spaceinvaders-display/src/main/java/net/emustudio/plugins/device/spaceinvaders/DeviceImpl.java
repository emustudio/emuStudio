/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import javax.swing.JFrame;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Space Invaders display")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice {
    private final SpaceInvadersHardware hardware = new SpaceInvadersHardware();
    private final boolean guiSupported;
    private final int scale;
    private final boolean colorOverlay;
    private Context8080 cpu;
    private MemoryContext<Byte> memory;
    private FrameClock frameClock;
    private volatile DisplayWindow window;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        scale = settings.getInt("scale", 2);
        colorOverlay = settings.getBoolean("colorOverlay", true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        if (memory.getCellTypeClass() != Byte.class || memory.getSize() <= 0x3FFF) {
            throw new PluginInitializationException(this, "Space Invaders requires byte memory through address 3FFFh");
        }
        int attached = 0;
        for (int port = SpaceInvadersHardware.INPUT_1_PORT; port <= SpaceInvadersHardware.SHIFT_DATA_PORT; port++) {
            if (!cpu.attachDevice(port, hardware)) {
                for (int cleanup = SpaceInvadersHardware.INPUT_1_PORT; cleanup < port; cleanup++) {
                    cpu.detachDevice(cleanup);
                }
                throw new PluginInitializationException(this,
                        String.format("Space Invaders cannot attach to CPU port %02Xh", port));
            }
            attached++;
        }
        if (attached == 4) {
            frameClock = new FrameClock(cpu, this::frameReady);
            frameClock.start();
        }
    }

    private void frameReady() {
        DisplayWindow current = window;
        if (current != null) {
            current.frameReady();
        }
    }

    @Override
    public void reset() {
        hardware.reset();
    }

    @Override
    public void destroy() {
        if (frameClock != null) {
            frameClock.close();
            frameClock = null;
        }
        if (cpu != null) {
            for (int port = SpaceInvadersHardware.INPUT_1_PORT; port <= SpaceInvadersHardware.SHIFT_DATA_PORT; port++) {
                cpu.detachDevice(port);
            }
        }
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (window == null) {
                window = new DisplayWindow(parent, memory, hardware, scale, colorOverlay);
            }
            window.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public void showSettings(JFrame parent) {
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
    public String getDescription() {
        return "Space Invaders framebuffer, controls, shift register, and raster interrupts.";
    }
}
