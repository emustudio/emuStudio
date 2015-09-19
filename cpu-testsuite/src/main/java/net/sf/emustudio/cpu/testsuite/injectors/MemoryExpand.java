package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

public class MemoryExpand<CpuRunnerType extends CpuRunner> implements SingleOperandInjector<Integer, CpuRunnerType> {

    @Override
    public void inject(CpuRunner cpuRunner, Integer address) {
        cpuRunner.ensureProgramSize(address + 4);
    }

    @Override
    public String toString() {
        return String.format("memoryExpander");
    }
}