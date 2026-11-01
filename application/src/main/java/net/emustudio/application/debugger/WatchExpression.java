/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.*;
import java.util.function.LongSupplier;

/**
 * Small expression language: registers, reg(NAME), mem[address], mem16/16be/32, integer arithmetic,
 * bit operations, comparisons and short-circuit &&/||.
 */
public final class WatchExpression {
    private final String text;
    private final DebugCPU cpu;
    private final MemoryContext<?> memory;
    private final int bank;
    private int at;

    private WatchExpression(String text, DebugCPU cpu, MemoryContext<?> memory, int bank) {
        if (text == null || text.length() > 1024)
            throw new IllegalArgumentException("Expression too long");
        this.text = text;
        this.cpu = cpu;
        this.memory = memory;
        this.bank = bank;
    }

    public static long evaluate(String text, DebugCPU cpu, MemoryContext<?> memory, int bank) {
        WatchExpression p = new WatchExpression(text, cpu, memory, bank);
        LongSupplier value = p.expression(0);
        p.space();
        if (p.at != text.length()) throw p.error("Unexpected input");
        return value.getAsLong();
    }

    private static final String[][] OPS = {
        {"||"},
        {"&&"},
        {"|"},
        {"^"},
        {"&"},
        {"==", "!="},
        {"<=", ">=", "<", ">"},
        {"<<", ">>"},
        {"+", "-"},
        {"*", "/", "%"}
    };

    private LongSupplier expression(int level) {
        if (level == OPS.length) return primary();
        LongSupplier left = expression(level + 1);
        for (; ; ) {
            String op = null;
            space();
            for (String candidate : OPS[level]) {
                if (text.startsWith(candidate, at)
                        && !(candidate.length() == 1
                                && at + 1 < text.length()
                                && text.charAt(at + 1) == candidate.charAt(0)
                                && "&|<>".contains(candidate))) {
                    op = candidate;
                    at += candidate.length();
                    break;
                }
            }
            if (op == null) return left;
            LongSupplier a = left, b = expression(level + 1);
            String operator = op;
            left = () -> apply(operator, a, b);
        }
    }

    private static long apply(String op, LongSupplier a, LongSupplier b) {
        long x = a.getAsLong();
        if (op.equals("&&")) return x != 0 && b.getAsLong() != 0 ? 1 : 0;
        if (op.equals("||")) return x != 0 || b.getAsLong() != 0 ? 1 : 0;
        long y = b.getAsLong();
        switch (op) {
            case "+":
                return x + y;
            case "-":
                return x - y;
            case "*":
                return x * y;
            case "/":
                return x / y;
            case "%":
                return x % y;
            case "&":
                return x & y;
            case "|":
                return x | y;
            case "^":
                return x ^ y;
            case "<<":
                return x << y;
            case ">>":
                return x >>> y;
            case "==":
                return x == y ? 1 : 0;
            case "!=":
                return x != y ? 1 : 0;
            case "<":
                return x < y ? 1 : 0;
            case ">":
                return x > y ? 1 : 0;
            case "<=":
                return x <= y ? 1 : 0;
            case ">=":
                return x >= y ? 1 : 0;
            default:
                throw new AssertionError(op);
        }
    }

    private LongSupplier primary() {
        space();
        if (take("(")) {
            LongSupplier value = expression(0);
            expect(")");
            return value;
        }
        if (take("-")) {
            LongSupplier value = primary();
            return () -> -value.getAsLong();
        }
        if (take("~")) {
            LongSupplier value = primary();
            return () -> ~value.getAsLong();
        }
        if (take("!")) {
            LongSupplier value = primary();
            return () -> value.getAsLong() == 0 ? 1 : 0;
        }
        if (take("+")) return primary();
        space();
        int start = at;
        while (at < text.length()
                && (Character.isLetterOrDigit(text.charAt(at)) || text.charAt(at) == '_')) at++;
        if (start == at) throw error("Expected number, register, or memory expression");
        String token = text.substring(start, at);
        if (Character.isDigit(token.charAt(0))) {
            long value =
                    token.startsWith("0x") || token.startsWith("0X")
                            ? Long.parseUnsignedLong(token.substring(2), 16)
                            : Long.parseLong(token);
            return () -> value;
        }
        if (token.equalsIgnoreCase("reg")) {
            expect("(");
            space();
            int begin = at;
            while (at < text.length() && Character.isLetterOrDigit(text.charAt(at))) at++;
            String name = text.substring(begin, at);
            expect(")");
            return () -> register(name);
        }
        if (token.toLowerCase(Locale.ROOT).startsWith("mem")) {
            String fn = token.toLowerCase(Locale.ROOT);
            int width;
            switch (fn) {
                case "mem":
                    width = 1;
                    break;
                case "mem16":
                case "mem16be":
                    width = 2;
                    break;
                case "mem32":
                case "mem32be":
                    width = 4;
                    break;
                default:
                    throw error("Unknown memory function");
            }
            boolean bracket = take("[");
            if (!bracket) expect("(");
            LongSupplier address = expression(0);
            expect(bracket ? "]" : ")");
            return () -> {
                long location = address.getAsLong();
                if (memory == null
                        || memory.getCellTypeClass() != Byte.class
                        || location < 0
                        || location > memory.getSize() - width)
                    throw error("Memory address outside byte memory");
                long value = 0;
                for (int i = 0; i < width; i++)
                    value |=
                            ((Byte) memory.readBank((int) location + i, bank) & 255L)
                                    << (8 * (fn.endsWith("be") ? width - 1 - i : i));
                return value;
            };
        }
        return () -> register(token);
    }

    private long register(String name) {
        if (cpu == null) throw error("CPU register inspection unsupported");
        return cpu.readRegister(name);
    }

    private void space() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++;
    }

    private boolean take(String s) {
        space();
        if (!text.startsWith(s, at)) return false;
        at += s.length();
        return true;
    }

    private void expect(String s) {
        if (!take(s)) throw error("Expected " + s);
    }

    private IllegalArgumentException error(String s) {
        return new IllegalArgumentException(s + " at column " + (at + 1));
    }
}
