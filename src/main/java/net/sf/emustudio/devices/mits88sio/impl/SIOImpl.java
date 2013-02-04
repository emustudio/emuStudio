/*
 * Mits88SIO.java
 *
 * Created on Utorok, 2007, november 13, 17:01
 *
 * Copyright (C) 2007-2013 Peter Jakubčo
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
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.LoggerFactory;
import emulib.runtime.StaticDialogs;
import emulib.runtime.interfaces.Logger;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import net.sf.emustudio.devices.mits88sio.gui.ConfigDialog;
import net.sf.emustudio.devices.mits88sio.gui.SIODialog;
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
 * @author Peter Jakubčo
 */
@PluginType(type = PLUGIN_TYPE.DEVICE,
title = "MITS 88-SIO Board",
copyright = "\u00A9 Copyright 2007-2013, Peter Jakubčo",
description = "Custom implementation of MITS 88-SIO serial card.")
public class SIOImpl extends AbstractDevice {

    public static final int CPU_PORT1 = 0x10;
    public static final int CPU_PORT2 = 0x11;
    private static final Logger LOGGER = LoggerFactory.getLogger(SIOImpl.class);
    
    private StatusCPUPort statusPort;
    private DataCPUPort dataPort;
    private PhysicalPort physicalPort;
    private int statusPortNumber;
    private int dataPortNumber;
    private ExtendedContext cpu;
    private SIODialog gui;

    public SIOImpl(Long pluginID) {
        super(pluginID);
        statusPort = new StatusCPUPort(this);
        dataPort = new DataCPUPort(this);
        physicalPort = new PhysicalPort(dataPort);
        statusPortNumber = CPU_PORT1;
        dataPortNumber = CPU_PORT2;

        try {
            ContextPool.getInstance().register(pluginID, physicalPort, DeviceContext.class);
        } catch (Exception e) {
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

    private void readSettings() {
        String s;
        s = settings.readSetting(pluginID, "port1");
        if (s != null) {
            try {
                statusPortNumber = Integer.decode(s);
            } catch (NumberFormatException e) {
                statusPortNumber = CPU_PORT1;
            }
        }
        s = settings.readSetting(pluginID, "port2");
        if (s != null) {
            try {
                dataPortNumber = Integer.decode(s);
            } catch (NumberFormatException e) {
                dataPortNumber = CPU_PORT2;
            }
        }
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
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);
        readSettings();

        try {
            cpu = (ExtendedContext) ContextPool.getInstance().getCPUContext(pluginID, ExtendedContext.class);
        } catch (InvalidContextException e) {
            StaticDialogs.showErrorMessage("Warning: Could not connect to CPU", getTitle());
        }
        
        // get a device connected into this card
        try {
            DeviceContext device = ContextPool.getInstance().getDeviceContext(pluginID, DeviceContext.class);
            if (device != null) {
                dataPort.attachDevice(device);
            } else {
                LOGGER.warning("No device is connected into the MITS SIO.");
            }
        } catch (InvalidContextException e) {
            StaticDialogs.showErrorMessage("Warning: Could not get connected device", getTitle());
        }

        if (cpu == null) {
            LOGGER.warning("MITS SIO is not connected to the CPU.");
            return true;
        }

        // attach IO ports
        if (cpu.attachDevice(statusPort, statusPortNumber) == false) {
            String newPort = (String)JOptionPane.showInputDialog(null,
                    "Status port can not be attached to default CPU port, please enter another one: ", getTitle(),
                    JOptionPane.ERROR_MESSAGE, null, null, CPU_PORT1);
            try {
                statusPortNumber = Integer.decode(newPort);
            } catch (NumberFormatException e) {
                StaticDialogs.showErrorMessage("Error: wrong port number", getTitle());
                return false;
            }
            if (cpu.attachDevice(statusPort, statusPortNumber) == false) {
                StaticDialogs.showErrorMessage("Error: status port still cannot be attached", getTitle());
                return false;
            }
        }
        if (cpu.attachDevice(dataPort, dataPortNumber) == false) {
            String newPort = (String)JOptionPane.showInputDialog(null,
                    "Data port can not be attached to default CPU port, please enter another one: ", getTitle(),
                    JOptionPane.ERROR_MESSAGE, null, null, CPU_PORT2);

            try {
                dataPortNumber = Integer.decode(newPort);
            } catch (NumberFormatException e) {
                StaticDialogs.showErrorMessage("Error: wrong port number", getTitle());
                return false;
            }
            if (cpu.attachDevice(dataPort, dataPortNumber) == false) {
                StaticDialogs.showErrorMessage("Error: data port still cannot be attached", getTitle());
                return false;
            }
        }
        return true;
    }

    @Override
    public void showGUI() {
        if (gui == null) {
            gui = new SIODialog(statusPortNumber, dataPortNumber, dataPort.getAttachedDeviceID());
        }
        gui.setVisible(true);
    }

    @Override
    public void destroy() {
        cpu.detachDevice(statusPortNumber);
        cpu.detachDevice(dataPortNumber);
        if (gui != null) {
            gui.dispose();
        }
        gui = null;
    }

    @Override
    public void showSettings() {
        new ConfigDialog(pluginID, settings).setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
