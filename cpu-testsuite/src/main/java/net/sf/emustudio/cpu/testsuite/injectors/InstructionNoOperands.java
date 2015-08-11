package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;

public class InstructionNoOperands<OperandType extends Number, TCpuRunnerType extends CpuRunner>
        implements RunnerInjector<OperandType, TCpuRunnerType> {
    private final int[] instruction;

    public InstructionNoOperands(int... instruction) {
        this.instruction = instruction;
    }

    @Override
    public void inject(TCpuRunnerType cpuRunner, OperandType first) {
        cpuRunner.setProgram(instruction);
    }
}
