package net.sf.emustudio.intel8080.impl.suite.injectors;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;

public class InstructionByteOperand implements RunnerInjector<Byte> {
    private final int opcode;

    public InstructionByteOperand(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Byte operand) {
        cpuRunner.setProgram(opcode, operand & 0xFF);
    }
}
