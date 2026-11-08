/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.AbstractPlugin;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88sio.gui.SettingsDialog;
import net.emustudio.plugins.device.mits88sio.gui.SioGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "MITS 88-SIO",
        description = "MITS 88-SIO serial board; capable of connecting single device. Supports attaching to multiple CPU ports."
)
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractPlugin implements Device {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);

    private final SioUnitSettings sioSettings;
    private final UART.DeviceChannel deviceChannel;
    private final boolean guiSupported;
    private SioUnit sioUnit;
    private UART uart;
    private SioGui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.sioSettings = new SioUnitSettings(settings);
        this.deviceChannel = new UART.DeviceChannel();
        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
        applicationApi.getContextPool().register(pluginID, deviceChannel, DeviceContext.class);
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public void reset() {
        sioUnit.reset(guiSupported);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        Context8080 cpu = applicationApi.getContextPool().get(pluginID, Context8080.class);
        this.sioUnit = new SioUnit(sioSettings, cpu);
        this.uart = sioUnit.getUART();
        deviceChannel.setUART(uart);

        // get a device attached to this board
        try {
            uart.setDevice(applicationApi.getContextPool().getDevice(pluginID, Byte.class));
        } catch (ContextNotFoundException e) {
            LOGGER.warn("No device is connected into the 88-SIO.");
        }
        sioUnit.attach();
        sioSettings.addObserver(() -> sioUnit.attach());
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                gui = new SioGui(parent, uart, applicationApi.getGUI());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public void destroy() {
        uart.setDevice(null);
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
        sioSettings.clearObservers();
    }

    @Override
    public void showSettings(JFrame parent) {
        if (guiSupported) {
            new SettingsDialog(parent, sioSettings, applicationApi.getDialogs(), applicationApi.getGUI()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }

}
