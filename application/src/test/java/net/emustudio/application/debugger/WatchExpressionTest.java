/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class WatchExpressionTest {
    @Test
    public void multiplicationPrecedesAdditionAndParenthesesOverridePrecedence() {
        assertEquals(14L, WatchExpression.evaluate(" 2 + 3 * 4 ", null, null, 0));
        assertEquals(20L, WatchExpression.evaluate("(2 + 3) * 4", null, null, 0));
        assertEquals(5L, WatchExpression.evaluate("12 - 4 - 3", null, null, 0));
        assertEquals(3L, WatchExpression.evaluate("20 / 3 % 3 + 3", null, null, 0));
    }

    @Test
    public void shiftsUseArithmeticOperandsAndUnsignedRightShift() {
        assertEquals(16L, WatchExpression.evaluate("1 << 2 + 2", null, null, 0));
        assertEquals(Long.MAX_VALUE, WatchExpression.evaluate("0xFFFFFFFFFFFFFFFF >> 1", null, null, 0));
    }

    @Test
    public void bitwiseOperatorsHaveDistinctPrecedence() {
        assertEquals(5L, WatchExpression.evaluate("1 | 6 ^ 3 & 2", null, null, 0));
        assertEquals(240L, WatchExpression.evaluate("~0x0F & 0xFF", null, null, 0));
    }

    @Test
    public void comparisonsReturnBooleanIntegers() {
        assertEquals(1L, WatchExpression.evaluate("3 < 4 && 4 <= 4 && 5 > 4 && 5 >= 5", null, null, 0));
        assertEquals(1L, WatchExpression.evaluate("3 == 3 && 3 != 4", null, null, 0));
        assertEquals(0L, WatchExpression.evaluate("3 == 4 || 5 < 4", null, null, 0));
    }

    @Test
    public void unarySignsAndLogicalNegationApplyBeforeAddition() {
        assertEquals(-2L, WatchExpression.evaluate("-3 + +1", null, null, 0));
        assertEquals(1L, WatchExpression.evaluate("!0", null, null, 0));
        assertEquals(0L, WatchExpression.evaluate("!42", null, null, 0));
    }

    @Test
    public void registerNamesAndExplicitRegisterCallsReadCpuValues() {
        DebugCPU cpu = mock(DebugCPU.class);
        when(cpu.readRegister("PC")).thenReturn(0x100L);
        when(cpu.readRegister("SP")).thenReturn(0x10L);

        long result = WatchExpression.evaluate("PC + reg(SP)", cpu, null, 0);

        assertEquals(0x110L, result);
        verify(cpu).readRegister("PC");
        verify(cpu).readRegister("SP");
    }

    @Test
    public void byteReadsUseRequestedBankAndUnsignedValues() {
        MemoryContext<Byte> memory = byteMemory(4);
        when(memory.readBank(2, 1)).thenReturn((byte) 0xff);

        long result = WatchExpression.evaluate("mem[1 + 1]", null, memory, 1);

        assertEquals(255L, result);
        verify(memory).readBank(2, 1);
        verify(memory, never()).readBank(anyInt(), eq(0));
    }

    @Test
    public void wordAndDoubleWordReadsSupportBothByteOrdersAndDelimiters() {
        MemoryContext<Byte> memory = byteMemory(4);
        when(memory.readBank(0, 0)).thenReturn((byte) 0x12);
        when(memory.readBank(1, 0)).thenReturn((byte) 0x34);
        when(memory.readBank(2, 0)).thenReturn((byte) 0x80);
        when(memory.readBank(3, 0)).thenReturn((byte) 0xff);

        assertEquals(0x3412L, WatchExpression.evaluate("mem16[0]", null, memory, 0));
        assertEquals(0x1234L, WatchExpression.evaluate("mem16be(0)", null, memory, 0));
        assertEquals(0xff803412L, WatchExpression.evaluate("mem32(0)", null, memory, 0));
        assertEquals(0x123480ffL, WatchExpression.evaluate("mem32be[0]", null, memory, 0));
    }

    @Test
    public void falseConjunctionSkipsUnavailableRegisterAndMemoryReads() {
        assertEquals(0L, WatchExpression.evaluate("0 && PC", null, null, 0));
        assertEquals(0L, WatchExpression.evaluate("0 && mem[-1]", null, null, 0));
        assertEquals(0L, WatchExpression.evaluate("0 && (1 / 0)", null, null, 0));
    }

    @Test
    public void trueDisjunctionSkipsUnavailableReadsAndNormalizesTruth() {
        assertEquals(1L, WatchExpression.evaluate("7 || PC", null, null, 0));
        assertEquals(1L, WatchExpression.evaluate("1 || mem[0]", null, null, 0));
        assertEquals(1L, WatchExpression.evaluate("1 || (1 / 0)", null, null, 0));
    }

    @Test
    public void shortCircuitStillRejectsMalformedRightHandSyntax() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("1 || (2 + )", null, null, 0));

        assertTrue(error.getMessage().contains("Expected number, register, or memory expression"));
    }

    @Test
    public void unsupportedRegisterInspectionReportsReason() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("PC", null, null, 0));

        assertTrue(error.getMessage().contains("CPU register inspection unsupported"));
    }

    @Test
    public void outOfBoundsMemoryReadsFailBeforeReadingCells() {
        MemoryContext<Byte> memory = byteMemory(4);

        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[-1]", null, memory, 0));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[4]", null, memory, 0));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem16[3]", null, memory, 0));
        verify(memory, never()).readBank(anyInt(), anyInt());
    }

    @Test
    public void missingMemoryAndUnknownMemoryFunctionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[0]", null, null, 0));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem64[0]", null, null, 0));
        assertTrue(error.getMessage().contains("Unknown memory function"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nonByteMemoryIsRejectedBeforeReadingCells() {
        MemoryContext<Integer> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Integer.class);

        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[0]", null, memory, 0));

        verify(memory, never()).readBank(anyInt(), anyInt());
    }

    @Test
    public void syntaxErrorsIncludePositionAndRequiredDelimiter() {
        IllegalArgumentException missingBracket = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[0", null, null, 0));
        IllegalArgumentException trailingInput = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("1 2", null, null, 0));

        assertEquals("Expected ] at column 6", missingBracket.getMessage());
        assertEquals("Unexpected input at column 3", trailingInput.getMessage());
    }

    @Test
    public void emptyNullAndOversizedExpressionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("", null, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate(null, null, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("1".repeat(1025), null, null, 0));
    }

    @SuppressWarnings("unchecked")
    private static MemoryContext<Byte> byteMemory(int size) {
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(size);
        return memory;
    }
}
