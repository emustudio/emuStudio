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
package net.sf.emustudio.cpu.testsuite;

import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Test<T extends Number> implements BiConsumer<T, T> {
    private final List<Consumer<RunnerContext<T>>> verifiers = new ArrayList<>();
    private final BiFunction<T, T, RunnerContext<T>> runner;

    public Test(BiFunction<T, T, RunnerContext<T>> runner, List<Consumer<RunnerContext<T>>> verifiers) {
        this.runner = Objects.requireNonNull(runner);
        this.verifiers.addAll(new ArrayList<>(Objects.requireNonNull(verifiers)));
    }

    private void verify(RunnerContext<T> context) {
        for (Consumer<RunnerContext<T>> verifier : verifiers) {
            try {
                verifier.accept(context);
            } catch (Throwable e) {
                System.out.println("Verifier failed: " + context);
                throw e;
            }
        }
    }

    @Override
    public void accept(T first, T second) {
        verify(runner.apply(first, second));
    }

}
