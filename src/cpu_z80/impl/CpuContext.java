/*
 * CpuContext.java
 *
 * Created on 18.6.2008, 8:50:11
 * hold to: KISS, YAGNI
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
package cpu_z80.impl;

import interfaces.C17E8D62E685AD7E54C209C30482E3C00C8C56ECC;
import java.util.Hashtable;
import emuLib8.plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public final class CpuContext implements C17E8D62E685AD7E54C209C30482E3C00C8C56ECC {

    private Hashtable<Integer, IDeviceContext> devicesList;
    private CpuZ80 z80;

    public CpuContext(CpuZ80 z80) {
        devicesList = new Hashtable<Integer, IDeviceContext>();
        this.z80 = z80;
    }

    @Override
    public String getID() {
        return "Z80Context";
    }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(IDeviceContext device, int port) {
        if (devicesList.containsKey(port)) {
            return false;
        }
        if (!device.getDataType().equals(Short.class)) {
            return false;
        }
        devicesList.put(port, device);
        return true;
    }

    @Override
    public void detachDevice(int port) {
        if (devicesList.containsKey(port)) {
            devicesList.remove(port);
        }
    }

    public void clearDevices() {
        devicesList.clear();
    }

    /**
     * Performs I/O operation.
     * @param port I/O port
     * @param read whether method should read or write to the port
     * @param val value to be written to the port. if parameter read is set to
     *            true, then val is ignored.
     * @return value from the port if read is true, otherwise 0
     */
    public short fireIO(int port, boolean read, short val) {
        if (devicesList.containsKey(port) == false) {
            // this behavior isn't constant for all situations...
            // on ALTAIR computer it depends on setting of one switch on front
            // panel (called IR or what..)
            return 0;
        }
        if (read == true) {
            return (Short) devicesList.get(port).read();
        } else {
            devicesList.get(port).write(val);
        }
        return 0;
    }

    // TODO implement, please...
    @Override
    public void interrupt(byte[] instr) {
        z80.setIntVector(instr);
    }

    @Override
    public boolean isInterruptSupported() {
        return true;
    }

    @Override
    public void setInterrupt(IDeviceContext device, int mask) {
        z80.setInterrupt(device, mask);
    }

    @Override
    public void clearInterrupt(IDeviceContext device, int mask) {
        z80.clearInterrupt(device, mask);
    }
}
