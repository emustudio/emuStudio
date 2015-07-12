package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;

public class InstructionOperand<K extends Number, CpuRunnerType extends CpuRunner>
        implements RunnerInjector<K, CpuRunnerType> {
    private final int opcode;

    public InstructionOperand(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public void inject(CpuRunnerType cpuRunner, K operand) {
        int tmpOperand = operand.intValue() & 0xFFFF;
        if (operand instanceof Byte) {
            tmpOperand &= 0xFF;
            cpuRunner.setProgram(opcode, tmpOperand);
        } else {
            cpuRunner.setProgram(opcode, tmpOperand & 0xFF, (tmpOperand >>> 8) & 0xFF);
        }
        cpuRunner.ensureProgramSize(tmpOperand + 2);
    }
}
