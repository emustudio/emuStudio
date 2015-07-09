package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

import java.util.Objects;
import java.util.function.Function;

public class Register implements RunnerInjector<Byte> {
    private final int register;

    public Register(int register) {
        this.register = register;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Byte value) {
        cpuRunner.setRegister(register, value);
    }
}
