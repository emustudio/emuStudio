/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InstructionSingleOperand<K extends Number, CpuRunnerType extends CpuRunner>
        implements SingleOperandInjector<K, CpuRunnerType> {
    private final List<Integer> opcodes;
    private final List<Integer> opcodesAfterOperand = new ArrayList<>();

    public InstructionSingleOperand(int... opcodes) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        this.opcodes = Collections.unmodifiableList(tmpList);
    }

    public InstructionSingleOperand placeOpcodesAfterOperand(int... opcodes) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        opcodesAfterOperand.addAll(tmpList);
        return this;
    }

    @Override
    public void inject(CpuRunnerType cpuRunner, K operand) {
        int tmpOperand = operand.intValue() & 0xFFFF;
        List<Integer> program = new ArrayList<>(opcodes);
        if (operand instanceof Byte) {
            program.add(tmpOperand & 0xFF);
        } else {
            program.add(tmpOperand & 0xFF);
            program.add((tmpOperand >>> 8) & 0xFF);
        }
        program.addAll(opcodesAfterOperand);
        cpuRunner.setProgram(program);
        cpuRunner.ensureProgramSize(tmpOperand + 2);
    }

    @Override
    public String toString() {
        return String.format(
                "instruction: %s",
                Utils.toHexString(opcodes.toArray()) + " (operand) " + Utils.toHexString(opcodesAfterOperand.toArray())
        );
    }

}
