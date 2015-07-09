package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

@FunctionalInterface
public interface RunnerInjector<OperandType extends Number> {

    void inject(CpuRunner cpuRunner, OperandType value);

}
