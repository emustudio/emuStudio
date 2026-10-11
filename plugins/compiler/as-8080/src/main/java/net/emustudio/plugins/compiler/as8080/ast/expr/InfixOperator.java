/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import java.util.function.BiFunction;

public enum InfixOperator {
    ADD(Integer::sum),
    SUBTRACT((x, y) -> x - y),
    DIVIDE((x, y) -> x / y),
    MULTIPLY((x, y) -> x * y),
    MOD((x, y) -> x % y),
    AND((x, y) -> x & y),
    OR((x, y) -> x | y),
    XOR((x, y) -> x ^ y),
    SHL((x, y) -> x << y),
    SHR((x, y) -> x >>> y),
    EQUAL((x, y) -> x.equals(y) ? 1 : 0),
    LT((x, y) -> x < y ? 1 : 0),
    LTE((x, y) -> x <= y ? 1 : 0),
    GT((x, y) -> x > y ? 1 : 0),
    GTE((x, y) -> x >= y ? 1 : 0);

    private final BiFunction<Integer, Integer, Integer> operation;

    InfixOperator(BiFunction<Integer, Integer, Integer> operation) {
        this.operation = operation;
    }

    int apply(int left, int right) {
        return operation.apply(left, right);
    }
}
