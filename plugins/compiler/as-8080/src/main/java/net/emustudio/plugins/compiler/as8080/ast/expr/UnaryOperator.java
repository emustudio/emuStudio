/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import java.util.function.Function;

public enum UnaryOperator {
    ADD(x -> x),
    SUBTRACT(x -> -x),
    NOT(x -> ~x);

    private final Function<Integer, Integer> operation;

    UnaryOperator(Function<Integer, Integer> operation) {
        this.operation = operation;
    }

    int apply(int value) {
        return operation.apply(value);
    }
}
