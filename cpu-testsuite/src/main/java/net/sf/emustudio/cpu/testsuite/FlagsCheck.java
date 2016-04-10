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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public abstract class FlagsCheck<T extends Number, SpecificFlagsBuilder extends FlagsCheck<T, ?>> {
    protected final List<BiConsumer<RunnerContext<T>, Integer>> evaluators = new ArrayList<>();

    private boolean switchFirstAndSecond;
    protected int expectedFlags = 0;
    protected int expectedNotFlags = 0;

    public SpecificFlagsBuilder reset() {
        expectedFlags = 0;
        expectedNotFlags = 0;
        return (SpecificFlagsBuilder)this;
    }

    public SpecificFlagsBuilder or(int flags) {
        expectedFlags |= flags;
        return (SpecificFlagsBuilder)this;
    }

    public SpecificFlagsBuilder switchFirstAndSecond() {
        switchFirstAndSecond = !switchFirstAndSecond;
        return (SpecificFlagsBuilder)this;
    }

    public SpecificFlagsBuilder expectFlagOnlyWhen(int flag, BiFunction<RunnerContext<T>, Number, Boolean> predicate) {
        evaluators.add(((context, result) -> {
            if (predicate.apply(context, result)) {
                expectedFlags |= flag;
            } else {
                expectedNotFlags |= flag;
            }
        }));
        return (SpecificFlagsBuilder)this;
    }

    public int getExpectedFlags() {
        return expectedFlags;
    }

    public int getNotExpectedFlags() {
        return expectedNotFlags;
    }

    public void eval(RunnerContext<T> context, int result) {
        for (BiConsumer<RunnerContext<T>, Integer> evaluator : evaluators) {
            if (switchFirstAndSecond) {
                evaluator.accept(context.switchFirstAndSecond(), result);
            } else {
                evaluator.accept(context, result);
            }
        }
    }

}
