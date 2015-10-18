package net.sf.emustudio.intel8080.assembler.impl;

import emulib.plugins.memory.AbstractMemoryContext;

public class MemoryStub extends AbstractMemoryContext<Short> {
    private final short[] memory = new short[1000];

    @Override
    public Short read(int memoryPosition) {
        return memory[memoryPosition];
    }

    @Override
    public Short[] readWord(int memoryPosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int memoryPosition, Short value) {
        memory[memoryPosition] = value;
    }

    @Override
    public void writeWord(int memoryPosition, Short[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }

    @Override
    public void clear() {

    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
