/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.cpu.testsuite.memory;

import emulib.plugins.memory.Memory;

public class ShortMemoryStub implements MemoryStub<Short> {
    private final int wordReadingStrategy;

    protected short[] memory = new short[1000];
    private int wordCellsCount = 2;

    public ShortMemoryStub(int wordReadingStrategy) {
        this.wordReadingStrategy = wordReadingStrategy;
    }

    @Override
    public void setMemory(short[] memory) {
        this.memory = memory;
    }

    @Override
    public Short read(int memoryPosition) {
        return memory[memoryPosition];
    }

    @Override
    public Short[] readWord(int memoryPosition) {
        Short[] word = new Short[wordCellsCount];
        for (int i = 0; i < wordCellsCount; i++) {
            word[i] = memory[memoryPosition + i];
        }

        return word;
    }

    @Override
    public void write(int memoryPosition, Short value) {
        memory[memoryPosition] = value;
    }

    @Override
    public void writeWord(int memoryPosition, Short[] cells) {
        for (int i = 0; i < wordCellsCount; i++) {
            memory[memoryPosition + i] = cells[i];
        }
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }

    @Override
    public void clear() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    @Override
    public void addMemoryListener(Memory.MemoryListener listener) {

    }

    @Override
    public void removeMemoryListener(Memory.MemoryListener listener) {

    }

    @Override
    public int getSize() {
        return memory.length;
    }

    @Override
    public boolean areMemoryNotificationsEnabled() {
        return false;
    }

    @Override
    public void setMemoryNotificationsEnabled(boolean enabled) {

    }

    @Override
    public void setWordCellsCount(int count) {
        this.wordCellsCount = count;
    }

    @Override
    public int getWordReadingStrategy() {
        return wordReadingStrategy;
    }
}
