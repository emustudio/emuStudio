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
package net.emustudio.plugins.device.mits88disk.ports;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.mits88disk.DeviceImpl;
import net.emustudio.plugins.device.mits88disk.Drive;

import java.util.Objects;

/**
 * Port 2, Control port.
 * <p>
 * IN: sector pos
 * OUT: set flags
 */
public class ControlPort implements DeviceContext<Short> {
    private final DeviceImpl disk;

    public ControlPort(DeviceImpl disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Short readData() {
        Drive currentDrive = disk.getCurrentDrive();
        currentDrive.nextSectorIfHeadIsLoaded();

        return currentDrive.getPort2status();
    }

    @Override
    public void writeData(Short val) {
        disk.getCurrentDrive().writeToPort2(val);
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    @Override
    public String toString() {
        return "88-DISK Control Port";
    }

}
