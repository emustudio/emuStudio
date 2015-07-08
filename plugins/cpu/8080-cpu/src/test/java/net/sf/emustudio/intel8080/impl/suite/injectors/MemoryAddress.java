package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class MemoryAddress implements RunnerInjector<Integer> {
    private final int value;
    private final boolean word;

    public MemoryAddress(Byte value) {
        this.value = value & 0xFF;
        word = false;
    }

    public MemoryAddress(Integer value) {
        this.value = value & 0xFFFF;
        word = true;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Integer address) {
        cpuRunner.setByte(address, value & 0xFF);
        if (word) {
            cpuRunner.setByte(address + 1, (value >>> 8) & 0xFF);
        }
    }
}
