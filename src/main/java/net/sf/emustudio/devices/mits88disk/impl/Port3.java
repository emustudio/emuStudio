/*
 * Port3.java
 *
 * Created on 18.6.2008, 15:13:58
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
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
import emulib.runtime.StaticDialogs;

/**
 * Port 3.
 * 
 * IN: read data
 * OUT: write data
 *
 * @author Peter Jakubčo
 */
public class Port3 implements DeviceContext<Short> {
    private DiskImpl disk;

    public Port3(DiskImpl disk) {
        this.disk = disk;
    }

    @Override
    public Short read() {
        short data = 0;
        try {
            data = disk.getCurrentDrive().readData();
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Couldn't read from disk");
        }
        return data;
    }

    @Override
    public void write(Short data) {
        try {
            disk.getCurrentDrive().writeData(data);
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Couldn't write to disk");
        }
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
