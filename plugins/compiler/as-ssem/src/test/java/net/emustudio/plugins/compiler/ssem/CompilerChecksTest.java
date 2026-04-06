/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.*;

public class CompilerChecksTest {

    private static final SourceCodePosition POS = new SourceCodePosition(1, 0, "test.ssem");

    @Test
    public void testCheckStartLineDefinedDoesNotThrowWhenNotDefined() {
        CompilerChecks.checkStartLineDefined(false, POS, 0);
        // no exception means pass
    }

    @Test(expected = CompileException.class)
    public void testCheckStartLineDefinedThrowsWhenAlreadyDefined() {
        CompilerChecks.checkStartLineDefined(true, POS, 5);
    }

    @Test
    public void testCheckLineOutOfBoundsAcceptsValidLine() {
        CompilerChecks.checkLineOutOfBounds(POS, 0);
        CompilerChecks.checkLineOutOfBounds(POS, 15);
        CompilerChecks.checkLineOutOfBounds(POS, 31);
    }

    @Test(expected = CompileException.class)
    public void testCheckLineOutOfBoundsThrowsForNegativeLine() {
        CompilerChecks.checkLineOutOfBounds(POS, -1);
    }

    @Test(expected = CompileException.class)
    public void testCheckLineOutOfBoundsThrowsForLineTooHigh() {
        CompilerChecks.checkLineOutOfBounds(POS, 32);
    }

    @Test
    public void testCheckDuplicateLineDefinitionDoesNotThrowWhenNoDuplicate() {
        CompilerChecks.checkDuplicateLineDefinition(false, POS, 1);
    }

    @Test(expected = CompileException.class)
    public void testCheckDuplicateLineDefinitionThrowsWhenDuplicate() {
        CompilerChecks.checkDuplicateLineDefinition(true, POS, 1);
    }

    @Test
    public void testCheckUnknownInstructionDoesNotThrowWhenKnown() {
        CompilerChecks.checkUnknownInstruction(false, POS);
    }

    @Test(expected = CompileException.class)
    public void testCheckUnknownInstructionThrowsWhenUnknown() {
        CompilerChecks.checkUnknownInstruction(true, POS);
    }

    @Test
    public void testCheckOperandOutOfBoundsAcceptsValidOperand() {
        CompilerChecks.checkOperandOutOfBounds(POS, SSEMLexer.JMP, 0);
        CompilerChecks.checkOperandOutOfBounds(POS, SSEMLexer.JMP, 31);
    }

    @Test(expected = CompileException.class)
    public void testCheckOperandOutOfBoundsThrowsForOperandTooHigh() {
        CompilerChecks.checkOperandOutOfBounds(POS, SSEMLexer.JMP, 32);
    }

    @Test(expected = CompileException.class)
    public void testCheckOperandOutOfBoundsThrowsForNegativeOperand() {
        CompilerChecks.checkOperandOutOfBounds(POS, SSEMLexer.JMP, -1);
    }

    @Test
    public void testCheckOperandOutOfBoundsSkipsCheckForBNUM() {
        // BNUM can have operands out of 0-31 range
        CompilerChecks.checkOperandOutOfBounds(POS, SSEMLexer.BNUM, 99999);
    }

    @Test
    public void testCheckOperandOutOfBoundsSkipsCheckForNUM() {
        // NUM can have operands out of 0-31 range
        CompilerChecks.checkOperandOutOfBounds(POS, SSEMLexer.NUM, 99999);
    }

    @Test
    public void testCheckedParseNumberSuccess() {
        CommonToken token = new CommonToken(SSEMLexer.NUMBER, "42");
        token.setLine(1);
        token.setCharPositionInLine(0);
        Function<Token, Integer> parser = t -> Integer.parseInt(t.getText());
        int result = CompilerChecks.checkedParseNumber("test.ssem", token, parser);
        assertEquals(42, result);
    }

    @Test(expected = CompileException.class)
    public void testCheckedParseNumberThrowsOnInvalidNumber() {
        CommonToken token = new CommonToken(SSEMLexer.NUMBER, "notanumber");
        token.setLine(1);
        token.setCharPositionInLine(0);
        Function<Token, Integer> parser = t -> Integer.parseInt(t.getText());
        CompilerChecks.checkedParseNumber("test.ssem", token, parser);
    }
}

