/*
 * CpuPort2.java
 *
 * Created on 18.6.2008, 14:30:59
 * hold to: KISS, YAGNI
 *
 * This is the data port of 88-SIO card.
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
 *
 * ----------------------------------------------------------------------------
 * A read to the data port gets the buffered character, a write
 * to the data port writes the character to the device.
 *
 */

package sio88;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class CpuPort2 implements IDeviceContext {
    private Mits88SIO sio;
    private IDeviceContext dev;
    
    public CpuPort2(Mits88SIO sio) {
        this.sio = sio;
    }

    public void attachDevice(IDeviceContext device) { this.dev = device; }
    public void detachDevice() { this.dev = null; }
    public IDeviceContext getAttachedDevice() { return dev; }
    
    @Override
    public void out(EventObject evt, Object data) {
        if (dev == null) return;
        dev.out(evt, data);
    }

    @Override
    public Object in(EventObject evt) {
    //    if (buffer == 0 && gui != null) {
      //      // get key from terminal (polling)
        //    buffer = gui.getChar();
       // }
        short v = sio.buffer;
        sio.status &= 0xFE;
        sio.buffer = 0;
        return v;
    }

    /**
     * This is communication method between device and
     * SIO. For terminal: If user pressed a key, then it is
     * sent from terminal to SIO device via this method.
     */
    public void writeBuffer(short data) {
        sio.status |= 0x01;
        sio.buffer = data;
    }
    

    @Override
    public String getID() { return "88-SIO-PORT2"; }

	@Override
	public Class<?> getDataType() {
		return Short.class;
	}

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}


}
