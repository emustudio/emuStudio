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
package net.sf.emustudio.memory.standard.impl;

import emulib.annotations.ContextType;
import emulib.plugins.memory.AbstractMemoryContext;
import emulib.runtime.HEXFileManager;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.memory.standard.StandardMemoryContext;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ContextType(id = "Standard Memory")
public class MemoryContextImpl extends AbstractMemoryContext<Short> implements StandardMemoryContext {
    public final static int DEFAULT_MEM_SIZE = 65536;

    private final List<AddressRange> romList = new ArrayList<>();

    public int lastImageStart = 0;
    private short[][] mem = new short[1][0];
    private int banksCount;
    private short bankSelect = 0;
    private int bankCommon = 0;
    private int activeBank;

    public void init(int size, int banks, int bankCommon) {
        if (banks <= 0) {
            throw new IllegalArgumentException("Number of banks must be >= 1!");
        }

        this.bankCommon = bankCommon;
        this.banksCount = banks;
        mem = new short[banks][size];
    }

    @Override
    public void clear() {
        for (short[] mem1 : mem) {
            for (int j = 0; j < mem1.length; j++) {
                mem1[j] = 0;
            }
        }
        lastImageStart = 0;
        notifyMemoryChanged(-1);
    }

    public void destroy() {
        clear();
        mem = null;
        activeBank = 0;
        banksCount = 0;
    }

    @Override
    public int getBanksCount() {
        return banksCount;
    }

    @Override
    public short getSelectedBank() {
        return bankSelect;
    }

    @Override
    public void selectBank(short bankSelect) {
        if (bankSelect < banksCount) {
            this.bankSelect = bankSelect;
        }
    }

    @Override
    public int getCommonBoundary() {
        return bankCommon;
    }

    @Override
    public boolean loadHex(String filename, int bank) {
        try {
            lastImageStart = HEXFileManager.loadIntoMemory(new File(filename), this);
        } catch (FileNotFoundException ex) {
            StaticDialogs.showErrorMessage("File not found: " + filename);
            return false;
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Error opening file: " + filename);
            return false;
        } finally {
            notifyMemoryChanged(-1);
        }
        return true;
    }

    @Override
    public boolean loadBin(String filename, int address, int bank) {
        lastImageStart = 0;
        File f = new File(filename);
        if (!f.isFile()) {
            StaticDialogs.showErrorMessage("Specified file name doesn't point to a file");
            return false;
        }

        try (RandomAccessFile binaryFile = new RandomAccessFile(f, "r")) {
            long position = 0, length = binaryFile.length();
            while (position < length) {
                mem[bank][address++] = (short) (binaryFile.readUnsignedByte() & 0xFF);
                position++;
            }
            binaryFile.close();
        } catch (EOFException ex) {
            // ignored intentionally
        } catch (FileNotFoundException ex) {
            StaticDialogs.showErrorMessage("File not found: " + filename);
            return false;
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Error opening file: " + filename);
            return false;
        } finally {
            notifyMemoryChanged(-1);
        }
        return true;
    }

    @Override
    public Short read(int from) {
        if (from < bankCommon) {
            return mem[bankSelect][from];
        } else {
            return mem[0][from];
        }
    }

    public Object read(int from, int bank) {
        if (from < bankCommon) {
            return mem[bank][from];
        } else {
            return mem[0][from];
        }
    }

    @Override
    public Short[] readWord(int from) {
        activeBank = (from < bankCommon) ? bankSelect : 0;
        return new Short[] { mem[activeBank][from] , mem[activeBank][from + 1] };
    }

    @Override
    public void write(int to, Short val) {
        if (!isROM(to)) {
            activeBank = (to < bankCommon) ? bankSelect : 0;
            mem[activeBank][to] = (short) (val & 0xFF);
            notifyMemoryChanged(to);
        }
    }

    public void write(int to, short val, int bank) {
        if (!isROM(to)) {
            activeBank = (to < bankCommon) ? bank : 0;
            mem[activeBank][to] = (short)(val & 0xFF);
            notifyMemoryChanged(to);
        }
    }

    @Override
    public void writeWord(int to, Short[] cells) {
        if (isROM(to)) {
            return;
        }
        activeBank = (to < bankCommon) ? bankSelect : 0;
        mem[activeBank][to] = (short)(cells[0] & 0xFF);
        mem[activeBank][to + 1] = (short)(cells[1] & 0xFF);
        notifyMemoryChanged(to);
        notifyMemoryChanged(to + 1);
    }

    @Override
    public int getSize() {
        return mem[0].length;
    }

    /**
     * Merges all continuous ranges into one.
     */
    private void mergeRanges() {
        if (romList.isEmpty()) {
            return;
        }
        Collections.sort(romList);

        AddressRange range = romList.get(0);
        for (int i = 1; i < romList.size(); i++) {
            AddressRange otherRange = romList.get(i);
            if (range.getStopAddress() == otherRange.getStartAddress() + 1) {
                // merge
                romList.remove(range);
                romList.remove(otherRange);
                range = new AddressRangeImpl(range.getStartAddress(), otherRange.getStopAddress());
                romList.add(range);
            } else {
                range = otherRange;
            }
        }
    }

    private void removeROMRange(AddressRange rangeToRemove) {
        if (rangeToRemove == null) {
            return;
        }
        int removeStartAddr = rangeToRemove.getStartAddress();
        int removeStopAddr = rangeToRemove.getStopAddress();

        List<AddressRange> toRemove = new ArrayList<>();
        List<AddressRange> toAdd = new ArrayList<>();
        
        romList.forEach(range -> {
            int startAddr = range.getStartAddress();
            int stopAddr = range.getStopAddress();

            if ((startAddr >= removeStartAddr) && (stopAddr <= removeStopAddr)) {
                toRemove.add(range);
            } else if ((startAddr < removeStartAddr) && (stopAddr >= removeStartAddr) && (stopAddr <= removeStopAddr)) {
                toRemove.add(range);
                toAdd.add(new AddressRangeImpl(startAddr, removeStartAddr - 1));
            } else if ((startAddr >= removeStartAddr) && (startAddr <= removeStopAddr) && (stopAddr > removeStopAddr)) {
                toRemove.add(range);
                toAdd.add(new AddressRangeImpl(removeStopAddr + 1, stopAddr));
            } else if ((startAddr < removeStartAddr) && (stopAddr > removeStopAddr)) {
                toRemove.add(range);
                toAdd.add(new AddressRangeImpl(startAddr, removeStartAddr - 1));
                toAdd.add(new AddressRangeImpl(removeStopAddr + 1, stopAddr));
            }
        });
        
        toRemove.forEach(romList::remove);
        toAdd.forEach(romList::add);
                
        mergeRanges();
    }

    private void addRomRange(AddressRange range) {
        removeROMRange(range);
        romList.add(range);
    }

    @Override
    public void setRAM(AddressRange range) {
        if (range.getStartAddress() > range.getStopAddress()) {
            throw new IllegalArgumentException("Range stop address must be > than start address!");
        }
        removeROMRange(range);
    }

    @Override
    public void setROM(AddressRange range) {
        if (range.getStartAddress() > range.getStopAddress()) {
            throw new IllegalArgumentException("Range stop address must be > than start address!");
        }
        addRomRange(range);
    }

    @Override
    public boolean isROM(int address) {
        for (AddressRange range : romList) {
            int startAddress = range.getStartAddress();
            int stopAddress = range.getStopAddress();
            if ((startAddress <= address) && (address <= stopAddress)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<AddressRange> getROMRanges() {
        return romList;
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
