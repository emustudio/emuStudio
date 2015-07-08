package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class RegisterPairPSW implements RunnerInjector<Integer> {
    private final int registerPairPSW;

    public RegisterPairPSW(int registerPairPSW) {
        this.registerPairPSW = registerPairPSW;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Integer value) {
        cpuRunner.setRegisterPairPSW(registerPairPSW, value & 0xFFFF);
    }
}
