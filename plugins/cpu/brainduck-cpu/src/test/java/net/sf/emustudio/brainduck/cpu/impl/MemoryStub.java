package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.ContextType;
import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;

@ContextType
public class MemoryStub implements MemoryContext<Short> {
    private final byte[] memory;
    private int afterProgram;

    public MemoryStub(int size) {
        this.memory = new byte[size];
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
    public Object readWord(int from) {
        if (from == memory.length - 1) {
            return memory[from];
        }
        int low = memory[from] & 0xFF;
        int high = memory[from + 1];
        return (int) ((high << 8) | low);
    }

    @Override
    public void write(int to, Short val) {
        memory[to] = (byte) (val & 0xFF);
    }

    @Override
    public void writeWord(int to, Object val) {
        byte low = (byte) ((Integer) val & 0xFF);
        memory[to] = low;
        if (to < memory.length - 1) {
            byte high = (byte) (((Integer) val >>> 8) & 0xFF);
            memory[to + 1] = high;
        }
    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
