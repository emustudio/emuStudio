/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import static org.junit.Assert.*;

import net.emustudio.emulib.plugins.cpu.debug.DebugProbe;

import org.junit.Test;

public class WatchExpressionTest {
    @Test
    public void expressionsHavePrecedencePointersAndShortCircuit() {
        DebugMemory m = new DebugMemory(16, 2, 8);
        m.values[1][2] = 0x34;
        m.values[1][3] = 0x12;
        DebugProbe cpu = new DebugProbe("Z80", m, org.mockito.Mockito.mock(net.emustudio.emulib.plugins.cpu.Disassembler.class));
        cpu.register("HL", 16, () -> 2, v -> {});
        assertEquals(0x1234, WatchExpression.evaluate("mem16[HL]", cpu, m, 1));
        assertEquals(0x3412, WatchExpression.evaluate("mem16be(reg(HL))", cpu, m, 1));
        assertEquals(7, WatchExpression.evaluate("1 + 2 * 3", cpu, m, 0));
        assertEquals(1, WatchExpression.evaluate("(HL << 2) == 8 && mem[HL] == 0x34", cpu, m, 1));
        assertEquals(0, WatchExpression.evaluate("0 && (1 / 0)", cpu, m, 0));
        assertEquals(1, WatchExpression.evaluate("1 || mem[999]", cpu, m, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem16[15]", cpu, m, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> WatchExpression.evaluate("UNKNOWN", cpu, m, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> WatchExpression.evaluate("1 garbage", cpu, m, 0));
    }
}
