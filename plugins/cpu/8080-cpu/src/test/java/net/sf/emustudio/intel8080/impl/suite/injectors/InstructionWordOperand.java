package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class InstructionWordOperand implements RunnerInjector<Integer> {
    private final int opcode;

    public InstructionWordOperand(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Integer operand) {
        cpuRunner.ensureProgramSize(operand + 2);
        cpuRunner.setProgram(opcode, operand & 0xFF, (operand >>> 8) & 0xFF);
    }
}
