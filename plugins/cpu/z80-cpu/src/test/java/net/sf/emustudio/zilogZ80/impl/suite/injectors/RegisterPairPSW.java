package net.sf.emustudio.zilogZ80.impl.suite.injectors;

import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;
import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;

public class RegisterPairPSW implements SingleOperandInjector<Integer, CpuRunnerImpl> {
    private final int registerPairPSW;

    public RegisterPairPSW(int registerPairPSW) {
        this.registerPairPSW = registerPairPSW;
    }

    @Override
    public void inject(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPairPSW(registerPairPSW, value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("registerPairPSW[%x]", registerPairPSW);
    }

}
