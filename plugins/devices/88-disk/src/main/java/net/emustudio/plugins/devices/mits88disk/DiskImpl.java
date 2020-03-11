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
package net.emustudio.plugins.devices.mits88disk;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.devices.mits88disk.gui.DiskFrame;
import net.emustudio.plugins.devices.mits88disk.gui.SettingsDialog;
import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import net.emustudio.plugins.devices.mits88disk.ports.StatusPort;
import net.emustudio.plugins.devices.mits88disk.ports.ControlPort;
import net.emustudio.plugins.devices.mits88disk.ports.DataPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PluginRoot(
    type = PLUGIN_TYPE.DEVICE,
    title = "MITS 88-DISK device"
)
@SuppressWarnings("unused")
public class DiskImpl extends AbstractDevice {
    private final static Logger LOGGER = LoggerFactory.getLogger(DiskImpl.class);

    private final static int DRIVES_COUNT = 16;
    public final static int DEFAULT_CPU_PORT1 = 0x8;
    public final static int DEFAULT_CPU_PORT2 = 0x9;
    public final static int DEFAULT_CPU_PORT3 = 0xA;

    private final List<Drive> drives = new ArrayList<>();
    private final StatusPort statusPort;
    private final ControlPort controlPort;
    private final DataPort dataPort;

    private ExtendedContext cpuContext;

    private int port1CPU;
    private int port2CPU;
    private int port3CPU;
    private int currentDrive;
    private DiskFrame gui;
    private final boolean guiNotSupported;

    public DiskImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);

        for (int i = 0; i < DRIVES_COUNT; i++) {
            drives.add(new Drive(i));
        }

        this.currentDrive = 0xFF;
        port1CPU = DEFAULT_CPU_PORT1;
        port2CPU = DEFAULT_CPU_PORT2;
        port3CPU = DEFAULT_CPU_PORT3;

        statusPort = new StatusPort(this);
        controlPort = new ControlPort(this);
        dataPort = new DataPort(this);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        readSettings();
        ContextPool contextPool = applicationApi.getContextPool();
        if (contextPool != null) {
            cpuContext = contextPool.getCPUContext(pluginID, ExtendedContext.class);

            // attach device to CPU
            port1CPU = attachPort(1, statusPort, port1CPU);
            port2CPU = attachPort(2, controlPort, port2CPU);
            port3CPU = attachPort(3, dataPort, port3CPU);
        }

        if (!guiNotSupported) {
            gui = new DiskFrame(drives);
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
    public String getCopyright() {
        return "\u00A9 Copyright 2006-2020, Peter Jakubčo";
    }

    @Override
    public String getDescription() {
        return "MITS 88-DISK floppy disk controller.";
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
        if (guiNotSupported) {
            return;
        }
        new SettingsDialog(gui, settings, drives, applicationApi.getDialogs()).setVisible(true);
    }

    public Drive getCurrentDrive() {
        return drives.get(currentDrive);
    }

    public void setCurrentDrive(int index) {
        currentDrive = index;
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !guiNotSupported;
    }

    private void readSettings() {
        port1CPU = settings.getInt(SettingsConstants.PORT1_CPU, DEFAULT_CPU_PORT1);
        port2CPU = settings.getInt(SettingsConstants.PORT2_CPU, DEFAULT_CPU_PORT2);
        port3CPU = settings.getInt(SettingsConstants.PORT3_CPU, DEFAULT_CPU_PORT3);

        for (int i = 0; i < DRIVES_COUNT; i++) {
            Drive drive = drives.get(i);

            int sectorLength = settings.getInt(SettingsConstants.SECTOR_LENGTH + i, Drive.DEFAULT_SECTOR_LENGTH);
            drive.setSectorLength(sectorLength);

            int sectorsCount = settings.getInt(SettingsConstants.SECTORS_COUNT + i, Drive.DEFAULT_SECTORS_COUNT);
            drive.setSectorsCount(sectorsCount);

            String imagePath = settings.getString(SettingsConstants.IMAGE + i, null);
            Optional.ofNullable(imagePath).ifPresent(path -> {
                try {
                    drive.mount(Path.of(path));
                } catch (IOException ex) {
                    LOGGER.error("Could not mount image file {}", path, ex);
                    applicationApi.getDialogs().showError(
                        "Could not mount image file: " + path + ". Please see log file for more details.",
                        "MITS 88-DISK"
                    );
                }
            });
        }
    }

    private int attachPort(int diskPortNumber, DeviceContext<Short> diskPort, int cpuPort) throws PluginInitializationException {
        if (cpuContext.attachDevice(diskPort, cpuPort)) {
            return cpuPort;
        } else {
            int portNumber = cpuPort;
            boolean enteredValid = false;
            Dialogs dialogs = applicationApi.getDialogs();

            while (!enteredValid) {
                try {
                    portNumber = dialogs.readInteger(
                        "Port " + diskPortNumber + " could not be attached to CPU port " + cpuPort + "." +
                            "\nPlease enter another CPU port:",
                        "88-DISK"
                    ).orElseThrow(() -> new PluginInitializationException(
                        this, ": 88-DISK (port " + diskPortNumber + ") can not be attached to default CPU port " + cpuPort
                    ));

                    enteredValid = true;
                } catch (NumberFormatException e) {
                    dialogs.showError("Invalid number format", "MITS 88-DISK");
                }
            }

            if (!cpuContext.attachDevice(diskPort, portNumber)) {
                dialogs.showError(
                    "Port " + diskPortNumber + " still cannot be attached to provided CPU port " + portNumber,
                    "MITS 88-DISK"
                );
                throw new PluginInitializationException(
                    this, ": 88-DISK (port " + diskPortNumber + ") can not be attached to CPU port " + portNumber
                );
            }
            return portNumber;
        }
    }
}
