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
import net.sf.emustudio.cpu.testsuite.injectors.internal.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Instruction with single operand.
 *
 * It is used as an injector for the test runner.
 *
 * The order of bytes is as follows:
 *
 * 1. Initial opcodes (1 or more)
 * 2. Operand
 * 3. Possibly more opcodes (0 or more)
 *
 * @param <OperandType> type of operand (Byte or Integer)
 */
public class InstructionSingleOperand<T extends CpuRunner, OperandType extends Number>
        implements BiConsumer<T, OperandType> {
    private final List<Integer> opcodes;
    private final List<Integer> opcodesAfterOperand = new ArrayList<>();

    /**
     * Create instruction with single operand injector.
     *
     * @param opcodes 1 or more opcode(s) of the instruction. Each opcode must be a byte (don't get confused by int).
     */
    public InstructionSingleOperand(int... opcodes) {
        if (opcodes.length <= 0) {
            throw new IndexOutOfBoundsException("Expected 1 or more opcodes");
        }

        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        this.opcodes = Collections.unmodifiableList(tmpList);
    }

    /**
     * Inserts opcodes after operand.
     *
     * NOTE: size of operands is given by OperandType parameter (Byte = 8 bits, Integer = 16 bits)
     *
     * @param opcodes opcode(s). Each opcode must be a byte (don't get confused by int).
     * @return
     */
    public InstructionSingleOperand placeOpcodesAfterOperand(int... opcodes) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : opcodes) {
            tmpList.add(opcode);
        }
        opcodesAfterOperand.addAll(tmpList);
        return this;
    }

    @Override
    public void accept(T cpuRunner, OperandType operand) {
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
