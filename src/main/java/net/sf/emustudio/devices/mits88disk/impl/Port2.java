/*
 * Port2.java
 *
 * Created on 18.6.2008, 15:10:20
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

/**
 * Port 2.
 * 
 * IN: sector pos
 * OUT: set flags
 *
 * @author Peter Jakubčo
 */
public class Port2 implements DeviceContext<Short> {
    private DiskImpl disk;

    public Port2(DiskImpl disk) {
        this.disk = disk;
    }

    @Override
    public Short read() {
        try {
            return disk.getCurrentDrive().getSectorPos();
        } catch (IndexOutOfBoundsException e) {
        }
        return 0;
    }

    @Override
    public void write(Short val) {
        try {
            disk.getCurrentDrive().setFlags(val);
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
