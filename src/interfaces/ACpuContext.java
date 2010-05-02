/*
 * ACpuContext.java
 * (interface)
 *
 * Created on 18.6.2008, 8:56:44
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

package interfaces;

import plugins.cpu.ICPUContext;
import plugins.device.IDeviceContext;

/**
 * CPU context for 8080 processor
 * @author vbmacher
 */
public interface ACpuContext extends ICPUContext {
    public boolean attachDevice(IDeviceContext listener, int port);
    public void detachDevice(int port);
    
    public void interrupt(byte[] instr);
}
