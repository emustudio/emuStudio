/*
 * Port1.java
 *
 * Created on 18.6.2008, 15:01:27
 * hold to: KISS, YAGNI
 *
 * IN:  disk flags
 * OUT: select/unselect drive
 *
 * Copyright (C) 2008-2012 Peter Jakubčo <pjakubco@gmail.com>
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

import emulib.plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class Port1 implements IDeviceContext {

    private DiskImpl dsk;

    public Port1(DiskImpl dsk) {
        this.dsk = dsk;
    }

    public boolean attachDevice(IDeviceContext device) {
        return false;
    }

    public void detachDevice(IDeviceContext device) {
    }

    @Override
    public Object read() {
        return ((Drive) dsk.drives.get(dsk.current_drive)).getFlags();
    }

    @Override
    public void write(Object val) {
        short v = (Short) val;
        // select device
        dsk.current_drive = v & 0x0F;
        if ((v & 0x80) != 0) {
            // disable device
            ((Drive) dsk.drives.get(dsk.current_drive)).deselect();
            dsk.current_drive = 0xFF;
        } else {
            ((Drive) dsk.drives.get(dsk.current_drive)).select();
        }
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }

    @Override
    public String getID() {
        return "88-DISK-PORT1";
    }
}
