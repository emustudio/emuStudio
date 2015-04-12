/*
 * Copyright (C) 2008-2015 Peter Jakubčo
 *
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
 */
package net.sf.emustudio.devices.mits88disk.impl;

import emulib.plugins.device.DeviceContext;

import java.util.Objects;

/**
 * Port 1.
 * 
 * IN: disk flags
 * OUT: select/unselect drive
 */
public class Port1 implements DeviceContext<Short> {
    private final DiskImpl disk;

    public Port1(DiskImpl disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Short read() {
        return disk.getCurrentDrive().getFlags();
    }

    @Override
    public void write(Short value) {
        // select device
        disk.setCurrentDrive(value & 0x0F);
        if ((value & 0x80) != 0) {
            // disable device
            disk.getCurrentDrive().deselect();
            disk.setCurrentDrive(0xFF);
        } else {
            disk.getCurrentDrive().select();
        }
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
