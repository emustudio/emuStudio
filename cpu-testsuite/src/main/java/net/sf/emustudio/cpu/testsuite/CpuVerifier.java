package net.sf.emustudio.cpu.testsuite;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public abstract class CpuVerifier {
    protected final MemoryStub memoryStub;

    public CpuVerifier(MemoryStub memoryStub) {
        this.memoryStub = Objects.requireNonNull(memoryStub);
    }

    public void checkMemoryByte(int address, int value) {
        value &= 0xFF;
        assertEquals(
                String.format("Expected mem[%04x]=%02x, but was %02x", address, value, memoryStub.read(address)),
                value, memoryStub.read(address).intValue()
        );
    }

    public void checkMemoryWord(int address, int value) {
        assertEquals(
                String.format("Expected word mem[%04x]=%04x, but was %04x", address, value, memoryStub.readWord(address)),
                value, memoryStub.readWord(address).intValue()
        );
    }

    public abstract void checkFlags(int mask);

    public abstract void checkNotFlags(int mask);
}
