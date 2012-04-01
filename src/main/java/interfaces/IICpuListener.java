/*
 * IICpuListener.java
 * (interface)
 *
 * Created on 18.6.2008, 9:31:16
 * hold to: KISS, YAGNI, DRY
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

package interfaces;

import java.util.EventObject;
import emulib.plugins.cpu.ICPU.ICPUListener;

/**
 *
 * @author vbmacher
 */
public interface IICpuListener extends ICPUListener {
    public void frequencyChanged(EventObject evt, float freq);
}
