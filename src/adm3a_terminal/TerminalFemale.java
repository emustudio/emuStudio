/*
 * TerminalFemale.java
 *
 * Created on 28.7.2008, 21:38:02
 * hold to: KISS, YAGNI
 * 
 * Female plug
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

package terminal;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class TerminalFemale implements IDeviceContext {
    private IDeviceContext dev;
        
    public void attachDevice(IDeviceContext device) { this.dev = device; }
    public void detachDevice() { this.dev = null; }

    @Override
    public Object in(EventObject evt) { return (short)0; }

    @Override
    public void out(EventObject evt, Object val) {
        if (dev == null) return;
        dev.out(evt, val);
    }

    @Override
    public String getID() { return "ADM-3A"; }
    
	@Override
	public Class<?> getDataType() {
		return Short.class;
	}
	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

}
