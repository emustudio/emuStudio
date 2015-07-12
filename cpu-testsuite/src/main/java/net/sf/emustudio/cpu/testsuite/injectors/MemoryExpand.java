package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;

public class MemoryExpand<CpuRunnerType extends CpuRunner> implements RunnerInjector<Integer, CpuRunnerType> {

    @Override
    public void inject(CpuRunner cpuRunner, Integer address) {
        cpuRunner.ensureProgramSize(address + 4);
    }
}