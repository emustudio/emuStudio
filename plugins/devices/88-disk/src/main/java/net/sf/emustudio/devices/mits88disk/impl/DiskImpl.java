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
package net.sf.emustudio.devices.mits88disk.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.devices.mits88disk.gui.DiskFrame;
import net.sf.emustudio.devices.mits88disk.gui.SettingsDialog;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

import static net.sf.emustudio.devices.mits88disk.impl.SettingsConstants.IMAGE;
import static net.sf.emustudio.devices.mits88disk.impl.SettingsConstants.PORT1_CPU;
import static net.sf.emustudio.devices.mits88disk.impl.SettingsConstants.PORT2_CPU;
import static net.sf.emustudio.devices.mits88disk.impl.SettingsConstants.PORT3_CPU;
import static net.sf.emustudio.devices.mits88disk.impl.SettingsConstants.SECTORS_COUNT;
import static net.sf.emustudio.devices.mits88disk.impl.SettingsConstants.SECTOR_LENGTH;

/**
 * MITS 88-DISK Floppy Disk controller with up to 16 drives
 */ 
@PluginType(
        type = PLUGIN_TYPE.DEVICE,
        title = "MITS 88-DISK device",
        copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
        description = "Implementation of popular floppy disk controller."
)
@SuppressWarnings("unused")
public class DiskImpl extends AbstractDevice {
    private final static Logger LOGGER = LoggerFactory.getLogger(DiskImpl.class);
    
    private final static int DRIVES_COUNT = 16;
    public final static int DEFAULT_CPU_PORT1 = 0x8;
    public final static int DEFAULT_CPU_PORT2 = 0x9;
    public final static int DEFAULT_CPU_PORT3 = 0xA;

    private final ContextPool contextPool;
    private final List<Drive> drives = new ArrayList<>();
    private final Port1 port1;
    private final Port2 port2;
    private final Port3 port3;

    private SettingsManager settings;
    private ExtendedContext cpuContext;

    private int port1CPU;
    private int port2CPU;
    private int port3CPU;
    private int currentDrive;
    private DiskFrame gui;
    private boolean noGUI = false;

    public DiskImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);

        for (int i = 0; i < DRIVES_COUNT; i++) {
            drives.add(new Drive(i));
        }

        this.currentDrive = 0xFF;
        port1CPU = DEFAULT_CPU_PORT1;
        port2CPU = DEFAULT_CPU_PORT2;
        port3CPU = DEFAULT_CPU_PORT3;

        port1 = new Port1(this);
        port2 = new Port2(this);
        port3 = new Port3(this);
    }

    /**
     * Asks the user for new port number and tries to attach given port to this
     * port number on CPU.
     *
     * If a port of the DISK cannot be attached to the CPU, we want to ask the
     * user to provide another port number. He got only one chance.
     *
     * @param DISKport Port number in 88-DISK (1,2,3)
     * @param defaultPort Default port number on CPU
     * @param port The 88-DISK port object that needs to be attached
     * @return new port number if the attachment was successful, -1 otherwise
     */
    private int providePort(int DISKport, int defaultPort, DeviceContext port) {
        String providedPort = StaticDialogs.inputStringValue("Port "
                + DISKport + " can not be attached to default"
                + " CPU port, please enter another one: ", "88-DISK",
                String.valueOf(defaultPort));
        int portNumber;
        try {
            portNumber = Integer.decode(providedPort);
            if (!cpuContext.attachDevice(port, portNumber)) {
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
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        this.settings = Objects.requireNonNull(settings);
        cpuContext = contextPool.getCPUContext(pluginID, ExtendedContext.class);

        readSettings();
        // attach device to CPU
        if (!cpuContext.attachDevice(port1, port1CPU)) {
            port1CPU = providePort(1, port1CPU, port1);
            if (port1CPU == -1) {
                throw new PluginInitializationException(
                        this, ": 88-DISK (port1) can not be attached to default CPU port"
                );
            }
        }
        if (!cpuContext.attachDevice(port2, port2CPU)) {
            port2CPU = providePort(2, port2CPU, port2);
            if (port2CPU == -1) {
                throw new PluginInitializationException(
                        this, ": 88-DISK (port2) can not be attached to default CPU port"
                );
            }
        }
        if (!cpuContext.attachDevice(port3, port3CPU)) {
            port3CPU = providePort(3, port3CPU, port3);
            if (port3CPU == -1) {
                throw new PluginInitializationException(
                        this, ": 88-DISK (port3) can not be attached to default CPU port"
                );
            }
        }

        if (!noGUI) {
            gui = new DiskFrame(drives);
        }
    }

    private void readSettings() {
        String s;
        s = settings.readSetting(pluginID, SettingsManager.NO_GUI);
        noGUI = s != null && s.toUpperCase().equals("TRUE");
        s = settings.readSetting(pluginID, PORT1_CPU);
        if (s != null) {
            try {
                port1CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                LOGGER.error("Could not read Port 1 number, using default.", e);
                port1CPU = DEFAULT_CPU_PORT1;
            }
        }
        s = settings.readSetting(pluginID, PORT2_CPU);
        if (s != null) {
            try {
                port2CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                LOGGER.error("Could not read Port 2 number, using default.", e);
                port2CPU = DEFAULT_CPU_PORT2;
            }
        }
        s = settings.readSetting(pluginID, PORT3_CPU);
        if (s != null) {
            try {
                port3CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                LOGGER.error("Could not read Port 3 number, using default.", e);
                port3CPU = DEFAULT_CPU_PORT3;
            }
        }
        for (int i = 0; i < DRIVES_COUNT; i++) {
            String sectorsCountStr = settings.readSetting(pluginID, SECTORS_COUNT + i);
            String sectorLengthStr = settings.readSetting(pluginID, SECTOR_LENGTH + i);

            short sectorsCount = (sectorsCountStr == null) ? Drive.DEFAULT_SECTORS_COUNT : Short.parseShort(sectorsCountStr);
            short sectorLength = (sectorLengthStr == null) ? Drive.DEFAULT_SECTOR_LENGTH : Short.parseShort(sectorLengthStr);

            Drive drive = drives.get(i);
            drive.setSectorsCount(sectorsCount);
            drive.setSectorLength(sectorLength);

            s = settings.readSetting(pluginID, IMAGE + i);
            if (s != null) {
                try {
                    drive.mount(new File(s));
                } catch (IOException ex) {
                    LOGGER.error("Could not mount image file {}", s, ex);                    
                    StaticDialogs.showErrorMessage(ex.getMessage());
                }
            }
        }
    }

    @Override
    public void showGUI() {
        if (gui != null) {
            gui.setVisible(true);
        }
    }

    @Override
    public String getVersion() {
        return Main.getVersion();
    }

    @Override
    public void destroy() {
        if (gui != null) {
            gui.dispose();
        }
        if (cpuContext != null) {
            cpuContext.detachDevice(0x8);
            cpuContext.detachDevice(0x9);
            cpuContext.detachDevice(0xA);
        }
        drives.clear();
    }

    @Override
    public void showSettings() {
        if (noGUI) {
            return;
        }
        new SettingsDialog(gui, pluginID, settings, drives).setVisible(true);
    }

    public Drive getCurrentDrive() {
        return drives.get(currentDrive);
    }

    public void setCurrentDrive(int index) {
        currentDrive = index;
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !noGUI;
    }

}
