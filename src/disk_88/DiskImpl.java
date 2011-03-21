/*
 * DiskImpl.java
 *
 * Created on Streda, 30 january 2008
 * 
 * KEEP IT SIMPLE STUPID
 * sometimes just... YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package disk_88;

import disk_88.gui.ConfigDialog;
import disk_88.gui.DiskFrame;
import interfaces.C738039DCA561A49F377859B108A9AD1EE6CBDACB;

import java.io.IOException;
import java.util.ArrayList;

import emuLib8.plugins.ISettingsHandler;
import emuLib8.plugins.device.IDeviceContext;
import emuLib8.plugins.device.SimpleDevice;
import emuLib8.runtime.Context;
import emuLib8.runtime.StaticDialogs;

/**
 * MITS 88-DISK Floppy Disk controller with up to eight drives (although I
 * think that the interface can actually support up to 16 drives). The connected
 * floppy drives were Pertec FD-400 8" hard-sectored floppy drives. Each
 * single-sided diskette has 77 tracks of 32, 137-byte sectors each (an
 * unformatted capacity of 337,568 bytes). The controller supported neither
 * interrupts nor DMA, so floppy access required the sustained attention of
 * the CPU. The standard I/O addresses were 10Q-12Q.
 *
 *    The controller is interfaced to the CPU by use of 3 I/O addreses,
 *   standardly, these are device numbers 10, 11, and 12 (octal).
 *
 *  Address     Mode    Function
 *  -------             ----    --------
 *
 *      10              Out             Selects and enables Controller and Drive
 *      10              In              Indicates status of Drive and Controller
 *      11              Out             Controls Disk Function
 *      11              In              Indicates current sector position of disk
 *      12              Out             Write data
 *      12              In              Read data*
 *
 *  Drive Select Out (Device 10 OUT):
 *
 *  +---+---+---+---+---+---+---+---+
 *  | C | X | X | X |   Device      |
 *  +---+---+---+---+---+---+---+---+
 *
 *  C = If this bit is 1, the disk controller selected by 'device' is
 *      cleared. If the bit is zero, 'device' is selected as the
 *      device being controlled by subsequent I/O operations.
 *  X = not used
 *  Device = value zero thru 15, selects drive to be controlled.
 *
 *  Drive Status In (Device 10 IN):
 *
 *    7   6   5   4   3   2   1   0
 *  +---+---+---+---+---+---+---+---+
 *  | R | Z | I | X | X | H | M | W |
 *  +---+---+---+---+---+---+---+---+
 *
 *  W - When 0, write circuit ready to write another byte.
 *  M - When 0, head movement is allowed
 *  H - When 0, indicates head is loaded for read/write
 *  X - not used (will be 0)
 *  I - When 0, indicates interrupts enabled (not used this emulator)
 *  Z - When 0, indicates head is on track 0
 *  R - When 0, indicates that read circuit has new byte to read
 *
 *  Drive Control (Device 11 OUT):
 *
 *  +---+---+---+---+---+---+---+---+
 *  | W | C | D | E | U | H | O | I |
 *  +---+---+---+---+---+---+---+---+
 *
 *  I - When 1, steps head IN one track
 *  O - When 1, steps head OUT out track
 *  H - When 1, loads head to drive surface
 *  U - When 1, unloads head
 *  E - Enables interrupts (ignored this simulator)
 *  D - Disables interrupts (ignored this simulator)
 *  C - When 1 lowers head current (ignored this simulator)
 *  W - When 1, starts Write Enable sequence:   W bit on device 10
 *      (see above) will go 1 and data will be read from port 12
 *      until 137 bytes have been read by the controller from
 *      that port.  The W bit will go off then, and the sector data
 *      will be written to disk. Before you do this, you must have
 *      stepped the track to the desired number, and waited until
 *      the right sector number is presented on device 11 IN, then
 *      set this bit.
 *
 *  Sector Position (Device 11 IN):
 *
 *  As the sectors pass by the read head, they are counted and the
 *  number of the current one is available in this register.
 *
 *  +---+---+---+---+---+---+---+---+
 *  | X | X |  Sector Number    | T |
 *  +---+---+---+---+---+---+---+---+
 *
 *  X = Not used
 *  Sector number = binary of the sector number currently under the
 *      head, 0-31.
 *  T = Sector True, is a 1 when the sector is positioned to read or
 *      write.
 *
 * @author vbmacher
 */
public class DiskImpl extends SimpleDevice {

    private final static int DRIVES_COUNT = 16;
    public final static int CPU_PORT1 = 0x8;
    public final static int CPU_PORT2 = 0x9;
    public final static int CPU_PORT3 = 0xA;
    private int port1CPU;
    private int port2CPU;
    private int port3CPU;
    private C738039DCA561A49F377859B108A9AD1EE6CBDACB cpu;
    public ArrayList<Drive> drives;
    private Port1 port1;
    private Port2 port2;
    private Port3 port3;
    public int current_drive;
    private DiskFrame gui;
    private boolean noGUI = false;

    public DiskImpl(Long pluginID) {
        super(pluginID);
        this.drives = new ArrayList<Drive>();
        for (int i = 0; i < DRIVES_COUNT; i++) {
            this.drives.add(new Drive());
        }

        this.current_drive = 0xFF;
        port1CPU = CPU_PORT1;
        port2CPU = CPU_PORT2;
        port3CPU = CPU_PORT3;

        port1 = new Port1(this);
        port2 = new Port2(this);
        port3 = new Port3(this);
    }

