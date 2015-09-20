package net.sf.emustudio.zilogZ80.impl.suite.injectors;

import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;
import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;

public class RegisterPair2 implements SingleOperandInjector<Integer, CpuRunnerImpl> {
    private final int registerPair;

    public RegisterPair2(int registerPair) {
        this.registerPair = registerPair;
    }

    @Override
    public void inject(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPair2(registerPair, value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("registerPair2[%04x]", registerPair);
    }

}
