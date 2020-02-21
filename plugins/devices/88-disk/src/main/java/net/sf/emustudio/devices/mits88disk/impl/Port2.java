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

import emulib.plugins.device.DeviceContext;

import java.util.Objects;

/**
 * Port 2.
 * <p>
 * IN: sector pos
 * OUT: set flags
 */
class Port2 implements DeviceContext<Short> {
    private final DiskImpl disk;

    Port2(DiskImpl disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Short read() {
        Drive currentDrive = disk.getCurrentDrive();
        currentDrive.nextSectorIfHeadIsLoaded();

        return currentDrive.getPort2status();
    }

    @Override
    public void write(Short val) {
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
