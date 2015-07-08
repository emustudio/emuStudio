package net.sf.emustudio.intel8080.impl.suite.runners;

import net.jcip.annotations.NotThreadSafe;
import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@NotThreadSafe
public class UnaryRunner<OperandType extends Number> implements Function<OperandType, RunnerContext<OperandType>> {
    private final CpuRunner cpuRunner;
    private final List<RunnerInjector<OperandType>> injectors = new ArrayList<>();
    private final List<RunnerInjector<OperandType>> injectorsToKeepAfterClear = new ArrayList<>();

    private int flagsBefore = -1;

    public UnaryRunner(CpuRunner cpuRunner) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
    }

    public void inject(RunnerInjector<OperandType>... injectors) {
        this.injectors.addAll(Arrays.asList(injectors));
    }

    public void keepCurrentInjectorsAfterClear() {
        injectorsToKeepAfterClear.addAll(injectors);
    }

    public void clearInjectors() {
        injectors.clear();
        injectors.addAll(injectorsToKeepAfterClear);
    }

    @Override
    public RunnerContext<OperandType> apply(OperandType first) {
        cpuRunner.reset();

        // set flags first; they may get overwritten by some injector
        if (flagsBefore != -1) {
            cpuRunner.setFlags(flagsBefore);
        }

        for (RunnerInjector<OperandType> injector : injectors) {
            injector.inject(cpuRunner, first);
        }

        RunnerContext<OperandType> context = new RunnerContext<>(
                first, (OperandType)(Number)0, cpuRunner.getFlags(), cpuRunner.getPC(), cpuRunner.getSP()
        );

        cpuRunner.step();
        flagsBefore = cpuRunner.getFlags();

        return context;
    }

    @Override
    public UnaryRunner clone() {
        UnaryRunner<OperandType> runner = new UnaryRunner<>(cpuRunner);
        runner.flagsBefore = flagsBefore;
        runner.injectors.addAll(this.injectors);

        return runner;
    }
}
