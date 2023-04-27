/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@PluginContext(id = "Byte Memory")
public class MemoryContextImpl extends AbstractMemoryContext<Byte> implements ByteMemoryContext {
    final static int DEFAULT_MEM_SIZE = 65536;

    private final RangeTree romRanges = new RangeTree();
    private final MemoryContextAnnotations annotations;
    private Byte[][] mem = new Byte[1][0];
    private int banksCount;
    private int bankSelect = 0;
    private int bankCommon = 0;

    protected MemoryContextImpl(MemoryContextAnnotations annotations) {
        this.annotations = Objects.requireNonNull(annotations);
    }

    void init(int size, int banks, int bankCommon) {
        if (banks <= 0) {
            throw new IllegalArgumentException("Number of banks must be >= 1!");
        }

        this.bankCommon = bankCommon;
        this.banksCount = banks;
        mem = new Byte[banks][size];
        clear();
    }

    @Override
    public void clear() {
        for (Byte[] bank : mem) {
            Arrays.fill(bank, (byte) 0);
        }
        notifyMemoryContentChanged(-1);
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
        Byte[] memBank = mem[bank(from)];
        int to = Math.min(memBank.length, from + count);
        return Arrays.copyOfRange(mem[bank(from)], from, to);
    }

    @Override
    public void write(int to, Byte value) {
        if (!isReadOnly(to)) {
            mem[bank(to)][to] = value;
            notifyMemoryContentChanged(to);
        }
    }

    public void writeBank(int to, byte val, int bank) {
        if (!isReadOnly(to)) {
            int activeBank = (to < bankCommon) ? bank : 0;
            mem[activeBank][to] = val;
            notifyMemoryContentChanged(to);
        }
    }

    public void write(int to, Byte[] values, int count) {
        if (!romRanges.intersects(to, to + count)) {
            System.arraycopy(values, 0, mem[bank(to)], to, count);
            notifyMemoryContentChanged(to, to + values.length);
        }
    }

    @Override
    public Class<Byte> getCellTypeClass() {
        return Byte.class;
    }

    @Override
    public int getSize() {
        return mem[0].length;
    }

    @Override
    public MemoryContextAnnotations annotations() {
        return annotations;
    }

    @Override
    public void setReadWrite(AddressRange range) {
        if (range.getStartAddress() > range.getStopAddress()) {
            throw new IllegalArgumentException("Range stop address must be > than start address!");
        }
        removeROMRange(range);
    }

    @Override
    public boolean isReadOnly(int address) {
        return romRanges.isIn(address);
    }

    @Override
    public List<? extends AddressRange> getReadOnly() {
        return romRanges.getRanges();
    }

    @Override
    public void setReadOnly(AddressRange range) {
        if (range.getStartAddress() > range.getStopAddress()) {
            throw new IllegalArgumentException("Range stop address must be > than start address!");
        }
        addRomRange(range);
    }

    @Override
    public Byte[][] getRawMemory() {
        return mem;
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
