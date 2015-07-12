package net.sf.emustudio.zilogZ80.impl.suite.injectors;

import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;
import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;

public class Register implements RunnerInjector<Byte, CpuRunnerImpl> {
    private final int register;

    public Register(int register) {
        this.register = register;
    }

    @Override
    public void inject(CpuRunnerImpl cpuRunner, Byte value) {
        cpuRunner.setRegister(register, value);
    }
}
