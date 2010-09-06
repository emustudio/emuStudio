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

package cpu_8080.impl;

import interfaces.C17E8D62E685AD7E54C209C30482E3C00C8C56ECC;
import java.util.Hashtable;
import plugins.device.IDeviceContext;


/**
 *
 * @author vbmacher
 */
public class CpuContext implements C17E8D62E685AD7E54C209C30482E3C00C8C56ECC {
    private Hashtable<Integer,IDeviceContext> devicesList;
    private int clockFrequency = 2000; // kHz
    private Cpu8080 cpu;

    public CpuContext(Cpu8080 cpu) {
        devicesList = new Hashtable<Integer,IDeviceContext>();
        this.cpu = cpu;
    }
    
    @Override
    public String getID() { return "i8080_context"; }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(IDeviceContext listener, int port) {
        if (devicesList.containsKey(port)) return false;
        if (listener.getDataType() != Short.class) return false;
        devicesList.put(port, listener);
        return true;
    }
    @Override
    public void detachDevice(int port) {
        if (devicesList.containsKey(port))
            devicesList.remove(port);
    }
    
    public void clearDevices() { devicesList.clear(); }
    
    public int getFrequency() { return this.clockFrequency; }
    // frequency in kHz
    public void setFrequency(int freq) { this.clockFrequency = freq; }
    
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
            return 0;
        }
        if (read == true) 
            return (Short)devicesList.get(port).read();
        else devicesList.get(port).write(val);
        return 0;
    }

    /*
     * the interrupting device can insert any instruction on the data bus
     * for execution by the CPU. The first byte of a multi-byte instruction
     * is read during the interrupt acknowledge cycle. Subsequent bytes are
     * read in by a normal memory read sequence.
     */
    @Override
    public void interrupt(byte[] instr) {
        short b1 = (instr.length >= 1) ? instr[0] : 0;
        short b2 = (instr.length >= 2) ? instr[1] : 0;
        short b3 = (instr.length >= 3) ? instr[2] : 0;
        cpu.interrupt(b1, b2, b3);
    }

    @Override
    public boolean isInterruptSupported() {
        return true;
    }

    @Override
    public void setInterrupt(IDeviceContext device, int mask) {

    }

    @Override
    public void clearInterrupt(IDeviceContext device, int mask) {
        
    }

}
