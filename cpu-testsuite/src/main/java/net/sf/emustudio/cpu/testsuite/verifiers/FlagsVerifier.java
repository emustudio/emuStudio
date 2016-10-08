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
package net.sf.emustudio.cpu.testsuite.verifiers;

import net.sf.emustudio.cpu.testsuite.CpuVerifier;
import net.sf.emustudio.cpu.testsuite.FlagsCheck;
import net.sf.emustudio.cpu.testsuite.RunnerContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Flags verifier.
 *
 * Used as a test verifier.
 *
 * @param <OperandT> operands type (Byte or Integer)
 */
public class FlagsVerifier<OperandT extends Number> implements Consumer<RunnerContext<OperandT>> {
    private final Function<RunnerContext<OperandT>, Integer> operation;
    private final FlagsCheck flagsCheck;
    private final CpuVerifier verifier;

    /**
     * Creates new flags verifier.
     *
     * @param verifier CPU verifier
     * @param operation operation which will be used for checking flags
     * @param flagsCheck flags checker
     */
    public FlagsVerifier(CpuVerifier verifier, Function<RunnerContext<OperandT>, Integer> operation, FlagsCheck flagsCheck) {
        this.operation = Objects.requireNonNull(operation);
        this.flagsCheck = Objects.requireNonNull(flagsCheck);
        this.verifier = Objects.requireNonNull(verifier);
    }

    @Override
    public void accept(RunnerContext<OperandT> context) {
        flagsCheck.reset();
        flagsCheck.eval(context, operation.apply(context));

        verifier.checkFlags(flagsCheck.getExpectedFlags());
        verifier.checkNotFlags(flagsCheck.getNotExpectedFlags());
    }
}
