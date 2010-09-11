/*
 * SMemoryContext.java
 * (interface)
 *
 * Created on 18.6.2008, 8:29:36
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

import java.util.Hashtable;
import emuLib8.plugins.memory.IMemoryContext;

/**
 * Specific context for this kind of memory.
 * Supports banking, ROM ranges, loading HEX/BIN files.
 * 
 * @author vbmacher
 */
public interface C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8 extends IMemoryContext {
    public boolean isRom(int address);
    public Hashtable<Integer,Integer> getROMRanges();
    public void setRAM(int from, int to);
    public void setROM(int from, int to);

    public int getBanksCount();
    public short getSelectedBank();
    public void setSeletedBank(short bankSelect);
    public int getCommonBoundary();

    public boolean loadHex(String filename, int bank);
    public boolean loadBin(String filename, int address, int bank);
    
}
