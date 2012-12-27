/*
 * Mits88SIO.java
 *
 * Created on Utorok, 2007, november 13, 17:01
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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

import emulib.annotations.ContextType;
import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import java.util.ArrayList;
import java.util.List;
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
 * All I/O is via programmed I/O.  Each each has a status port
 * and a data port.
 *
 * From: http://www.altair32.com/Altair32specs.htm
 * The standard I/O addresses assigned by MITS was 20Q-21Q  for the first port
 * and 22Q-23Q for the second. The second port of the 2SIO is "connected" to a
 * virtual line printer and the paper tape reader/punch for support under CP/M.
 *
 * @author vbmacher
 */
@PluginType(type = PLUGIN_TYPE.DEVICE,
title = "MITS 88-SIO Board",
copyright = "\u00A9 Copyright 2007-2012, Peter Jakubčo",
description = "Custom implementation of MITS 88-SIO serial card.")
public class Mits88SIO extends AbstractDevice {

    public static final int CPU_PORT1 = 0x10;
    public static final int CPU_PORT2 = 0x11;
    public List<Short> buffer = new ArrayList<Short>();
    public short status;
    private StatusPort port1;
    private DataPort port2;
    private int port1Number;
    private int port2Number;
    /**
     * Port where devices are connected
     */
    private PhysicalPort externPort;
    private ExtendedContext cpu = null;
    public SIODialog gui = null;

    public Mits88SIO(Long pluginID) {
        super(pluginID);
        port1 = new StatusPort(this);
        port2 = new DataPort(this);
        port1Number = CPU_PORT1;
        port2Number = CPU_PORT2;
        externPort = new PhysicalPort(port2);

        try {
            ContextPool.getInstance().register(pluginID, externPort, DeviceContext.class);
        } catch (RuntimeException e) {
            StaticDialogs.showErrorMessage("Could not register MITS 88-SIO context",
                    Mits88SIO.class.getAnnotation(PluginType.class).title());
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
      //  buffer.clear(); // = 0;    /* Data */
      //  status = 0x02; /* Status */
        if (buffer.isEmpty()) {
            status = 0x02;
        }
    }

    private void readSettings() {
        String s;
        s = settings.readSetting(pluginID, "port1");
        if (s != null) {
            try {
                port1Number = Integer.decode(s);
            } catch (NumberFormatException e) {
                port1Number = CPU_PORT1;
            }
        }
        s = settings.readSetting(pluginID, "port2");
        if (s != null) {
            try {
                port2Number = Integer.decode(s);
            } catch (NumberFormatException e) {
                port2Number = CPU_PORT2;
            }
        }
    }
    
    private String getTitle() {
        return Mits88SIO.class.getAnnotation(PluginType.class).title();
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

        // try to 'catch' any first connected device
        DeviceContext device = ContextPool.getInstance().getDeviceContext(pluginID,
                DeviceContext.class);
        if (device != null) {
            port2.attachDevice(device);
        }

        readSettings();

        cpu = (ExtendedContext) ContextPool.getInstance().getCPUContext(pluginID,
                ExtendedContext.class);
        if (cpu == null) {
            return true;
        }

        // attach IO ports
        if (cpu.attachDevice(port1, port1Number) == false) {
            String p;
            p = JOptionPane.showInputDialog(getTitle() + " (port1) can not be"
                    + " attached to default CPU port, please enter another one: ",
                    CPU_PORT1);
            try {
                port1Number = Integer.decode(p);
            } catch (NumberFormatException e) {
                StaticDialogs.showErrorMessage("Error: wrong port number");
                return false;
            }
            if (cpu.attachDevice(port1, port1Number) == false) {
                StaticDialogs.showErrorMessage("Error: " + getTitle()
                        + " (port1) still can't be attached");
                return false;
            }
        }
        if (cpu.attachDevice(port2, port2Number) == false) {
            String p;
            p = JOptionPane.showInputDialog(getTitle() + " (port2) can not be"
                    + " attached to default CPU port, please enter another one: ",
                    CPU_PORT2);
            try {
                port2Number = Integer.decode(p);
            } catch (NumberFormatException e) {
                StaticDialogs.showErrorMessage("Error: wrong port number");
                return false;
            }
            if (cpu.attachDevice(port2, port2Number) == false) {
                StaticDialogs.showErrorMessage("Error: " + getTitle()
                        + " (port2) still can't be attached");
                return false;
            }
        }
        return true;
    }

    @Override
    public void showGUI() {
        if (gui == null) {
            String name = (port2.getAttachedDevice() == null) ? "none"
                    : port2.getAttachedDevice().getClass().getAnnotation(ContextType.class).id();
            gui = new SIODialog(null, false, name, port1Number, port2Number);
        }
        gui.setVisible(true);
    }

    @Override
    public void destroy() {
        cpu.detachDevice(port1Number);
        cpu.detachDevice(port2Number);
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
