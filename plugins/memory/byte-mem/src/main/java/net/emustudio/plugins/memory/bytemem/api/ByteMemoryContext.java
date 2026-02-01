/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.List;

/**
 * "Byte" memory context.
 * <p>
 * Supports bank switching, ROM ranges, and loading HEX/BIN files.
 */
@SuppressWarnings("unused")
@PluginContext(id = "Byte memory")
public interface ByteMemoryContext extends MemoryContext<Byte> {

    /**
     * Determine whether specified memory position is read-only.
     *
     * @param address memory position
     * @return true if the memory position is read only, false otherwise
     */
    boolean isReadOnly(int address);

    /**
     * Get list of ranges of read-only addresses.
     *
     * @return list of ROM memory addresses
     */
    List<? extends AddressRange> getReadOnly();

    /**
     * Set specified memory range as ROM (Read Only Memory).
     *
     * @param range address range
     */
    void setReadOnly(AddressRange range);

    /**
     * Set specified memory range as RAM (Random Access Memory).
     *
     * @param range address range
     */
    void setReadWrite(AddressRange range);

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
    int getSelectedBank();

    /**
     * Select (set as active) a memory bank.
     *
     * @param bankIndex index (number) of a bank which should be selected
     */
    void selectBank(int bankIndex);

    /**
     * Return an address in the memory which represents a boundary from which
     * the memory banks have the same content. Before this address all banks
     * can have different content.
     *
     * @return common boundary address
     */
    int getCommonBoundary();

    /**
     * Returns raw memory represented by Java array.
     * <p>
     * Memory notifications must be handled manually if this array changes.
     *
     * @return raw memory
     */
    Byte[][] getRawMemory();

    /**
     * This interface represents a range of addresses in the memory.
     */
    interface AddressRange {
        int getStartAddress();

        int getStopAddress();
    }
}
