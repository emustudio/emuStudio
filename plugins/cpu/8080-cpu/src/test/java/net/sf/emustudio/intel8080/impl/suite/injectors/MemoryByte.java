package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class MemoryByte implements RunnerInjector<Byte> {
    private final int address;

    public MemoryByte(int address) {
        if (address <= 0) {
            throw new IllegalArgumentException("Address can be only > 0!");
        }

        this.address = address;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Byte value) {
        cpuRunner.setByte(address, value);
    }
}
