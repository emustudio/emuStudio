package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.TwoOperandsInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstructionTwoOperands<K extends Number, CpuRunnerType extends CpuRunner>
    implements TwoOperandsInjector<K, CpuRunnerType> {
    private final List<Integer> opcodes;
    private final List<Integer> opcodesAfterOperand = new ArrayList<>();

    public InstructionTwoOperands(int... opcodes) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        this.opcodes = Collections.unmodifiableList(tmpList);
    }

    public InstructionTwoOperands placeOpcodesAfterOperands(int... opcodes) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        opcodesAfterOperand.addAll(tmpList);
        return this;
    }

    @Override
    public void inject(CpuRunnerType cpuRunner, K first, K second) {
        int tmpFirst = first.intValue() & 0xFFFF;
        int tmpSecond = second.intValue() & 0xFFFF;

        List<Integer> program = new ArrayList<>(opcodes);
        if (first instanceof Byte) {
            program.add(tmpFirst & 0xFF);
            program.add(tmpSecond & 0xFF);
        } else {
            program.add(tmpFirst & 0xFF);
            program.add((tmpFirst >>> 8) & 0xFF);
            program.add(tmpSecond & 0xFF);
            program.add((tmpSecond >>> 8) & 0xFF);
        }
        program.addAll(opcodesAfterOperand);
        cpuRunner.setProgram(program);
        cpuRunner.ensureProgramSize(tmpFirst + 2);
        cpuRunner.ensureProgramSize(tmpSecond + 2);
    }

    @Override
    public String toString() {
        return String.format(
                "instruction: %s",
                Utils.toHexString(opcodes.toArray()) + " (operand) (operand) " + Utils.toHexString(opcodesAfterOperand.toArray())
        );
    }


}
