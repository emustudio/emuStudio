package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;
import net.sf.emustudio.intel8080.impl.suite.CpuRunnerImpl;

public class Register implements SingleOperandInjector<Byte, CpuRunnerImpl> {
    private final int register;

    public Register(int register) {
        this.register = register;
    }

    @Override
    public void inject(CpuRunnerImpl cpuRunner, Byte value) {
        cpuRunner.setRegister(register, value);
    }
}
