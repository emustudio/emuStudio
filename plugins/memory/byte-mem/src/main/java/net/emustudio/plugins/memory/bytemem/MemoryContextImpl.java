/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@PluginContext(id = "Byte Memory")
public class MemoryContextImpl extends AbstractMemoryContext<Short> implements ByteMemoryContext {
    final static int DEFAULT_MEM_SIZE = 65536;

    private final RangeTree romRanges = new RangeTree();
    private final Dialogs dialogs;

    public MemoryContextImpl(Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    int lastImageStart = 0;
    private short[][] mem = new short[1][0];
    private int banksCount;
    private short bankSelect = 0;
    private int bankCommon = 0;

    void init(int size, int banks, int bankCommon) {
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
            Arrays.fill(mem1, (short) 0);
        }
        lastImageStart = 0;
        notifyMemoryChanged(-1);
    }

    void destroy() {
        clear();
        mem = null;
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

    public void loadHex(Path hexFile, int bank) {
        short currentBank = bankSelect;
        try {
            bankSelect = (short)bank;
            lastImageStart = IntelHEX.loadIntoMemory(hexFile.toFile(), this);
        } catch (FileNotFoundException ex) {
            dialogs.showError("File not found: " + hexFile);
        } catch (Exception e) {
            dialogs.showError("Error opening file: " + hexFile);
        } finally {
            bankSelect = currentBank;
            notifyMemoryChanged(-1);
        }
    }

    public void loadBin(Path binFile, int address, int bank) {
        lastImageStart = 0;
        try (RandomAccessFile binaryFile = new RandomAccessFile(binFile.toFile(), "r")) {
            long position = 0, length = binaryFile.length();
            while (position < length) {
                mem[bank][address++] = (short) (binaryFile.readUnsignedByte() & 0xFF);
                position++;
            }
        } catch (EOFException ignored) {
            // ignored intentionally
        } catch (FileNotFoundException ex) {
            dialogs.showError("File not found: " + binFile);
        } catch (Exception e) {
            dialogs.showError("Error opening file: " + binFile);
        } finally {
            notifyMemoryChanged(-1);
        }
    }

    @Override
    public Short read(int from) {
        int activeBank = (from < bankCommon) ? bankSelect : 0;
        return mem[activeBank][from];
    }

    public Short read(int from, int bank) {
        if (from < bankCommon) {
            return mem[bank][from];
        } else {
            return mem[0][from];
        }
    }

    @Override
    public Short[] readWord(int from) {
        int activeBank = (from < bankCommon) ? bankSelect : 0;
        return new Short[]{mem[activeBank][from], mem[activeBank][(from + 1) & 0xFFFF]};
    }

    @Override
    public void write(int to, Short val) {
        if (!isReadOnly(to)) {
            int activeBank = (to < bankCommon) ? bankSelect : 0;
            mem[activeBank][to] = (short) (val & 0xFF);
            notifyMemoryChanged(to);
        }
    }

    public void write(int to, short val, int bank) {
        if (!isReadOnly(to)) {
            int activeBank = (to < bankCommon) ? bank : 0;
            mem[activeBank][to] = (short) (val & 0xFF);
            notifyMemoryChanged(to);
        }
    }

    @Override
    public void writeWord(int to, Short[] cells) {
        if (isReadOnly(to)) {
            return;
        }
        int activeBank = (to < bankCommon) ? bankSelect : 0;
        mem[activeBank][to] = (short) (cells[0] & 0xFF);

        if (isReadOnly(to + 1)) {
            return;
        }

        mem[activeBank][to + 1] = (short) (cells[1] & 0xFF);
        notifyMemoryChanged(to);
        notifyMemoryChanged(to + 1);
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    @Override
    public int getSize() {
        return mem[0].length;
    }

    @Override
    public void setReadWrite(AddressRange range) {
        if (range.getStartAddress() > range.getStopAddress()) {
            throw new IllegalArgumentException("Range stop address must be > than start address!");
        }
        removeROMRange(range);
    }

    @Override
    public void setReadOnly(AddressRange range) {
        if (range.getStartAddress() > range.getStopAddress()) {
            throw new IllegalArgumentException("Range stop address must be > than start address!");
        }
        addRomRange(range);
    }

    @Override
    public boolean isReadOnly(int address) {
        return romRanges.isIn(address);
    }

    @Override
    public List<? extends AddressRange> getReadOnly() {
        return romRanges.getRanges();
    }

    private void removeROMRange(AddressRange rangeToRemove) {
        romRanges.remove(rangeToRemove.getStartAddress(), rangeToRemove.getStopAddress());
    }

    private void addRomRange(AddressRange range) {
        romRanges.add(range.getStartAddress(), range.getStopAddress());
    }
}
