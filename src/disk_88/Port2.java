/*
 * Port2.java
 *
 * Created on 18.6.2008, 15:10:20
 * hold to: KISS, YAGNI
 *
 * IN: sector pos
 * OUT: set flags
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package disk_88;

import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class Port2 implements IDeviceContext {

    private DiskImpl dsk;

    public Port2(DiskImpl dsk) {
        this.dsk = dsk;
    }

    public boolean attachDevice(IDeviceContext device) {
        return false;
    }

    public void detachDevice(IDeviceContext device) {
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
    public String getID() {
        return "88-DISK-PORT2";
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
