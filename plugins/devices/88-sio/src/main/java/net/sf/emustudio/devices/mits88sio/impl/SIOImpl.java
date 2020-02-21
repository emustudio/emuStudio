/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.devices.mits88sio.gui.ConfigDialog;
import net.sf.emustudio.devices.mits88sio.gui.StatusDialog;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
    type = PLUGIN_TYPE.DEVICE,
    title = "MITS 88-SIO Board",
    copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
    description = "Custom implementation of MITS 88-SIO serial board."
)
@SuppressWarnings("unused")
public class SIOImpl extends AbstractDevice implements SIOSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SIOImpl.class);

    private final Transmitter transmitter = new Transmitter();
    private final Port1 statusPort = new Port1(transmitter);
    private final Port2 dataPort = new Port2(transmitter);
    private final ContextPool contextPool;
    private final SIOSettings sioSettings;

    private StatusDialog gui;
    private CPUPorts cpuPorts;

    public SIOImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        sioSettings = new SIOSettings(pluginID);

        try {
            PhysicalPort physicalPort = new PhysicalPort(transmitter);
            contextPool.register(pluginID, physicalPort, DeviceContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            LOGGER.error("Could not register 88-SIO device context", e);
            StaticDialogs.showErrorMessage("Could not register MITS 88-SIO physical port", super.getTitle());
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
    @SuppressWarnings("unchecked")
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        ExtendedContext cpu = contextPool.getCPUContext(pluginID, ExtendedContext.class);

        cpuPorts = new CPUPorts(cpu);

        sioSettings.setSettingsManager(settings);
        sioSettings.addChangedObserver(this);

        // get a device attached to this board
        DeviceContext device = contextPool.getDeviceContext(pluginID, DeviceContext.class);
        if (device != null) {
            if (device.getDataType() != Short.class) {
                LOGGER.error("Could not connect device which does not operate on Short data!");
                throw new PluginInitializationException(this, "Incompatible input device!");
            }

            transmitter.setDevice(device);
        } else {
            LOGGER.warn("No device is connected into the 88-SIO.");
        }
        sioSettings.read();
    }

    @Override
    public void showGUI() {
        if (!sioSettings.isNoGUI()) {
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
        if (!sioSettings.isNoGUI()) {
            new ConfigDialog(sioSettings).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !sioSettings.isNoGUI();
    }

    @Override
    public void settingsChanged() {
        cpuPorts.reattachStatusPort(sioSettings.getStatusPorts(), statusPort);
        cpuPorts.reattachDataPort(sioSettings.getDataPorts(), dataPort);
    }
}
