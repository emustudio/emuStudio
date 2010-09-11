/*
 * Mits88SIO.java
 *
 * Created on Utorok, 2007, november 13, 17:01
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package sio88;

import javax.swing.JOptionPane;

import interfaces.C17E8D62E685AD7E54C209C30482E3C00C8C56ECC;
import emuLib8.plugins.ISettingsHandler;
import emuLib8.plugins.device.IDeviceContext;
import emuLib8.plugins.device.SimpleDevice;
import emuLib8.runtime.Context;
import emuLib8.runtime.StaticDialogs;
import sio88.gui.ConfigDialog;
import sio88.gui.SIODialog;

/**
 * These functions support a simulated MITS 2SIO interface card.
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
public class Mits88SIO extends SimpleDevice {

    public static final int CPU_PORT1 = 0x10;
    public static final int CPU_PORT2 = 0x11;
    public short buffer;
    public short status;
    private CpuPort1 port1;
    private CpuPort2 port2;
    private int port1CPU;
    private int port2CPU;
    /**
     * Port where devices are connected
     */
    private PhysicalPort externPort;
    private C17E8D62E685AD7E54C209C30482E3C00C8C56ECC cpu = null;
    public SIODialog gui = null;

    public Mits88SIO(Long pluginID) {
        super(pluginID);
        port1 = new CpuPort1(this);
        port2 = new CpuPort2(this);
        port1CPU = CPU_PORT1;
        port2CPU = CPU_PORT2;
        externPort = new PhysicalPort(port2);

        if (!Context.getInstance().register(pluginID, externPort,
                IDeviceContext.class)) {
            StaticDialogs.showErrorMessage("Could not register the "
                    + "MITS 88-SIO device");
        }
    }

    @Override
    public String getDescription() {
        return "Recomended to use with MITS Altair8800 computer. This is"
                + " an implementation of MITS 88-SIO serial card. It has one"
                + " physical port and 2 programmable IO ports (default):"
                + " 0x10(status), 0x11(data). For programming see manual at\n"
                + "http://www.classiccmp.org/dunfield/s100c/mits/88sio_1.pdf";
    }

    @Override
    public String getVersion() {
        return "0.16b";
    }

    @Override
    public String getTitle() {
        return "MITS-88-SIO serial card";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2007-2010, P.Jakubčo";
    }

    /* Reset routine */
    @Override
    public void reset() {
        buffer = 0;    /* Data */
        status = 0x02; /* Status */
    }

    private void readSettings() {
        String s;
        s = settings.readSetting(pluginID, "port1");
        if (s != null) {
            try {
                port1CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port1CPU = CPU_PORT1;
            }
        }
        s = settings.readSetting(pluginID, "port2");
        if (s != null) {
            try {
                port2CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port2CPU = CPU_PORT2;
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
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);

        cpu = (C17E8D62E685AD7E54C209C30482E3C00C8C56ECC) Context.getInstance().getCPUContext(pluginID,
                C17E8D62E685AD7E54C209C30482E3C00C8C56ECC.class);

        // try to 'catch' any first connected device
        IDeviceContext device = Context.getInstance().getDeviceContext(pluginID,
                IDeviceContext.class);
        if (device != null) {
            port2.attachDevice(device);
        }

        readSettings();

        if (cpu == null) {
            return true;
        }

        // attach IO ports
        if (cpu.attachDevice(port1, port1CPU) == false) {
            String p;
            p = JOptionPane.showInputDialog(getTitle() + " (port1) can not be"
                    + " attached to default CPU port, please enter another one: ",
                    CPU_PORT1);
            try {
                port1CPU = Integer.decode(p);
            } catch (NumberFormatException e) {
                StaticDialogs.showErrorMessage("Error: wrong port number");
                return false;
            }
            if (cpu.attachDevice(port1, port1CPU) == false) {
                StaticDialogs.showErrorMessage("Error: " + getTitle()
                        + " (port1) still can't be attached");
                return false;
            }
        }
        if (cpu.attachDevice(port2, port2CPU) == false) {
            String p;
            p = JOptionPane.showInputDialog(getTitle() + " (port2) can not be"
                    + " attached to default CPU port, please enter another one: ",
                    CPU_PORT2);
            try {
                port2CPU = Integer.decode(p);
            } catch (NumberFormatException e) {
                StaticDialogs.showErrorMessage("Error: wrong port number");
                return false;
            }
            if (cpu.attachDevice(port2, port2CPU) == false) {
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
                    : port2.getAttachedDevice().getID();
            gui = new SIODialog(null, false, name, port1CPU, port2CPU);
        }
        gui.setVisible(true);
    }

    @Override
    public void destroy() {
        cpu.detachDevice(port1CPU);
        cpu.detachDevice(port2CPU);
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
