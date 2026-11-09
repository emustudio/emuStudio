/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.emulib.plugin.cpu.AbstractCPUContext;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import org.mockito.MockSettings;
import static org.mockito.Mockito.*;

/** CPU test doubles expose the same stable context as real CPU plugins. */
public final class TestCpu {
    private static final Map<CPU, Context> contexts = Collections.synchronizedMap(new WeakHashMap<>());
    private TestCpu() { }
    public static CPU mockCpu() { return mockCpu(withSettings()); }
    public static CPU mockCpu(MockSettings settings) {
        CPU cpu = mock(CPU.class, settings);
        Context context = spy(new Context());
        context.publish(CPU.RunState.STATE_STOPPED_BREAK, 0);
        contexts.put(cpu, context);
        return cpu;
    }

    public static CPUContext context(CPU cpu) { return Objects.requireNonNull(contexts.get(cpu)); }

    public static void publish(CPU cpu, CPU.RunState state) {
        ((Context) context(cpu)).publish(state, context(cpu).getInstructionLocation());
    }

    private static final class Context extends AbstractCPUContext {
        @Override public boolean isPassedCyclesSupported() { return false; }
        void publish(CPU.RunState state, int address) { fireStateChanged(state, address, Float.NaN); }
    }
}
