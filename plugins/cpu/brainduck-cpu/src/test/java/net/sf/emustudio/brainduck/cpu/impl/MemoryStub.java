package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.ContextType;
import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;

@ContextType
public class MemoryStub implements MemoryContext<Short, Integer> {
    private final short[] memory;
    private int afterProgram;

    public MemoryStub(int size) {
        this.memory = new short[size];
    }

    public void setProgram(byte[] program) {
        clear();
        for (afterProgram = 0; afterProgram < program.length; afterProgram++) {
            memory[afterProgram] = program[afterProgram];
        }
    }

    public int getDataStart() {
        return afterProgram + 1;
    }

    public void setData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            memory[afterProgram + 1 + i] = data[i];
        }
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
    public Class<?> getDataType() {
        return Short.class;
    }

    @Override
    public Short read(int from) {
        return (short)memory[from];
    }

    @Override
    public Integer readWord(int from) {
        if (from == memory.length - 1) {
            return (int)memory[from];
        }
        int low = memory[from] & 0xFF;
        int high = memory[from + 1];
        return (high << 8) | low;
    }

    @Override
    public void write(int to, Short val) {
        memory[to] = (short)(val & 0xFF);
    }

    @Override
    public void writeWord(int to, Integer val) {
        short low = (byte) (val & 0xFF);
        memory[to] = low;
        if (to < memory.length - 1) {
            short high = (byte) ((val >>> 8) & 0xFF);
            memory[to + 1] = high;
        }
    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
