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
package net.emustudio.plugins.devices.mits88disk.ports;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.devices.mits88disk.DiskImpl;
import net.emustudio.plugins.devices.mits88disk.Drive;

import java.util.Objects;

/**
 * Port 1, Status port.
 * <p>
 * IN: disk flags
 * OUT: select/unselect drive
 */
public class StatusPort implements DeviceContext<Short> {
    private final DiskImpl disk;

    public StatusPort(DiskImpl disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Short readData() {
        // interpret port1 status
        return disk.getCurrentDrive().getPort1status();
    }

    @Override
    public void writeData(Short value) {
        // select device
        disk.setCurrentDrive(value & 0x0F);
        Drive drive = disk.getCurrentDrive();
        if ((value & 0x80) != 0) {
            // disable device
            drive.deselect();
            disk.setCurrentDrive(0xFF);
        } else {
            drive.select();
        }
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    @Override
    public String toString() {
        return "88-DISK Status Port";
    }
}
