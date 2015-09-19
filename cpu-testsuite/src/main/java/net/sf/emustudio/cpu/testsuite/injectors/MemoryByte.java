package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

public class MemoryByte<TCpuRunnerType extends CpuRunner> implements SingleOperandInjector<Byte, TCpuRunnerType> {
    private final int address;

    public MemoryByte(int address) {
        if (address <= 0) {
            throw new IllegalArgumentException("Address can be only > 0! (was " + address + ")");
        }

        this.address = address;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Byte value) {
        cpuRunner.setByte(address, value);
    }

    @Override
    public String toString() {
        return String.format("memoryByte[%04x]", address);
    }

}
