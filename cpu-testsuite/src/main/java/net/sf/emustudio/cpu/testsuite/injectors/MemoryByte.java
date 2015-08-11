package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;

public class MemoryByte<TCpuRunnerType extends CpuRunner> implements RunnerInjector<Byte, TCpuRunnerType> {
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
}