    /**
     * Asks the user for new port number and tries to attach given port to this
     * port number on CPU.
     *
     * If a port of the DISK cannot be attached to the CPU, we want to ask
     * the user to provide another port number. He got only one chance.
     *
     * @param DISKport      Port number in 88-DISK (1,2,3)
     * @param defaultPort   Default port number on CPU
     * @param port          The 88-DISK port object that needs to be attached
     * @return new port number if the attachement was successful, -1 otherwise
     */
    private int providePort(int DISKport, int defaultPort, IDeviceContext port) {
        String providedPort = StaticDialogs.inputStringValue("Port " +
                String.valueOf(DISKport) + " can not be attached to default" +
                " CPU port, please enter another one: ", "88-DISK", 
                String.valueOf(defaultPort));
        int portNumber = 0;
        try {
            portNumber = Integer.decode(providedPort);
            if (cpu.attachDevice(port, portNumber) == false) {
                StaticDialogs.showErrorMessage("Error: the device still can't be attached");
                return -1;
            }
        } catch (NumberFormatException e) {
            StaticDialogs.showMessage("Bad number");
            return -1;
        }
        return portNumber;
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);

        cpu = (C738039DCA561A49F377859B108A9AD1EE6CBDACB)
                Context.getInstance().getCPUContext(pluginID,
                C738039DCA561A49F377859B108A9AD1EE6CBDACB.class);

        if (cpu == null) {
            StaticDialogs.showErrorMessage("Cannot connect to the CPU", "88-DISK");
            return false;
        }

        readSettings();

        // attach device to CPU
        if (cpu.attachDevice(port1, port1CPU) == false) {
            port1CPU = providePort(1,port1CPU,port1);
            if (port1CPU == -1) {
                StaticDialogs.showErrorMessage("88-DISK (port1) can not be "
                        + "attached to default CPU port");
                return false;
            }
        }
        if (cpu.attachDevice(port2, port2CPU) == false) {
            port2CPU = providePort(2, port2CPU, port2);
            if (port2CPU == -1) {
                StaticDialogs.showErrorMessage("88-DISK (port2) can not be "
                        + "attached to default CPU port");
                return false;
            }
        }
        if (cpu.attachDevice(port3, port3CPU) == false) {
            port3CPU = providePort(3, port3CPU, port3);
            if (port3CPU == -1) {
                StaticDialogs.showErrorMessage("88-DISK (port3) can not be "
                        + "attached to default CPU port");
                return false;
            }
        }
        return true;
    }

    private void readSettings() {
        String s;
        s = settings.readSetting(pluginID, "nogui");
        if (s != null && s.toUpperCase().equals("TRUE")) {
            noGUI = true;
        } else {
            noGUI = false;
        }
        s = settings.readSetting(pluginID, "port1CPU");
        if (s != null) {
            try {
                port1CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port1CPU = CPU_PORT1;
            }
        }
        s = settings.readSetting(pluginID, "port2CPU");
        if (s != null) {
            try {
                port2CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port2CPU = CPU_PORT2;
            }
        }
        s = settings.readSetting(pluginID, "port3CPU");
        if (s != null) {
            try {
                port3CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port3CPU = CPU_PORT3;
            }
        }
        for (int i = 0; i < 16; i++) {
            s = settings.readSetting(pluginID, "image" + i);
            if (s != null) {
                try {
                    drives.get(i).mount(s);
                } catch (IOException ex) {
                    StaticDialogs.showErrorMessage(ex.getMessage());
                }
            }
        }
        
        if (noGUI)
            return;

        gui = new DiskFrame(drives);
        s = settings.readSetting(pluginID, "always_on_top");
        if (s != null && s.toUpperCase().equals("TRUE")) {
            gui.setAlwaysOnTop(true);
        } else {
            gui.setAlwaysOnTop(false);
        }
    }

    @Override
    public void showGUI() {
        if (gui != null)
            gui.setVisible(true);
    }

    @Override
    public String getDescription() {
        return "MITS 88-DISK Floppy Disk controller with up to 16 drives. "
                + "The connected floppy drives were Pertec FD-400 8\" "
                + "hard-sectored floppy drives. Each single-sided "
                + "diskette has 77 tracks of 32, 137-byte sectors each (an "
                + "unformatted capacity of 337,568 bytes). The controller "
                + "supported neither interrupts nor DMA, so floppy access "
                + "required the sustained attention of the CPU. The standard "
                + "I/O addresses were 10Q-12Q.";
    }

    @Override
    public String getVersion() {
        return "0.26b1";
    }

    @Override
    public String getTitle() {
        return "MITS 88-DISK";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2008-2010, P. Jakubčo";
    }

    @Override
    public void destroy() {
        if (gui != null) {
            gui.dispose();
        }
        if (cpu != null) {
            cpu.detachDevice(0x8);
            cpu.detachDevice(0x9);
            cpu.detachDevice(0xA);
        }
        drives.clear();
    }

    @Override
    public void showSettings() {
        if (noGUI)
            return;
        new ConfigDialog(pluginID, settings, drives, gui).setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !noGUI;
    }
}
