/*
 * Port3.java
 *
 * Created on 18.6.2008, 15:13:58
 * hold to: KISS, YAGNI
 *
 * IN: read data
 * OUT: write data
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

import java.io.IOException;
import emulib.plugins.device.IDeviceContext;
import emulib.runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
public class Port3 implements IDeviceContext {

    private DiskImpl dsk;

    public Port3(DiskImpl dsk) {
        this.dsk = dsk;
    }

    public boolean attachDevice(IDeviceContext device) {
        return false;
    }

    public void detachDevice(IDeviceContext device) {
    }

    @Override
    public Object read() {
        short d = 0;
        try {
            d = ((Drive) dsk.drives.get(dsk.current_drive)).readData();
        } catch (IOException e) {
            StaticDialogs.showErrorMessage("Couldn't read from disk");
        }
        return d;
    }

    @Override
    public void write(Object val) {
        try {
            ((Drive) dsk.drives.get(dsk.current_drive)).writeData((Short) val);
        } catch (IOException e) {
            StaticDialogs.showErrorMessage("Couldn't write to disk");
        }
    }

    @Override
    public String getID() {
        return "88-DISK-PORT3";
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
