package net.sf.emustudio.intel8080.impl;

import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;

public class MemoryStub implements MemoryContext<Short> {
    private short[] memory;

    public void setMemory(short[] memory) {
        this.memory = memory;
    }

    @Override
    public Short read(int memoryPosition) {
        return memory[memoryPosition];
    }

    @Override
    public Object readWord(int memoryPosition) {
        int low = memory[memoryPosition] & 0xFF;
        int high = memory[memoryPosition + 1];
        return (high << 8) | low;
    }

    @Override
    public void write(int memoryPosition, Short value) {
        memory[memoryPosition] = value;
    }

    @Override
    public void writeWord(int memoryPosition, Object value) {
        short low = (short) ((Integer) value & 0xFF);
        memory[memoryPosition] = low;
        short high = (short) (((Integer) value >>> 8) & 0xFF);
        memory[memoryPosition + 1] = high;
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
}
