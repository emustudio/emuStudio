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
 * Port 1.
 * 
 * IN: disk flags
 * OUT: select/unselect drive
 */
class Port1 implements DeviceContext<Short> {
    private final DiskImpl disk;

    Port1(DiskImpl disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Short read() {
        // interpret port1 status
        Drive currentDrive = disk.getCurrentDrive();

        short status = currentDrive.getPort1status();

//        System.out.println("port1 = " + currentDrive.portStatusToString(status));
//        System.out.flush();
//
        return status;
    }

    @Override
    public void write(Short value) {
        // select device
        disk.setCurrentDrive(value & 0x0F);
        Drive drive = disk.getCurrentDrive();
        if ((value & 0x80) != 0) {
//            System.out.println("DISK UNSELECT " + value);
//            System.out.flush();
//
            // disable device
            drive.deselect();
            disk.setCurrentDrive(0xFF);
        } else {
//            System.out.println("DISK SELECT = " + value);
//            System.out.flush();
//
            drive.select();
        }
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }
}
