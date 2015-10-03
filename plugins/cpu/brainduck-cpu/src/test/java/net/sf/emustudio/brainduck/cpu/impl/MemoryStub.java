package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.ContextType;
import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;

@ContextType
public class MemoryStub implements MemoryContext<Short> {
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
        return memory[from];
    }

    @Override
    public Short[] readWord(int from) {
        if (from == memory.length - 1) {
            return new Short[] { memory[from] };
        }
        return new Short[] { memory[from], memory[from + 1] };
    }

    @Override
    public void write(int to, Short val) {
        memory[to] = (short)(val & 0xFF);
    }

    @Override
    public void writeWord(int to, Short[] cells) {
        memory[to] = cells[0];
        if (to < memory.length - 1) {
            memory[to + 1] = cells[1];
        }
    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
