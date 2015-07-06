package net.sf.emustudio.intel8080.impl.suite.runners;

public class RunnerContext<T> {
    public final T first;
    public final T second;
    public final int result;

    public final int flagsBefore;
    public final int PCbefore;
    public final int SPbefore;

    public RunnerContext(T first, T second, int result, int flagsBefore, int PCbefore, int SPbefore) {
        this.first = first;
        this.second = second;
        this.result = result;
        this.flagsBefore = flagsBefore;
        this.PCbefore = PCbefore;
        this.SPbefore = SPbefore;
    }

    public RunnerContext(T first, T second, int result, int flagsBefore) {
        this(first, second, result, flagsBefore, 0, 0);
    }
}
