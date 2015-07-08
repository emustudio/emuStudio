package net.sf.emustudio.intel8080.impl.suite.runners;

import net.jcip.annotations.NotThreadSafe;
import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@NotThreadSafe
public class BinaryRunner<OperandType extends Number>
        implements BiFunction<OperandType, OperandType, RunnerContext<OperandType>> {
    private final CpuRunner cpuRunner;
    private final Map<RunnerInjector<OperandType>, Boolean> injectors = new HashMap<>();
    private final List<BothRunnerInjector<OperandType>> bothInjectors = new ArrayList<>();

    private final Map<RunnerInjector<OperandType>, Boolean> injectorsToKeepAfterClear = new HashMap<>();
    private final List<BothRunnerInjector<OperandType>> bothInjectorsToKeepAfterClear = new ArrayList<>();

    private int flagsBefore = -1;

    @FunctionalInterface
    public interface BothRunnerInjector<OT extends Number> {
        void inject(CpuRunner cpuRunner, OT first, OT second);
    }

    public BinaryRunner(CpuRunner cpuRunner) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
    }

    public void keepCurrentInjectorsAfterClear() {
        injectorsToKeepAfterClear.putAll(injectors);
        bothInjectorsToKeepAfterClear.addAll(bothInjectors);
    }

    public void injectFirst(RunnerInjector<OperandType>... injectors) {
        for (RunnerInjector<OperandType> injector : injectors) {
            this.injectors.put(injector, true);
        }
    }

    public void injectSecond(RunnerInjector<OperandType>... injectors) {
        for (RunnerInjector<OperandType> injector : injectors) {
            this.injectors.put(injector, false);
        }
    }

    public void injectBoth(BothRunnerInjector... injectors) {
        bothInjectors.addAll(Arrays.asList(injectors));
    }

    public void clearInjectors() {
        injectors.clear();
        bothInjectors.clear();

        injectors.putAll(injectorsToKeepAfterClear);
        bothInjectors.addAll(bothInjectorsToKeepAfterClear);
    }

    @Override
    public RunnerContext<OperandType> apply(OperandType first, OperandType second) {
        cpuRunner.reset();

        // first preserve flags; they may get overwritten by some injector
        if (flagsBefore != -1) {
            cpuRunner.setFlags(flagsBefore);
        }

        for (Map.Entry<RunnerInjector<OperandType>, Boolean> injectorEntry : injectors.entrySet()) {
            OperandType argument = injectorEntry.getValue() ? first : second;
            injectorEntry.getKey().inject(cpuRunner, argument);
        }
        for (BothRunnerInjector<OperandType> bothInjector : bothInjectors) {
            bothInjector.inject(cpuRunner, first, second);
        }

        RunnerContext<OperandType> context = new RunnerContext<>(
                first, second, cpuRunner.getFlags(), cpuRunner.getPC(), cpuRunner.getSP()
        );

        cpuRunner.step();
        flagsBefore = cpuRunner.getFlags();

        return context;
    }

    @Override
    public BinaryRunner clone() {
        BinaryRunner<OperandType> runner = new BinaryRunner<>(cpuRunner);
        runner.flagsBefore = flagsBefore;
        runner.injectors.putAll(this.injectors);
        runner.bothInjectors.addAll(this.bothInjectors);

        return runner;
    }

}
