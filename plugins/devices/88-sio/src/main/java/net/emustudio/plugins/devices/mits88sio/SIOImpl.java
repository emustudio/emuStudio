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
package net.emustudio.plugins.devices.mits88sio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.*;
import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import net.emustudio.plugins.devices.mits88sio.gui.ConfigDialog;
import net.emustudio.plugins.devices.mits88sio.gui.StatusDialog;
import net.emustudio.plugins.devices.mits88sio.ports.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.DEVICE,
    title = "MITS 88-SIO serial board"
)
@SuppressWarnings("unused")
public class SIOImpl extends AbstractDevice implements SIOSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SIOImpl.class);

    private final Transmitter transmitter = new Transmitter();
    private final StatusPort statusPort = new StatusPort(transmitter);
    private final DataPort dataPort = new DataPort(transmitter);
    private final SIOSettings sioSettings;

    private StatusDialog gui;
    private CPUPorts cpuPorts;

    public SIOImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
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
        transmitter.reset();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        ExtendedContext cpu = applicationApi.getContextPool().getCPUContext(pluginID, ExtendedContext.class);

        cpuPorts = new CPUPorts(cpu);
        sioSettings.addChangedObserver(this);

        // get a device attached to this board
        try {
            DeviceContext<Short> device = applicationApi.getContextPool().getDeviceContext(pluginID, DeviceContext.class);
            if (device.getDataType() != Short.class) {
                throw new PluginInitializationException(
                    "Unexpected device data type. Expected Short but was: " + device.getDataType()
                );
            }

            transmitter.setDevice(device);
        } catch (ContextNotFoundException e) {
            LOGGER.warn("No device is connected into the 88-SIO.");
        }
        sioSettings.read();
    }

    @Override
    public void showGUI() {
        if (!sioSettings.isGuiNotSupported()) {
            if (gui == null) {
                gui = new StatusDialog(transmitter.getDeviceId(), transmitter, cpuPorts);
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
        }
        gui = null;
        sioSettings.removeChangedObserver(this);
    }

    @Override
    public void showSettings() {
        if (!sioSettings.isGuiNotSupported()) {
            new ConfigDialog(sioSettings, applicationApi.getDialogs()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !sioSettings.isGuiNotSupported();
    }

    @Override
    public void settingsChanged() {
        try {
            cpuPorts.reattachStatusPort(sioSettings.getStatusPorts(), statusPort);
            cpuPorts.reattachDataPort(sioSettings.getDataPorts(), dataPort);
        } catch (CouldNotAttachException e) {
            LOGGER.error(e.getMessage(), e);
            applicationApi.getDialogs().showError(e.getMessage(), "MITS 88-SIO");
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.devices.mits88sio.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
