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

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;

/**
 * IN: sector pos
 * OUT: set flags
 * 
 * @author Peter Jakubčo
 */
@ContextType
public class Port2 implements DeviceContext {

    private DiskImpl dsk;

    public Port2(DiskImpl dsk) {
        this.dsk = dsk;
    }

    public boolean attachDevice(DeviceContext device) {
        return false;
    }

    public void detachDevice(DeviceContext device) {
    }

    @Override
    public Object read() {
        return ((Drive) dsk.drives.get(dsk.current_drive)).getSectorPos();
    }

    @Override
    public void write(Object val) {
        ((Drive) dsk.drives.get(dsk.current_drive)).setFlags((Short) val);
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
