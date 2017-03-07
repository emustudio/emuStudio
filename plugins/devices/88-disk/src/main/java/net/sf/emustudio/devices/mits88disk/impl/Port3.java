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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Port 3.
 * 
 * IN: read data
 * OUT: write data
 */
class Port3 implements DeviceContext<Short> {
    private final static Logger LOGGER = LoggerFactory.getLogger(Port3.class);

    private final DiskImpl disk;

    Port3(DiskImpl disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Short read() {
        short data = 0;
        try {
            data = disk.getCurrentDrive().readData();
        } catch (Exception e) {
            LOGGER.error("Could not read from disk", e);
        }
        return data;
    }

    @Override
    public void write(Short data) {
        try {
            disk.getCurrentDrive().writeData(data);
        } catch (Exception e) {
            LOGGER.error("Could not write to disk", e);
        }
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    @Override
    public String toString() {
        return "88-DISK Data Port";
    }
}
