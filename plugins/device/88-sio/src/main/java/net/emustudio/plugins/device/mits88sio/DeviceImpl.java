/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.*;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88sio.ports.*;
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
    title = "MITS 88-SIO serial board"
)
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractDevice implements SIOSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);

    private final Transmitter transmitter = new Transmitter();
    private final CpuStatusPort cpuStatusPort = new CpuStatusPort(transmitter);
    private final CpuDataPort cpuDataPort = new CpuDataPort(transmitter);
    private final SIOSettings sioSettings;

    private SioGui gui;
    private CpuPorts cpuPorts;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.sioSettings = new SIOSettings(settings);

        PhysicalPort physicalPort = new PhysicalPort(transmitter);
        try {
            applicationApi.getContextPool().register(pluginID, physicalPort, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register 88-SIO device context", e);
            applicationApi.getDialogs().showError(
                "Could not register MITS 88-SIO physical port. Please see log file for details.", super.getTitle()
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
        return "Custom implementation of MITS 88-SIO serial board.";
    }

    @Override
    public void reset() {
        transmitter.reset(sioSettings.isGuiNotSupported());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        Context8080 cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);

        cpuPorts = new CpuPorts(cpu);
        sioSettings.addChangedObserver(this);

        // get a device attached to this board
        try {
            DeviceContext<Byte> device = applicationApi.getContextPool().getDeviceContext(pluginID, DeviceContext.class);
            if (device.getDataType() != Byte.class) {
                throw new PluginInitializationException(
                    "Unexpected device data type. Expected Byte but was: " + device.getDataType()
                );
            }

            transmitter.setDevice(device);
        } catch (ContextNotFoundException e) {
            LOGGER.warn("No device is connected into the 88-SIO.");
        }
        sioSettings.read();
    }

    @Override
    public void showGUI(JFrame parent) {
        if (!sioSettings.isGuiNotSupported()) {
            if (gui == null) {
                gui = new SioGui(parent, transmitter.getDeviceId(), transmitter, cpuPorts);
            }
            gui.setVisible(true);
        }
    }

    @Override
    public void destroy() {
        cpuPorts.destroy();
        transmitter.setDevice(null);
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
        sioSettings.removeChangedObserver(this);
    }

    @Override
    public void showSettings(JFrame parent) {
        if (!sioSettings.isGuiNotSupported()) {
            new SettingsDialog(parent, sioSettings, applicationApi.getDialogs()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !sioSettings.isGuiNotSupported();
    }

    @Override
    public void settingsChanged() {
        try {
            cpuPorts.reattachStatusPort(sioSettings.getStatusPorts(), cpuStatusPort);
            cpuPorts.reattachDataPort(sioSettings.getDataPorts(), cpuDataPort);
        } catch (CouldNotAttachException e) {
            LOGGER.error(e.getMessage(), e);
            applicationApi.getDialogs().showError(e.getMessage(), "MITS 88-SIO");
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.mits88sio.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
