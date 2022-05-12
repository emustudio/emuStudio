/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
 * Port 2, Control port.
 * <p>
 * IN: sector pos
 * OUT: set flags
 */
public class ControlPort implements DeviceContext<Byte> {
    private final DriveCollection disk;

    public ControlPort(DriveCollection disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Byte readData() {
        return disk.getCurrentDrive().map(drive -> {
            drive.nextSectorIfHeadIsLoaded();
            return drive.getPort2status();
        }).orElse(Drive.SECTOR0);
    }

    @Override
    public void writeData(Byte val) {
        disk.getCurrentDrive().ifPresent(drive -> drive.writeToPort2(val));
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public String toString() {
        return DIALOG_TITLE + " Control Port";
    }

}
