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

import java.util.function.Consumer;

/**
 * Instruction without operands.
 *
 * It is used as an injector for the test runner.
 *
 * Can have 1 or more opcodes.
 */
public class NoOperInstr<T extends CpuRunner> implements Consumer<T> {
    private final DefaultProgramGenerator strategy = new DefaultProgramGenerator();

    /**
     * Creates instruction with no operands injector.
     *
     * @param opcodes 1 or more opcode(s) of the instruction. Each opcode must be a byte (don't get confused by int).
     */
    public NoOperInstr(int... opcodes) {
        strategy.addOpcodes(opcodes);
    }

    @Override
    public void accept(T cpuRunner) {
        cpuRunner.setProgram(strategy.generate());
        strategy.clearOperands();
    }

    @Override
    public String toString() {
        return strategy.toString();
    }

}
