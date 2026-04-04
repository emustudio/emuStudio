/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
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

    public void setProgram(byte[] program) {
        clear();
        for (afterProgram = 0; afterProgram < program.length; afterProgram++) {
            memory[0][afterProgram] = program[afterProgram];
        }
    }

    public int getDataStart() {
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
    public void addMemoryListener(MemoryListener listener) {
    }

    @Override
    public void removeMemoryListener(MemoryListener listener) {
    }

    @Override
    public int getSize() {
        return this.memory[0].length;
    }

    @Override
    public boolean areMemoryNotificationsEnabled() {
        return false;
    }

    @Override
    public MemoryContextAnnotations annotations() {
        return null;
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
        int to = Math.min(this.memory[0].length, memoryPosition + count);
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
    public Class<Byte> getCellTypeClass() {
        return null;
    }
}
