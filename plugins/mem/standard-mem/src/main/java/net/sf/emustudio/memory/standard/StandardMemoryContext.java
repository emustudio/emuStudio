/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
 */
@ContextType(id = "Standard memory")
public interface StandardMemoryContext extends MemoryContext<Short> {
    
    /**
     * This interface represents a range of addresses in the memory.
     */
    interface AddressRange {
        int getStartAddress();
        int getStopAddress();
    }
    
    /**
     * Determine whether specified memory position is read-only.
     * 
     * @param address memory position
     * @return true if the memory position is read only, false otherwise
     */
    boolean isROM(int address);
    
    /**
     * Get list of ranges of read-only addresses.
     * 
     * @return list of ROM memory addresses
     */
    List<? extends AddressRange> getROMRanges();

    /**
     * Set specified memory range as RAM (Random Access Memory).
     * 
     * @param range address range
     */
    void setRAM(AddressRange range);
    
    /**
     * Set specified memory range as ROM (Read Only Memory).
     * 
     * @param range address range
     */
    void setROM(AddressRange range);

    /**
     * Get number of available memory banks.
     * 
     * @return count of memory banks
     */
    int getBanksCount();
    
    /**
     * Get index of the selected memory bank.
     * 
     * @return index of active (selected) memory bank
     */
    short getSelectedBank();
    
    /**
     * Select (set as active) a memory bank.
     * 
     * @param bankIndex index (number) of a bank which should be selected
     */
    void selectBank(short bankIndex);
    
    /**
     * Return an address in the memory which represents a boundary from which
     * the memory banks have the same content. Before this address all banks
     * can have different content.
     * 
     * @return common boundary address
     */
    int getCommonBoundary();

    /**
     * Loads a HEX file into the memory.
     * 
     * @param filename file name
     * @param bank bank index
     * @return true if the file was loaded successfully, false otherwise
     */
    boolean loadHex(String filename, int bank);
    
    /**
     * Loads a binary file into the memory.
     * 
     * @param filename file name
     * @param address location in the memory
     * @param bank bank index
     * @return true if the file was loaded successfully, false otherwise
     */
    boolean loadBin(String filename, int address, int bank);
    
}
