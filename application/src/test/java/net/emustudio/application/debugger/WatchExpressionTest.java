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
        assertEquals(14L, WatchExpression.evaluate(" 2 + 3 * 4 ", null, null));
        assertEquals(20L, WatchExpression.evaluate("(2 + 3) * 4", null, null));
        assertEquals(5L, WatchExpression.evaluate("12 - 4 - 3", null, null));
        assertEquals(3L, WatchExpression.evaluate("20 / 3 % 3 + 3", null, null));
    }

    @Test
    public void shiftsUseArithmeticOperandsAndUnsignedRightShift() {
        assertEquals(16L, WatchExpression.evaluate("1 << 2 + 2", null, null));
        assertEquals(Long.MAX_VALUE, WatchExpression.evaluate("0xFFFFFFFFFFFFFFFF >> 1", null, null));
    }

    @Test
    public void bitwiseOperatorsHaveDistinctPrecedence() {
        assertEquals(5L, WatchExpression.evaluate("1 | 6 ^ 3 & 2", null, null));
        assertEquals(240L, WatchExpression.evaluate("~0x0F & 0xFF", null, null));
    }

    @Test
    public void comparisonsReturnBooleanIntegers() {
        assertEquals(1L, WatchExpression.evaluate("3 < 4 && 4 <= 4 && 5 > 4 && 5 >= 5", null, null));
        assertEquals(1L, WatchExpression.evaluate("3 == 3 && 3 != 4", null, null));
        assertEquals(0L, WatchExpression.evaluate("3 == 4 || 5 < 4", null, null));
    }

    @Test
    public void unarySignsAndLogicalNegationApplyBeforeAddition() {
        assertEquals(-2L, WatchExpression.evaluate("-3 + +1", null, null));
        assertEquals(1L, WatchExpression.evaluate("!0", null, null));
        assertEquals(0L, WatchExpression.evaluate("!42", null, null));
    }

    @Test
    public void registerNamesAndExplicitRegisterCallsReadCpuValues() {
        DebugCPU cpu = mock(DebugCPU.class);
        when(cpu.readRegister("PC")).thenReturn(0x100L);
        when(cpu.readRegister("SP")).thenReturn(0x10L);

        long result = WatchExpression.evaluate("PC + reg(SP)", cpu, null);

        assertEquals(0x110L, result);
        verify(cpu).readRegister("PC");
        verify(cpu).readRegister("SP");
    }

    @Test
    public void byteReadsUseCpuWiringAndUnsignedValues() {
        MemoryContext<Byte> memory = byteMemory(4);
        when(memory.peek(2)).thenReturn((byte) 0xff);

        long result = WatchExpression.evaluate("mem[1 + 1]", null, memory);

        assertEquals(255L, result);
        verify(memory).peek(2);
    }

    @Test
    public void wordAndDoubleWordReadsSupportBothByteOrdersAndDelimiters() {
        MemoryContext<Byte> memory = byteMemory(4);
        when(memory.peek(0)).thenReturn((byte) 0x12);
        when(memory.peek(1)).thenReturn((byte) 0x34);
        when(memory.peek(2)).thenReturn((byte) 0x80);
        when(memory.peek(3)).thenReturn((byte) 0xff);

        assertEquals(0x3412L, WatchExpression.evaluate("mem16[0]", null, memory));
        assertEquals(0x1234L, WatchExpression.evaluate("mem16be(0)", null, memory));
        assertEquals(0xff803412L, WatchExpression.evaluate("mem32(0)", null, memory));
        assertEquals(0x123480ffL, WatchExpression.evaluate("mem32be[0]", null, memory));
    }

    @Test
    public void falseConjunctionSkipsUnavailableRegisterAndMemoryReads() {
        assertEquals(0L, WatchExpression.evaluate("0 && PC", null, null));
        assertEquals(0L, WatchExpression.evaluate("0 && mem[-1]", null, null));
        assertEquals(0L, WatchExpression.evaluate("0 && (1 / 0)", null, null));
    }

    @Test
    public void trueDisjunctionSkipsUnavailableReadsAndNormalizesTruth() {
        assertEquals(1L, WatchExpression.evaluate("7 || PC", null, null));
        assertEquals(1L, WatchExpression.evaluate("1 || mem[0]", null, null));
        assertEquals(1L, WatchExpression.evaluate("1 || (1 / 0)", null, null));
    }

    @Test
    public void shortCircuitStillRejectsMalformedRightHandSyntax() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("1 || (2 + )", null, null));

        assertTrue(error.getMessage().contains("Expected number, register, or memory expression"));
    }

    @Test
    public void unsupportedRegisterInspectionReportsReason() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("PC", null, null));

        assertTrue(error.getMessage().contains("CPU register inspection unsupported"));
    }

    @Test
    public void outOfBoundsMemoryReadsFailBeforeReadingCells() {
        MemoryContext<Byte> memory = byteMemory(4);

        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[-1]", null, memory));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[4]", null, memory));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem16[3]", null, memory));
        verify(memory, never()).peek(anyInt());
    }

    @Test
    public void missingMemoryAndUnknownMemoryFunctionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[0]", null, null));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem64[0]", null, null));
        assertTrue(error.getMessage().contains("Unknown memory function"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nonByteMemoryIsRejectedBeforeReadingCells() {
        MemoryContext<Integer> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Integer.class);

        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[0]", null, memory));

        verify(memory, never()).peek(anyInt());
    }

    @Test
    public void syntaxErrorsIncludePositionAndRequiredDelimiter() {
        IllegalArgumentException missingBracket = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("mem[0", null, null));
        IllegalArgumentException trailingInput = assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("1 2", null, null));

        assertEquals("Expected ] at column 6", missingBracket.getMessage());
        assertEquals("Unexpected input at column 3", trailingInput.getMessage());
    }

    @Test
    public void emptyNullAndOversizedExpressionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate(null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> WatchExpression.evaluate("1".repeat(1025), null, null));
    }

    @SuppressWarnings("unchecked")
    private static MemoryContext<Byte> byteMemory(int size) {
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(size);
        return memory;
    }
}
