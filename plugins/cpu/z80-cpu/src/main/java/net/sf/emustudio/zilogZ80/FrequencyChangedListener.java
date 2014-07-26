/*
 * FrequencyChangedListener.java
 *
 * Created on 18.6.2008, 9:31:16
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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

package net.sf.emustudio.zilogZ80;

import emulib.plugins.cpu.CPU.CPUListener;

/**
 * Listener of frequency changes
 * 
 * @author Peter Jakubčo
 */
public interface FrequencyChangedListener extends CPUListener {
    
    /**
     * Handle frequency changes of the CPU.
     * 
     * @param newFrequency new Frequency in kHz
     */
    public void frequencyChanged(float newFrequency);
}
