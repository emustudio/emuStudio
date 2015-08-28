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
}
