package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class InstructionNoOperands<OperandType extends Number> implements RunnerInjector<OperandType> {
    private final int instruction;

    public InstructionNoOperands(int instruction) {
        this.instruction = instruction;
    }

    @Override
    public void inject(CpuRunner cpuRunner, OperandType first) {
        cpuRunner.setProgram(instruction);
    }
}
