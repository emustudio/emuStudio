/**
 * BrainCPUContext.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package braincpu.impl;

import interfaces.C7DC7DAD9D43BACD78DD57E84262789E50BB7D7D8;
import emulib.plugins.device.IDeviceContext;

public class BrainCPUContext implements C7DC7DAD9D43BACD78DD57E84262789E50BB7D7D8 {
    private IDeviceContext device;

    public BrainCPUContext() {
        device = null;
    }
    
    @Override
    public String getID() {
        return "brain-cpu-context";
    }

    /**
     * Pripojenie zariadenia do CPU. Procesor BainCPU môže mať
     * pripojené len jedno zariadenie, a to terminál
     * 
     * @param listener
     * @param port
     * @return
     */
    @Override
    public boolean attachDevice(IDeviceContext device) {
    	if (this.device != null) return false;
    	if (device.getDataType() != Short.class) return false;
    	this.device = device;
        return true;
    }
    @Override
    public void detachDevice() {
    	device = null;
    }
    
    /**
     * Metóda zapíše do zariadenia hodnotu val. Táto
     * metóda teda priamo komunikuje so zariadením.
     * @param val hodnota, ktorá sa má zapísať do zariadenia
     */
    public void writeToDevice(short val) {
    	if (device == null) return;
    	device.write(val);
    }
    
    /**
     * Metóda prečíta hodnotu zo zariadenia. Ak zariadenie
     * nemá nič čo poslať, nech pošle 0.. Táto metóda
     * teda priamo komunikuje so zariadením.
     * @return hodnotu zo zariadenia
     */
    public short readFromDevice() {
    	if (device == null) return 0;
    	return (Short)device.read();
    }

    @Override
    public boolean isInterruptSupported() {
        return false;
    }

    @Override
    public void setInterrupt(IDeviceContext device, int mask) {
    }

    @Override
    public void clearInterrupt(IDeviceContext device, int mask) {
    }

}
