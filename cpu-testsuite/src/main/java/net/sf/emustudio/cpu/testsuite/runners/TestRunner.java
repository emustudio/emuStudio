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

import net.jcip.annotations.NotThreadSafe;
import net.sf.emustudio.cpu.testsuite.CpuRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

@NotThreadSafe
public class TestRunner<OperandType extends Number, CpuRunnerType extends CpuRunner>
        implements BiFunction<OperandType, OperandType, RunnerContext<OperandType>> {
    private final CpuRunnerType cpuRunner;
    private final Map<SingleOperandInjector<OperandType, CpuRunnerType>, Boolean> singleOpInjectors = new LinkedHashMap<>();
    private final List<TwoOperandsInjector<OperandType, CpuRunnerType>> twoOpInjectors = new ArrayList<>();

    private final Map<SingleOperandInjector<OperandType, CpuRunnerType>, Boolean> toKeepSingleOpInjectors = new LinkedHashMap<>();
    private final List<TwoOperandsInjector<OperandType, CpuRunnerType>> toKeepTwoOpInjectors = new ArrayList<>();

    private int flagsBefore = -1;
    private boolean printInjectingProcess;

    public TestRunner(CpuRunnerType cpuRunner) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
    }

    public void keepCurrentInjectorsAfterClear() {
        toKeepSingleOpInjectors.putAll(singleOpInjectors);
        toKeepTwoOpInjectors.addAll(twoOpInjectors);
    }

    public void printInjectingProcess() {
        this.printInjectingProcess = true;
    }

    public void injectFirst(SingleOperandInjector<OperandType, CpuRunnerType>... injectors) {
        for (SingleOperandInjector<OperandType, CpuRunnerType> injector : injectors) {
            this.singleOpInjectors.put(injector, true);
        }
    }

    public void injectSecond(SingleOperandInjector<OperandType, CpuRunnerType>... injectors) {
        for (SingleOperandInjector<OperandType, CpuRunnerType> injector : injectors) {
            this.singleOpInjectors.put(injector, false);
        }
    }

    public void injectTwoOperands(TwoOperandsInjector<OperandType, CpuRunnerType>... injectors) {
        twoOpInjectors.addAll(Arrays.asList(injectors));
    }

    public void clearInjectors() {
        singleOpInjectors.clear();
        twoOpInjectors.clear();

        singleOpInjectors.putAll(toKeepSingleOpInjectors);
        twoOpInjectors.addAll(toKeepTwoOpInjectors);
    }

    private void printInjection(String formatString, Object... arguments) {
        System.out.println(String.format("Injecting " + formatString + " (to %s)", arguments));
    }

    @Override
    public RunnerContext<OperandType> apply(OperandType first, OperandType second) {
        cpuRunner.reset();

        // first preserve flags; they may get overwritten by some injector
        if (flagsBefore != -1) {
            cpuRunner.setFlags(flagsBefore);
        }

        for (Map.Entry<SingleOperandInjector<OperandType, CpuRunnerType>, Boolean> singleOpInjector : singleOpInjectors.entrySet()) {
            OperandType argument = singleOpInjector.getValue() ? first : second;
            if (printInjectingProcess) {
                String which = singleOpInjector.getValue() ? "first" : "second";
                printInjection(which + ": %x", argument, singleOpInjector.getKey());
            }
            singleOpInjector.getKey().inject(cpuRunner, argument);
        }
        for (TwoOperandsInjector<OperandType, CpuRunnerType> twoOpInjector : twoOpInjectors) {
            if (printInjectingProcess) {
                printInjection("two: %x, %x", first, second, twoOpInjector);
            }
            twoOpInjector.inject(cpuRunner, first, second);
        }

        RunnerContext<OperandType> context = new RunnerContext<>(
                first, second, cpuRunner.getFlags(), cpuRunner.getPC(), cpuRunner.getSP(), cpuRunner.getRegisters()
        );

        cpuRunner.step();
        flagsBefore = cpuRunner.getFlags();

        return context;
    }

    @Override
    public TestRunner<OperandType, CpuRunnerType> clone() {
        TestRunner<OperandType, CpuRunnerType> runner = new TestRunner<>(cpuRunner);
        runner.flagsBefore = flagsBefore;

        runner.singleOpInjectors.putAll(this.singleOpInjectors);
        runner.twoOpInjectors.addAll(this.twoOpInjectors);

        runner.toKeepSingleOpInjectors.putAll(toKeepSingleOpInjectors);
        runner.toKeepTwoOpInjectors.addAll(toKeepTwoOpInjectors);

        if (printInjectingProcess) {
            runner.printInjectingProcess();
        }

        return runner;
    }

}
