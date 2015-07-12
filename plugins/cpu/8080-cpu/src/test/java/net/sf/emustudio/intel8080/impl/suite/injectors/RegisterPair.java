package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;
import net.sf.emustudio.intel8080.impl.suite.CpuRunnerImpl;

public class RegisterPair implements RunnerInjector<Integer, CpuRunnerImpl> {
    private final int registerPair;

    public RegisterPair(int registerPair) {
        this.registerPair = registerPair;
    }

    @Override
    public void inject(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPair(registerPair, value & 0xFFFF);
    }
}
