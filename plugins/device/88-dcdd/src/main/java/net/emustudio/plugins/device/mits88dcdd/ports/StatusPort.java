/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88dcdd.ports;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;

import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

/**
 * Port 1, Status port.
 * <p>
 * IN: disk flags
 * OUT: select/unselect drive
 */
public class StatusPort implements DeviceContext<Byte> {
    private final DriveCollection disk;

    public StatusPort(DriveCollection disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Byte readData() {
        return disk.getCurrentDrive().map(Drive::getPort1status).orElse(Drive.DEAD_DRIVE);
    }

    @Override
    public void writeData(Byte value) {
        if ((value & 0x80) != 0) {
            disk.getCurrentDrive().ifPresent(Drive::deselect);
            disk.unsetCurrentDrive();
        } else {
            disk.setCurrentDrive(value & 0x0F);
            disk.getCurrentDrive().ifPresent(Drive::select);
        }
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public String toString() {
        return DIALOG_TITLE + " Status Port";
    }
}
