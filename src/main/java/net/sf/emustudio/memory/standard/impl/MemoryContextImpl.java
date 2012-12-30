/*
 * MemoryContextImpl.java
 *
 * Created on 18.6.2008, 8:00:16
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
package net.sf.emustudio.memory.standard.impl;

import emulib.plugins.memory.AbstractMemoryContext;
import emulib.runtime.HEXFileManager;
import emulib.runtime.StaticDialogs;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sf.emustudio.memory.standard.StandardMemoryContext;
import net.sf.emustudio.memory.standard.gui.MemoryFrame;

public class MemoryContextImpl extends AbstractMemoryContext<Short> implements StandardMemoryContext {

    public int lastImageStart = 0;
    private boolean lastStartSet = false;
    private short[][] mem;
    private boolean sizeSet; // whether memory was initialized (created)
    private int banksCount;
    private short bankSelect = 0;
    private int bankCommon = 0;
    private int activeBank;
    private MemoryFrame gui;
    private List<AddressRange> romList;

    public static class AddressRangeImpl implements AddressRange {

        private int startAddress;
        private int stopAddress;

        public AddressRangeImpl(int startAddress, int stopAddress) {
            this.startAddress = startAddress;
            this.stopAddress = stopAddress;
        }

        @Override
        public int getStartAddress() {
            return startAddress;
        }

        @Override
        public int getStopAddress() {
            return stopAddress;
        }

        @Override
        public int compareTo(AddressRange o) {
            if (o == this) {
                return 0;
            }
            if (startAddress < o.getStartAddress()) {
                return -1;
            } else if (startAddress > o.getStartAddress()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public MemoryContextImpl() {
        super();
        sizeSet = false;
        romList = new ArrayList<AddressRange>();
    }

    public boolean init(int size, int banks, int bankCommon, MemoryFrame gui) {
        this.gui = gui;
        if (sizeSet == true) {
            return false;
        }
        this.bankCommon = bankCommon;
        if (banks <= 0) {
            banks = 1;
        }
        this.banksCount = banks;
        mem = new short[size][banks];
        sizeSet = true;
        return true;
    }

    /**
     * Clears memory content.
     */
    @Override
    public void clear() {
        if (sizeSet == false) {
            return;
        }
        for (int i = 0; i < mem.length; i++) {
            for (int j = 0; j < banksCount; j++) {
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
        sizeSet = false;
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

    // this can parse classic old data
    @Override
    public boolean loadHex(String filename, int bank) {
        if (sizeSet == false) {
            return false;
        }
        lastStartSet = false;
        try {
            lastImageStart = HEXFileManager.loadIntoMemory(filename, this);
        } catch (java.io.FileNotFoundException ex) {
            StaticDialogs.showErrorMessage("File not found: " + filename);
            notifyMemoryChanged(-1);
            return false;
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Error opening file: " + filename);
            notifyMemoryChanged(-1);
            return false;
        }
        notifyMemoryChanged(-1);
        return true;
    }

    /**
     * Method loads a binary file into memory.
     *
     * @param filename Binary file name (has to be readable and has to exist)
     * @param address an address where the file should be loaded.
     * @return true if file was successfully loaded, false if not.
     */
    @Override
    public boolean loadBin(String filename, int address, int bank) {
        if (sizeSet == false) {
            return false;
        }
        lastImageStart = 0;
        lastStartSet = true;
        try {
            File f = new File(filename);
            if (f.isFile() == false) {
                throw new IOException("Specified file name doesn't point to a file");
            }
            RandomAccessFile vstup = new RandomAccessFile(f, "r");
            int i;
            long r = 0, l = vstup.length();
            while (r < l) {
                i = vstup.readUnsignedByte();
                mem[address++][bank] = (short) (i & 0xFF);
                l++;
            }
            vstup.close();
        } catch (EOFException ex) {
        } catch (java.io.FileNotFoundException ex) {
            StaticDialogs.showErrorMessage("File not found: " + filename);
            notifyMemoryChanged(-1);
            return false;
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Error opening file: " + filename);
            notifyMemoryChanged(-1);
            return false;
        }
        notifyMemoryChanged(-1);
        return true;
    }

    @Override
    public Short read(int from) {
        if (from < bankCommon) {
            return mem[from][bankSelect];
        } else {
            return mem[from][0];
        }
    }

    public Object read(int from, int bank) {
        if (from < bankCommon) {
            return mem[from][bank];
        } else {
            return mem[from][0];
        }
    }

    @Override
    public Object readWord(int from) {
        activeBank = (from < bankCommon) ? bankSelect : 0;
        if (from == mem.length - 1) {
            return mem[from][activeBank];
        }
        int low = mem[from][activeBank] & 0xFF;
        int high = mem[from + 1][activeBank];
        return (int) ((high << 8) | low);
    }

    @Override
    public void write(int to, Short val) {
        if (isROM(to) == true) {
            return;
        }
        activeBank = (to < bankCommon) ? bankSelect : 0;
        mem[to][activeBank] = (short) (val & 0xFF);
        notifyMemoryChanged(to);
    }

    public void write(int to, Object val, int bank) {
        if (isROM(to) == true) {
            return;
        }
        activeBank = (to < bankCommon) ? bank : 0;
        if (val instanceof Integer) {
            mem[to][activeBank] = (short) ((Integer) val & 0xFF);
        } else {
            mem[to][activeBank] = (short) ((Short) val & 0xFF);
        }
        notifyMemoryChanged(to);
    }

    @Override
    public void writeWord(int to, Object val) {
        if (isROM(to) == true) {
            return;
        }
        activeBank = (to < bankCommon) ? bankSelect : 0;
        short low = (short) ((Integer) val & 0xFF);
        mem[to][activeBank] = low;
        notifyMemoryChanged(to);
        if (to < mem.length - 1) {
            short high = (short) (((Integer) val >>> 8) & 0xFF);
            mem[to + 1][activeBank] = high;
            notifyMemoryChanged(to + 1);
        }
    }

    public int getSize() {
        if (sizeSet == false) {
            return 0;
        }
        return mem.length;
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

    // remove range from romBitmap
    @Override
    public void setRAM(AddressRange range) {
        if (sizeSet == false) {
            return;
        }
        if (range.getStartAddress() > range.getStopAddress()) {
            return;
        }
        removeROMRange(range);
    }

    @Override
    public void setROM(AddressRange range) {
        if (sizeSet == false) {
            return;
        }
        if (range.getStartAddress() > range.getStopAddress()) {
            return;
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

    // only for GUI purposes, this have nothing to do with memory emulation
    @Override
    public List<AddressRange> getROMRanges() {
        return romList;
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
