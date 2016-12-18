package net.sf.emustudio.cpu.testsuite.memory;

import emulib.plugins.memory.MemoryContext;

public interface MemoryStub<T extends Number> extends MemoryContext<T> {

    void setWordCellsCount(int count);

    void setMemory(short[] memory);

    int getWordReadingStrategy();

    default Byte[] numbersToBytes(Number[] read) {
        Byte[] word = new Byte[read.length];
        for (int i = 0; i < read.length; i++) {
            word[i] = read[i].byteValue();
        }
        return word;
    }

    default byte[] numbersToNativeBytes(Number[] read) {
        byte[] word = new byte[read.length];
        for (int i = 0; i < read.length; i++) {
            word[i] = read[i].byteValue();
        }
        return word;
    }

    default Byte[] shortsToBytes(Short[] read) {
        Byte[] word = new Byte[read.length];
        for (int i = 0; i < read.length; i++) {
            word[i] = read[i].byteValue();
        }
        return word;
    }

    default byte[] shortsToNativeBytes(Short[] read) {
        byte[] word = new byte[read.length];
        for (int i = 0; i < read.length; i++) {
            word[i] = read[i].byteValue();
        }
        return word;
    }

    default byte[] nativeShortsToNativeBytes(short[] read) {
        byte[] word = new byte[read.length];
        for (int i = 0; i < read.length; i++) {
            word[i] = (byte)(read[i] & 0xFF);
        }
        return word;
    }
}
