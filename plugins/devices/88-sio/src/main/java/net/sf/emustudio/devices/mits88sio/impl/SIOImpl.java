/*
 * Created on Utorok, 2007, november 13, 17:01
 *
 * Copyright (C) 2007-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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
 *
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
import emulib.runtime.LoggerFactory;
import emulib.runtime.StaticDialogs;
import emulib.runtime.interfaces.Logger;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import net.sf.emustudio.devices.mits88sio.gui.ConfigDialog;
import net.sf.emustudio.devices.mits88sio.gui.StatusDialog;
import net.sf.emustudio.intel8080.ExtendedContext;

/**
 * This class represents the emulator of MITS 2SIO card.
 *
 * The card had two physical I/O ports which could be connected
 * to any serial I/O device that would connect to a current loop,
 * RS232, or TTY interface.  Available baud rates were jumper
 * selectable for each port from 110 to 9600.
 *
 * All I/O is via programmed I/O. Each each has a status port and a data port.
 *
 * From: http://www.altair32.com/Altair32specs.htm The standard I/O addresses assigned by MITS was 20Q-21Q for the first
 * port and 22Q-23Q for the second. The second port of the 2SIO is "connected" to a virtual line printer and the paper
 * tape reader/punch for support under CP/M.
 *
 */
@PluginType(type = PLUGIN_TYPE.DEVICE,
title = "MITS 88-SIO Board",
copyright = "\u00A9 Copyright 2007-2014, Peter Jakubčo",
description = "Custom implementation of MITS 88-SIO serial board.")
public class SIOImpl extends AbstractDevice implements SIOSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SIOImpl.class);

    private StatusCPUPort statusPort;
    private DataCPUPort dataPort;
    private PhysicalPort physicalPort;
    private ExtendedContext cpu;
    private StatusDialog gui;
    private SIOSettings sioSettings;

    private int currentStatusPortNumber = -1;
    private int currentDataPortNumber = -1;

    public SIOImpl(Long pluginID) {
        super(pluginID);
        statusPort = new StatusCPUPort(this);
        dataPort = new DataCPUPort(this);
        physicalPort = new PhysicalPort(dataPort);
        sioSettings = new SIOSettings(pluginID);

        try {
            ContextPool.getInstance().register(pluginID, physicalPort, DeviceContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            LOGGER.error("Could not register 88-SIO device context", e);
            StaticDialogs.showErrorMessage("Could not register MITS 88-SIO physical port context",
                    SIOImpl.class.getAnnotation(PluginType.class).title());
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

    /* Reset routine
     * TODO: automatization process messes the reset...
     */
    @Override
    public void reset() {
        if (dataPort.isEmpty()) {
            statusPort.setStatus((short)0x02);
        }
    }

    public void setStatus(short status) {
        statusPort.setStatus(status);
    }

    public short getStatus() {
        return statusPort.read();
    }


    /**
     * I/O instruction handlers, called from the CPU module when an IN or OUT
     * instruction is issued.
     * Each function is passed an 'io' flag, where 0 means a read from
     * the port, and 1 means a write to the port.  On input, the actual
     * input is passed as the return value, on output, 'data' is written
     * to the device.
     *
     * Don't have to care about detaching ports from CPU if initialization
     * fails. Main module won't start and make all detachments.
     */
    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        sioSettings.setSettingsManager(settings);
        sioSettings.addChangedObserver(this);

        try {
            cpu = (ExtendedContext) ContextPool.getInstance().getCPUContext(pluginID, ExtendedContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            throw new PluginInitializationException(
                    this, ": Could not get CPU", e
            );
        }

        // get a device connected into this card
        try {
            DeviceContext device = ContextPool.getInstance().getDeviceContext(pluginID, DeviceContext.class);
            if (device != null) {
                dataPort.attachDevice(device);
            } else {
                LOGGER.warning("No device is connected into the 88-SIO.");
            }
        } catch (ContextNotFoundException | InvalidContextException e) {
            throw new PluginInitializationException(
                    this, ": Could not get connected device", e
            );
        }

        if (cpu == null) {
            LOGGER.warning("88-SIO is not connected to the CPU.");
        }
        sioSettings.read();
    }

    @Override
    public void showGUI() {
        if (!sioSettings.isNoGUI()) {
            if (gui == null) {
                gui = new StatusDialog(currentStatusPortNumber,
                        currentDataPortNumber, dataPort.getAttachedDeviceID());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public void destroy() {
        cpu.detachDevice(currentStatusPortNumber);
        cpu.detachDevice(currentDataPortNumber);
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
        if (cpu == null) {
            return;
        }
        if (currentDataPortNumber != -1) {
            cpu.detachDevice(currentDataPortNumber);
        }
        if (currentStatusPortNumber != -1) {
            cpu.detachDevice(currentStatusPortNumber);
        }
        currentDataPortNumber = sioSettings.getDataPortNumber();
        currentStatusPortNumber = sioSettings.getStatusPortNumber();

        // attach IO ports
        if (cpu.attachDevice(statusPort, currentStatusPortNumber) == false) {
            currentStatusPortNumber = getAnotherPortNumber("Status port", SIOSettings.DEFAULT_STATUS_PORT_NUMBER);
            if (cpu.attachDevice(statusPort, currentStatusPortNumber) == false) {
                StaticDialogs.showErrorMessage("Error: status port still cannot be attached", getTitle());
            }
        }
        if (cpu.attachDevice(dataPort, currentDataPortNumber) == false) {
            currentDataPortNumber = getAnotherPortNumber("Data port", SIOSettings.DEFAULT_DATA_PORT_NUMBER);
            if (cpu.attachDevice(dataPort, currentDataPortNumber) == false) {
                StaticDialogs.showErrorMessage("Error: data port still cannot be attached", getTitle());
            }
        }
    }
}
