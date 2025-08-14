/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88sio.gui.SettingsDialog;
import net.emustudio.plugins.device.mits88sio.gui.SioGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "MITS 88-SIO"
)
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractDevice {
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
        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        try {
            applicationApi.getContextPool().register(pluginID, deviceChannel, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register 88-SIO device context", e);
            applicationApi.getDialogs().showError(
                    "Could not register 88-SIO device channel. Please see log file for details.", super.getTitle()
            );
        }
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
        return "MITS 88-SIO serial board; capable of connecting single device. Supports attaching to multiple CPU ports.";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public void reset() {
        sioUnit.reset(guiSupported);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        Context8080 cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        this.sioUnit = new SioUnit(sioSettings, cpu);
        this.uart = sioUnit.getUART();
        deviceChannel.setUART(uart);

        // get a device attached to this board
        try {
            DeviceContext<Byte> device = applicationApi.getContextPool().getDeviceContext(pluginID, DeviceContext.class);
            if (device.getDataType() != Byte.class) {
                throw new PluginInitializationException(
                        "Unexpected device data type. Expected Byte but was: " + device.getDataType()
                );
            }
            uart.setDevice(device);
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
                gui = new SioGui(parent, uart);
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
            new SettingsDialog(parent, sioSettings, applicationApi.getDialogs()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.mits88sio.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
