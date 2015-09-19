package net.sf.emustudio.cpu.testsuite.runners;

import net.sf.emustudio.cpu.testsuite.CpuRunner;

@FunctionalInterface
public interface TwoOperandsInjector<OperandType extends Number, CpuRunnerType extends CpuRunner> {
    void inject(CpuRunnerType cpuRunner, OperandType first, OperandType second);
}
