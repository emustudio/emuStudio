package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstructionNoOperands<OperandType extends Number, CpuRunnerType extends CpuRunner>
        implements SingleOperandInjector<OperandType, CpuRunnerType> {
    private final List<Integer> opcodes;

    public InstructionNoOperands(int... instruction) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : instruction) {
            tmpList.add(opcode);
        }
        this.opcodes = Collections.unmodifiableList(tmpList);
    }

    @Override
    public void inject(CpuRunnerType cpuRunner, OperandType unused) {
        cpuRunner.setProgram(opcodes);
    }

    @Override
    public String toString() {
        return String.format("instruction: %s", Utils.toHexString(opcodes.toArray()));
    }

}
