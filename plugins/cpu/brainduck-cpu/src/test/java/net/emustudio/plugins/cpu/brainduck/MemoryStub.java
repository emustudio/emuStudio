/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@PluginContext
public class MemoryStub implements ByteMemoryContext {
    protected Byte[][] memory = new Byte[1][1000];
    private int afterProgram;

    public MemoryStub() {
        clear();
    }

    void setProgram(byte[] program) {
        clear();
        for (afterProgram = 0; afterProgram < program.length; afterProgram++) {
            memory[0][afterProgram] = program[afterProgram];
        }
    }

    int getDataStart() {
        return afterProgram + 1;
    }

    void setData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            memory[0][afterProgram + 1 + i] = data[i];
        }
    }

    @Override
    public boolean isReadOnly(int address) {
        return false;
    }

    @Override
    public List<? extends AddressRange> getReadOnly() {
        return Collections.emptyList();
    }

    @Override
    public void setReadOnly(AddressRange range) {

    }

    @Override
    public void setReadWrite(AddressRange range) {

    }

    @Override
    public int getBanksCount() {
        return 0;
    }

    @Override
    public int getSelectedBank() {
        return 0;
    }

    @Override
    public void selectBank(int bankIndex) {

    }

    @Override
    public int getCommonBoundary() {
        return 0;
    }

    @Override
    public Byte[][] getRawMemory() {
        return memory;
    }

    @Override
    public void clear() {
        Arrays.fill(this.memory[0], (byte) 0);
    }

    @Override
    public void addMemoryListener(Memory.MemoryListener listener) {
    }

    @Override
    public void removeMemoryListener(Memory.MemoryListener listener) {
    }

    @Override
    public int getSize() {
        return this.memory.length;
    }

    @Override
    public boolean areMemoryNotificationsEnabled() {
        return false;
    }

    @Override
    public void setMemoryNotificationsEnabled(boolean enabled) {
    }

    @Override
    public Byte read(int memoryPosition) {
        return this.memory[0][memoryPosition];
    }

    @Override
    public Byte[] read(int memoryPosition, int count) {
        int to = Math.min(this.memory.length, memoryPosition + count);
        return Arrays.copyOfRange(this.memory[0], memoryPosition, to);
    }

    @Override
    public void write(int memoryPosition, Byte value) {
        this.memory[0][memoryPosition] = value;
    }

    @Override
    public void write(int memoryPosition, Byte[] cells, int count) {
        System.arraycopy(cells, 0, this.memory[0], memoryPosition, count);
    }

    @Override
    public Class<Byte> getDataType() {
        return null;
    }
}
