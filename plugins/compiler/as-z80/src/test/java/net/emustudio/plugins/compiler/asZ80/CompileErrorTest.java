/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.*;

public class CompileErrorTest {
    private static final SourceCodePosition POS = new SourceCodePosition(1, 0, "test.asm");
    private static final Node NODE = new DataDB(POS);

    @Test
    public void testAlreadyDeclared() {
        CompileError error = CompileError.alreadyDeclared(NODE, "label");
        assertEquals(CompileError.ERROR_ALREADY_DECLARED, error.errorCode);
        assertTrue(error.msg.contains("label"));
        assertTrue(error.msg.contains("already declared"));
        assertNotNull(error.position);
    }

    @Test
    public void testInfiniteLoopDetected() {
        CompileError error = CompileError.infiniteLoopDetected(NODE, "include");
        assertEquals(CompileError.ERROR_INFINITE_LOOP_DETECTED, error.errorCode);
        assertTrue(error.msg.contains("include"));
        assertTrue(error.msg.contains("loop"));
    }

    @Test
    public void testCouldNotReadFile() {
        CompileError error = CompileError.couldNotReadFile(NODE, "file.asm", new IOException("not found"));
        assertEquals(CompileError.ERROR_CANNOT_READ_FILE, error.errorCode);
        assertTrue(error.msg.contains("file.asm"));
        assertTrue(error.msg.contains("not found"));
    }

    @Test
    public void testNotDefined() {
        CompileError error = CompileError.notDefined(NODE, "variable");
        assertEquals(CompileError.ERROR_NOT_DEFINED, error.errorCode);
        assertTrue(error.msg.contains("variable"));
    }

    @Test
    public void testAmbiguousExpression() {
        CompileError error = CompileError.ambiguousExpression(NODE);
        assertEquals(CompileError.ERROR_AMBIGUOUS_EXPRESSION, error.errorCode);
        assertTrue(error.msg.contains("Ambiguous"));
    }

    @Test
    public void testIfExpressionReferencesOwnBlock() {
        CompileError error = CompileError.ifExpressionReferencesOwnBlock(NODE);
        assertEquals(CompileError.ERROR_IF_EXPRESSION_REFERENCES_OWN_BLOCK, error.errorCode);
        assertTrue(error.msg.contains("If expression"));
    }

    @Test
    public void testDeclarationReferencesItself() {
        CompileError error = CompileError.declarationReferencesItself(NODE);
        assertEquals(CompileError.ERROR_DECLARATION_REFERENCES_ITSELF, error.errorCode);
        assertTrue(error.msg.contains("references itself"));
    }

    @Test
    public void testMacroArgumentsDoNotMatch() {
        CompileError error = CompileError.macroArgumentsDoNotMatch(NODE);
        assertEquals(CompileError.ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH, error.errorCode);
        assertTrue(error.msg.contains("arguments"));
    }

    @Test
    public void testExpressionIsBiggerThanExpected() {
        CompileError error = CompileError.expressionIsBiggerThanExpected(NODE, 1, 2);
        assertEquals(CompileError.ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED, error.errorCode);
        assertTrue(error.msg.contains("2 bytes"));
        assertTrue(error.msg.contains("1 byte"));
    }

    @Test
    public void testValueMustBePositive() {
        CompileError error = CompileError.valueMustBePositive(NODE);
        assertEquals(CompileError.ERROR_VALUE_MUST_BE_POSITIVE, error.errorCode);
        assertTrue(error.msg.contains("positive"));
    }

    @Test
    public void testValueOutOfBoundsMinMax() {
        CompileError error = CompileError.valueOutOfBounds(NODE, 0, 255);
        assertEquals(CompileError.ERROR_VALUE_OUT_OF_BOUNDS, error.errorCode);
        assertTrue(error.msg.contains("0"));
        assertTrue(error.msg.contains("255"));
    }

    @Test
    public void testValueOutOfBoundsAllowedValues() {
        CompileError error = CompileError.valueOutOfBounds(NODE, Set.of(0, 8, 16, 24));
        assertEquals(CompileError.ERROR_VALUE_OUT_OF_BOUNDS, error.errorCode);
        assertTrue(error.msg.contains("0x0"));
        assertTrue(error.msg.contains("0x8"));
        assertTrue(error.msg.contains("0x10"));
        assertTrue(error.msg.contains("0x18"));
    }

    @Test
    public void testToString() {
        CompileError error = CompileError.notDefined(NODE, "x");
        String result = error.toString();
        assertNotNull(result);
        assertTrue(result.contains("CompileError"));
        assertTrue(result.contains("Not defined: x"));
    }
}

