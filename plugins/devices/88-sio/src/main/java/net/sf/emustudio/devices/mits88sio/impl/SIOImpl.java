/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.mits88sio.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.devices.mits88sio.gui.ConfigDialog;
import net.sf.emustudio.devices.mits88sio.gui.StatusDialog;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.DEVICE,
        title = "MITS 88-SIO Board",
        copyright = "\u00A9 Copyright 2006-2016, Peter Jakubčo",
        description = "Custom implementation of MITS 88-SIO serial board."
)
@SuppressWarnings("unused")
public class SIOImpl extends AbstractDevice implements SIOSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SIOImpl.class);

    private final Transmitter transmitter = new Transmitter();
    private final Port1 statusPort = new Port1(transmitter);
    private final Port2 dataPort = new Port2(transmitter);
    private final PhysicalPort physicalPort = new PhysicalPort(transmitter);
    private final ContextPool contextPool;
    private final SIOSettings sioSettings;

    private ExtendedContext cpu;
    private StatusDialog gui;

    private int statusPortNumber = SIOSettings.DEFAULT_STATUS_PORT_NUMBER;
    private int dataPortNumber = SIOSettings.DEFAULT_DATA_PORT_NUMBER;

    public SIOImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        sioSettings = new SIOSettings(pluginID);

        try {
            contextPool.register(pluginID, physicalPort, DeviceContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            LOGGER.error("Could not register 88-SIO device context", e);
            StaticDialogs.showErrorMessage("Could not register MITS 88-SIO physical port", getTitle());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.devices.mits88sio.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            LOGGER.error("Cannot get version number", e);
            return "(unknown)";
        }
    }

    @Override
    public void reset() {
        transmitter.reset();
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        sioSettings.setSettingsManager(settings);
        sioSettings.addChangedObserver(this);

        try {
            cpu = (ExtendedContext) contextPool.getCPUContext(pluginID, ExtendedContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            throw new PluginInitializationException(this, ": Could not get CPU", e);
        }

        // get a device attached to this board
        try {
            DeviceContext device = contextPool.getDeviceContext(pluginID, DeviceContext.class);
            if (device != null) {
                transmitter.setDevice(device);
            } else {
                LOGGER.warn("No device is connected into the 88-SIO.");
            }
        } catch (ContextNotFoundException | InvalidContextException e) {
            throw new PluginInitializationException(this, ": Could not get connected device", e);
        }
        sioSettings.read();
    }

    @Override
    public void showGUI() {
        if (!sioSettings.isNoGUI()) {
            if (gui == null) {
                gui = new StatusDialog(statusPortNumber, dataPortNumber, transmitter.getDeviceId());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public void destroy() {
        cpu.detachDevice(statusPortNumber);
        cpu.detachDevice(dataPortNumber);
        transmitter.setDevice(null);
        if (gui != null) {
            gui.dispose();
        }
        gui = null;
        sioSettings.removeChangedObserver(this);
    }

    @Override
    public void showSettings() {
        if (!sioSettings.isNoGUI()) {
            new ConfigDialog(sioSettings).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !sioSettings.isNoGUI();
    }

    private int getAnotherPortNumber(String portName, int defaultPortNumber) {
        String newPort = (String) JOptionPane.showInputDialog(null, portName
                + " cannot be attached to CPU, please enter another one: ",
                getTitle(), JOptionPane.ERROR_MESSAGE, null, null, defaultPortNumber);
        try {
            return Integer.decode(newPort);
        } catch (NumberFormatException e) {
            LOGGER.error("Could not convert " + portName + " number to integer", e);
            StaticDialogs.showErrorMessage("Invalid port number", getTitle());
        }
        return defaultPortNumber;
    }

    @Override
    public void settingsChanged() {
        if (dataPortNumber != -1) {
            cpu.detachDevice(dataPortNumber);
        }
        if (statusPortNumber != -1) {
            cpu.detachDevice(statusPortNumber);
        }
        dataPortNumber = sioSettings.getDataPortNumber();
        statusPortNumber = sioSettings.getStatusPortNumber();

        // attach IO ports
        if (!cpu.attachDevice(statusPort, statusPortNumber)) {
            LOGGER.warn("Could not attach Status port to {}. Trying another one.", statusPortNumber);
            statusPortNumber = getAnotherPortNumber("Status port", SIOSettings.DEFAULT_STATUS_PORT_NUMBER);
            if (!cpu.attachDevice(statusPort, statusPortNumber)) {
                LOGGER.error("Could not attach Status port to {}.", statusPortNumber);
                StaticDialogs.showErrorMessage("Error: status port still cannot be attached", getTitle());
            }
        }
        if (!cpu.attachDevice(dataPort, dataPortNumber)) {
            LOGGER.warn("Could not attach Data port to {}. Trying another one.", dataPortNumber);
            dataPortNumber = getAnotherPortNumber("Data port", SIOSettings.DEFAULT_DATA_PORT_NUMBER);
            if (!cpu.attachDevice(dataPort, dataPortNumber)) {
                LOGGER.error("Could not attach Data port to {}.", dataPortNumber);
                StaticDialogs.showErrorMessage("Error: data port still cannot be attached", getTitle());
            }
        }
    }
}
