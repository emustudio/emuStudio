package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class MemoryExpand implements RunnerInjector<Integer> {

    @Override
    public void inject(CpuRunner cpuRunner, Integer address) {
        cpuRunner.ensureProgramSize(address + 4);
    }
}