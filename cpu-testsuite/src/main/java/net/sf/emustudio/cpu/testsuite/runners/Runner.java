package net.sf.emustudio.cpu.testsuite.runners;

import net.jcip.annotations.NotThreadSafe;
import net.sf.emustudio.cpu.testsuite.CpuRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

@NotThreadSafe
public class Runner<OperandType extends Number, CpuRunnerType extends CpuRunner>
        implements BiFunction<OperandType, OperandType, RunnerContext<OperandType>> {
    private final CpuRunnerType cpuRunner;
    private final Map<RunnerInjector<OperandType, CpuRunnerType>, Boolean> injectors = new HashMap<>();
    private final List<BothRunnerInjector<OperandType, CpuRunnerType>> bothInjectors = new ArrayList<>();

    private final Map<RunnerInjector<OperandType, CpuRunnerType>, Boolean> injectorsToKeepAfterClear = new HashMap<>();
    private final List<BothRunnerInjector<OperandType, CpuRunnerType>> bothInjectorsToKeepAfterClear = new ArrayList<>();

    private int flagsBefore = -1;

    @FunctionalInterface
    public interface BothRunnerInjector<OT extends Number, TCpuRunnerType extends CpuRunner> {
        void inject(TCpuRunnerType cpuRunner, OT first, OT second);
    }

    public Runner(CpuRunnerType cpuRunner) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
    }

    public void keepCurrentInjectorsAfterClear() {
        injectorsToKeepAfterClear.putAll(injectors);
        bothInjectorsToKeepAfterClear.addAll(bothInjectors);
    }

    public void injectFirst(RunnerInjector<OperandType, CpuRunnerType>... injectors) {
        for (RunnerInjector<OperandType, CpuRunnerType> injector : injectors) {
            this.injectors.put(injector, true);
        }
    }

    public void injectSecond(RunnerInjector<OperandType, CpuRunnerType>... injectors) {
        for (RunnerInjector<OperandType, CpuRunnerType> injector : injectors) {
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

        for (Map.Entry<RunnerInjector<OperandType, CpuRunnerType>, Boolean> injectorEntry : injectors.entrySet()) {
            OperandType argument = injectorEntry.getValue() ? first : second;
            injectorEntry.getKey().inject(cpuRunner, argument);
        }
        for (BothRunnerInjector<OperandType, CpuRunnerType> bothInjector : bothInjectors) {
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
    public Runner<OperandType, CpuRunnerType> clone() {
        Runner<OperandType, CpuRunnerType> runner = new Runner<>(cpuRunner);
        runner.flagsBefore = flagsBefore;
        runner.injectors.putAll(this.injectors);
        runner.bothInjectors.addAll(this.bothInjectors);

        return runner;
    }

}
