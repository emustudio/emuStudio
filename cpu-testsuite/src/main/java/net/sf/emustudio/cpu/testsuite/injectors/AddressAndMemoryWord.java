package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.Runner;

public class AddressAndMemoryWord implements Runner.BothRunnerInjector<Integer, CpuRunner> {

    @Override
    public void inject(CpuRunner cpuRunner, Integer first, Integer second) {
        cpuRunner.ensureProgramSize(first + 4);
        cpuRunner.setByte(first, second & 0xFF);
        cpuRunner.setByte(first + 1, (second >>> 8) & 0xFF);
    }
}
