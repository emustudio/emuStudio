package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class MemoryWord implements RunnerInjector<Integer> {
    private final int address;

    public MemoryWord(int address) {
        if (address <= 0) {
            throw new IllegalArgumentException("Address can be only > 0!");
        }

        this.address = address;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Integer value) {
        cpuRunner.setByte(address, value & 0xFF);
        cpuRunner.setByte(address + 1, (value >>> 8) & 0xFF);
    }
}
