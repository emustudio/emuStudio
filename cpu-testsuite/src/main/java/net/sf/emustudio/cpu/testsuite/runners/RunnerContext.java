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
package net.sf.emustudio.cpu.testsuite.runners;

import net.jcip.annotations.Immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Immutable
public class RunnerContext<T extends Number> {
    public final T first;
    public final T second;

    public final int flags;
    public final int PC;
    public final int SP;
    public final List<Integer> registers;

    public RunnerContext(T first, T second, int flags, int PC, int SP, List<Integer> registers) {
        this.first = first;
        this.second = second;
        this.flags = flags;
        this.PC = PC;
        this.SP = SP;

        this.registers = Collections.unmodifiableList(new ArrayList<>(registers));
    }

    public RunnerContext(T first, T second, int flags) {
        this(first, second, flags, 0, 0, Collections.emptyList());
    }

    public RunnerContext<T> switchFirstAndSecond() {
        return new RunnerContext<T>(second, first, flags, PC, SP, registers);
    }

    public int getRegister(int register) {
        return registers.get(register);
    }

    @Override
    public String toString() {
        return "RunnerContext{" +
            "first=" + first +
            ", second=" + second +
            ", flags=" + flags +
            ", PC=" + PC +
            ", SP=" + SP +
            ", registers=" + registers +
            '}';
    }
}
