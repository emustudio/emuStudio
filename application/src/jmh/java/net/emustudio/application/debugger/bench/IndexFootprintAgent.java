/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import java.lang.instrument.Instrumentation;

/** Benchmark-only shallow-size provider, never packaged with the application. */
public final class IndexFootprintAgent {
    private static Instrumentation instrumentation;
    private IndexFootprintAgent() { }
    public static void premain(String arguments, Instrumentation value) { instrumentation = value; }
    static long sizeOf(Object value) {
        if (instrumentation == null) throw new IllegalStateException("Run with the benchmark instrumentation agent");
        return instrumentation.getObjectSize(value);
    }
}
