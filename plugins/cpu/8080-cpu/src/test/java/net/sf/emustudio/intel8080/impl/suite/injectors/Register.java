package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunnerImpl;

import java.util.function.BiConsumer;

public class Register implements BiConsumer<CpuRunnerImpl, Byte> {
    private final int register;

    public Register(int register) {
        this.register = register;
    }

    @Override
    public void accept(CpuRunnerImpl cpuRunner, Byte value) {
        cpuRunner.setRegister(register, value);
    }
}
