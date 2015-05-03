/*
 * Copyright (C) 2008-2015 Peter Jakubčo
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
package net.sf.emustudio.memory.standard.impl;

import emulib.annotations.ContextType;
import emulib.plugins.memory.AbstractMemoryContext;
import emulib.runtime.HEXFileManager;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.memory.standard.StandardMemoryContext;
import net.sf.emustudio.memory.standard.gui.MemoryFrame;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ContextType(id = "Standard Memory")
public class MemoryContextImpl extends AbstractMemoryContext<Short, Integer> implements StandardMemoryContext {
    public final static int DEFAULT_MEM_SIZE = 65536;

    private final List<AddressRange> romList = new ArrayList<>();

    public int lastImageStart = 0;
    private short[][] mem = new short[0][0];
    private int banksCount;
    private short bankSelect = 0;
    private int bankCommon = 0;
    private int activeBank;
    private MemoryFrame gui;

    public void init(int size, int banks, int bankCommon, MemoryFrame gui) {
        if (banks <= 0) {
            throw new IllegalArgumentException("Number of banks must be >= 1!");
        }

        this.gui = gui; // can be null
        this.bankCommon = bankCommon;
        this.banksCount = banks;
        mem = new short[banks][size];
    }

    @Override
    public void clear() {
        for (int i = 0; i < mem.length; i++) {
            for (int j = 0; j < mem[i].length; j++) {
                mem[i][j] = 0;
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
            if (gui != null) {
                gui.updateBank(bankSelect);
            }
        }
    }

    @Override
    public int getCommonBoundary() {
        return bankCommon;
    }

    @Override
    public boolean loadHex(String filename, int bank) {
        try {
            lastImageStart = HEXFileManager.loadIntoMemory(filename, this);
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
    public Integer readWord(int from) {
        activeBank = (from < bankCommon) ? bankSelect : 0;
        if (from == mem[0].length - 1) {
            return (int)mem[activeBank][from];
        }
        int low = mem[activeBank][from] & 0xFF;
        int high = mem[activeBank][from + 1];
        return (high << 8) | low;
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
    public void writeWord(int to, Integer val) {
        if (isROM(to)) {
            return;
        }
        activeBank = (to < bankCommon) ? bankSelect : 0;
        short low = (short) (val & 0xFF);
        mem[activeBank][to] = low;
        notifyMemoryChanged(to);
        if (to < mem.length - 1) {
            short high = (short) ((val >>> 8) & 0xFF);
            mem[activeBank][to + 1] = high;
            notifyMemoryChanged(to + 1);
        }
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

        for (AddressRange range : romList) {
            int startAddr = range.getStartAddress();
            int stopAddr = range.getStopAddress();

            if ((startAddr >= removeStartAddr) && (stopAddr <= removeStopAddr)) {
                romList.remove(range);
                continue;
            }
            if ((startAddr < removeStartAddr) && (stopAddr >= removeStartAddr) && (stopAddr <= removeStopAddr)) {
                romList.remove(range);
                romList.add(new AddressRangeImpl(startAddr, removeStartAddr - 1));
                continue;
            }
            if ((startAddr >= removeStartAddr) && (startAddr <= removeStopAddr) && (stopAddr > removeStopAddr)) {
                romList.remove(range);
                romList.add(new AddressRangeImpl(removeStopAddr + 1, stopAddr));
                continue;
            }
            if ((startAddr < removeStartAddr) && (stopAddr > removeStopAddr)) {
                romList.remove(range);
                romList.add(new AddressRangeImpl(startAddr, removeStartAddr - 1));
                romList.add(new AddressRangeImpl(removeStopAddr + 1, stopAddr));
            }
        }
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
