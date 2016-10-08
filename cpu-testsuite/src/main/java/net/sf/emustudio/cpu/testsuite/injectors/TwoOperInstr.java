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
import net.sf.emustudio.cpu.testsuite.injectors.internal.DefaultProgramGenerator;

/**
 * Instruction with two operands.
 *
 * It is used as an injector for the test runner.
 *
 * The order of bytes is as follows:
 *
 * 1. Initial opcodes (1 or more)
 * 2. First Operand
 * 3. Second Operand
 * 4. Possibly more opcodes (0 or more)
 *
 * @param <T> type of CpuRunner
 * @param <OperandT> type of the operand (Byte or Integer)
 */
public class TwoOperInstr<T extends CpuRunner, OperandT extends Number> implements TwoOperInjector<T, OperandT> {
    private final DefaultProgramGenerator strategy = new DefaultProgramGenerator();

    /**
     * Create Instruction with two opcodes injector.
     *
     * @param opcodes 1 or more opcode(s) of the instruction. Each opcode must be a byte (don't get confused by int).
     */
    public TwoOperInstr(int... opcodes) {
        strategy.addOpcodes(opcodes);
    }

    /**
     * Will place more opcodes after the instruction operands.
     *
     * NOTE: size of operands is given by OperandType parameter (Byte = 8 bits, Integer = 16 bits)
     *
     * @param opcodes opcode(s). Each opcode must be a byte (don't get confused by int).
     * @return this
     */
    public TwoOperInstr placeOpcodesAfterOperands(int... opcodes) {
        strategy.addOpcodesAfterOperands(opcodes);
        return this;
    }

    @Override
    public void inject(T cpuRunner, OperandT first, OperandT second) {
        strategy.setOperands(first, second);

        int tmpFirst = first.intValue() & 0xFFFF;
        int tmpSecond = second.intValue() & 0xFFFF;

        cpuRunner.setProgram(strategy.generate());
        cpuRunner.ensureProgramSize(Math.max(tmpFirst + 2, tmpSecond + 2));

        strategy.clearOperands();
    }

    @Override
    public String toString() {
        return strategy.toString();
    }

}
