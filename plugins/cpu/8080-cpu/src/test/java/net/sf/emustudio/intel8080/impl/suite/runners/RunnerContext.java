package net.sf.emustudio.intel8080.impl.suite.runners;

public class RunnerContext<T extends Number> {
    public final T first;
    public final T second;

    public final int flags;
    public final int PC;
    public final int SP;

    public RunnerContext(T first, T second, int flags, int PC, int SP) {
        this.first = first;
        this.second = second;
        this.flags = flags;
        this.PC = PC;
        this.SP = SP;
    }

    public RunnerContext(T first, T second, int flags) {
        this(first, second, flags, 0, 0);
    }
}
