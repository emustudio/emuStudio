/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import javax.swing.JFrame;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "MITS 88-PIO")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice {
    private final Pio8255 pio = new Pio8255();
    private final boolean guiSupported;
    private Context8080 cpu;
    private PioGui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        try {
            for (int port = 0; port < 3; port++) {
                applicationApi.getContextPool().register(pluginID, pio.getChannel(port), DeviceContext.class);
            }
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            applicationApi.getDialogs().showError("Could not register 88-PIO port contexts", getTitle());
        }
    }

    @Override
    public void initialize() throws PluginInitializationException {
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        for (int port = Pio8255.PORT_A; port <= Pio8255.CONTROL_PORT; port++) {
            if (!cpu.attachDevice(port, pio)) {
                detachPorts();
                throw new PluginInitializationException(this,
                        String.format("88-PIO cannot attach to CPU port %02Xh", port));
            }
        }
    }

    @Override
    public void reset() {
        pio.reset();
    }

    @Override
    public void destroy() {
        detachPorts();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    private void detachPorts() {
        if (cpu != null) {
            for (int port = Pio8255.PORT_A; port <= Pio8255.CONTROL_PORT; port++) {
                cpu.detachDevice(port);
            }
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                gui = new PioGui(parent, pio);
            }
            gui.setVisible(true);
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
        return "MITS 88-PIO parallel interface with Intel 8255 mode-0 ports A, B, and C.";
    }
}
