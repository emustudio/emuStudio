package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.BinaryRunner;

public class AddressAndMemoryWord implements BinaryRunner.BothRunnerInjector<Integer> {

    @Override
    public void inject(CpuRunner cpuRunner, Integer first, Integer second) {
        cpuRunner.ensureProgramSize(first + 4);
        cpuRunner.setByte(first, second & 0xFF);
        cpuRunner.setByte(first + 1, (second >>> 8) & 0xFF);
    }
}
