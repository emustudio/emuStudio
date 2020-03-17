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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Port 3, Data port.
 * <p>
 * IN: read data
 * OUT: write data
 */
public class DataPort implements DeviceContext<Short> {
    private final static Logger LOGGER = LoggerFactory.getLogger(DataPort.class);

    private final DeviceImpl disk;

    public DataPort(DeviceImpl disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public Short readData() {
        short data = 0;
        try {
            data = disk.getCurrentDrive().readData();
        } catch (Exception e) {
            LOGGER.error("Could not read from disk", e);
        }
        return data;
    }

    @Override
    public void writeData(Short data) {
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
