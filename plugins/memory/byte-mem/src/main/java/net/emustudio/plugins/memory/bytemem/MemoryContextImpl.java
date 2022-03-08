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
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@PluginContext(id = "Byte Memory")
public class MemoryContextImpl extends AbstractMemoryContext<Byte> implements ByteMemoryContext {
    final static int DEFAULT_MEM_SIZE = 65536;

    private final RangeTree romRanges = new RangeTree();
    private final Dialogs dialogs;

    public MemoryContextImpl(Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    int lastImageStart = 0;
    private Byte[][] mem = new Byte[1][0];
    private int banksCount;
    private int bankSelect = 0;
    private int bankCommon = 0;

    void init(int size, int banks, int bankCommon) {
        if (banks <= 0) {
            throw new IllegalArgumentException("Number of banks must be >= 1!");
        }

        this.bankCommon = bankCommon;
        this.banksCount = banks;
        mem = new Byte[banks][size];
    }

    @Override
    public void clear() {
        for (Byte[] bank : mem) {
            Arrays.fill(bank, (byte) 0);
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
    public int getSelectedBank() {
        return bankSelect;
    }

    @Override
    public void selectBank(int bankSelect) {
        if (bankSelect < banksCount) {
            this.bankSelect = bankSelect;
        }
    }

    @Override
    public int getCommonBoundary() {
        return bankCommon;
    }

    public void loadHex(Path hexFile, int bank) {
        int currentBank = bankSelect;
        try {
            bankSelect = (short) bank;
            lastImageStart = IntelHEX.loadIntoMemory(hexFile.toFile(), this, p -> p);
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
                mem[bank][address++] = (byte) (binaryFile.readUnsignedByte() & 0xFF);
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
    public Byte read(int from) {
        int activeBank = (from < bankCommon) ? bankSelect : 0;
        return mem[activeBank][from];
    }

    public Byte readBank(int from, int bank) {
        if (from < bankCommon) {
            return mem[bank][from];
        } else {
            return mem[0][from];
        }
    }

    @Override
    public Byte[] read(int from, int count) {
        return Arrays.copyOfRange(mem[bank(from)], from, from + count); // from+count can be >= memory.length
    }

    @Override
    public void write(int to, Byte value) {
        if (!isReadOnly(to)) {
            mem[bank(to)][to] = value;
            notifyMemoryChanged(to);
        }
    }

    public void writeBank(int to, byte val, int bank) {
        if (!isReadOnly(to)) {
            int activeBank = (to < bankCommon) ? bank : 0;
            mem[activeBank][to] = val;
            notifyMemoryChanged(to);
        }
    }

    public void write(int to, Byte[] values, int count) {
        if (!romRanges.intersects(to, to + count)) {
            System.arraycopy(values, 0, mem[bank(to)], to, count);
            for (int i = 0; i < values.length; i++) {
                notifyMemoryChanged(to + i);
            }
        }
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
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

    private int bank(int address) {
        return (address < bankCommon) ? bankSelect : 0;
    }
}
