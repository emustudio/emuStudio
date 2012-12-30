/*
 * StandardMemoryContext.java
 *
 * Created on 18.6.2008, 8:29:36
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

package net.sf.emustudio.memory.standard;

import emulib.annotations.ContextType;
import emulib.plugins.memory.MemoryContext;
import java.util.List;

/**
 * Extended memory context.
 * 
 * Supports bank switching, ROM ranges, and loading HEX/BIN files.
 * 
 * @author Peter Jakubčo
 */
@ContextType
public interface StandardMemoryContext extends MemoryContext<Short> {
    
    /**
     * This interface represents a range of addresses in the memory.
     */
    public interface AddressRange extends Comparable<AddressRange> {
        public int getStartAddress();
        public int getStopAddress();
    }
    
    /**
     * Determine whether specified memory position is read-only.
     * 
     * @param address memory position
     * @return true if the memory position is read only, false otherwise
     */
    public boolean isROM(int address);
    
    /**
     * Get list of ranges of adresses in memory which are read-only.
     * 
     * @return list of ROM memory addresses
     */
    public List<AddressRange> getROMRanges();
    
    /**
     * Set specified memory range as RAM (Random Access Memory).
     * 
     * @param range address range
     */
    public void setRAM(AddressRange range);
    
    /**
     * Set specified memory range as ROM (Read Only Memory).
     * 
     * @param range address range
     */
    public void setROM(AddressRange range);

    /**
     * Get number of available memory banks.
     * 
     * @return count of memory banks
     */
    public int getBanksCount();
    
    /**
     * Get index of the selected memory bank.
     * 
     * @return index of active (selected) memory bank
     */
    public short getSelectedBank();
    
    /**
     * Select (set as active) a memory bank.
     * 
     * @param bankIndex index (number) of a bank which should be selected
     */
    public void selectBank(short bankIndex);
    
    /**
     * Return an address in the memory which represents a boundary from which
     * the memory banks have the same content. Before this address all banks
     * can have different content.
     * 
     * @return common boundary address
     */
    public int getCommonBoundary();

    /**
     * Loads a HEX file into the memory.
     * 
     * @param filename file name
     * @param bank bank index
     * @return true if the file was loaded successfully, false otherwise
     */
    public boolean loadHex(String filename, int bank);
    
    /**
     * Loads a binary file into the memory.
     * 
     * @param filename file name
     * @param address location in the memory
     * @param bank bank index
     * @return true if the file was loaded successfully, false otherwise
     */
    public boolean loadBin(String filename, int address, int bank);
    
}
