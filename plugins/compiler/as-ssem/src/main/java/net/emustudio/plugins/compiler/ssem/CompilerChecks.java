/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import org.antlr.v4.runtime.Token;

import java.util.function.Function;

public class CompilerChecks {

    public static void checkStartLineDefined(boolean defined, SourceCodePosition position, int startLine) {
        if (defined) {
            throw new CompileException(position, "Start line is already defined (at line " + startLine + ")!");
        }
    }

    public static void checkLineOutOfBounds(SourceCodePosition position, int line) {
        if (line < 0 || line > 31) {
            throw new CompileException(position, "Line number is out of bounds <0;31>: " + line);
        }
    }

    public static void checkDuplicateLineDefinition(boolean duplicate, SourceCodePosition position, int line) {
        if (duplicate) {
            throw new CompileException(position, "Duplicate line definition: " + line);
        }
    }

    public static void checkUnknownInstruction(boolean unknown, SourceCodePosition position) {
        if (unknown) {
            throw new CompileException(position, "Unrecognized instruction");
        }
    }

    public static void checkOperandOutOfBounds(SourceCodePosition position, int tokenType, long operand) {
        if (tokenType != SSEMLexer.BNUM && tokenType != SSEMLexer.NUM && (operand < 0 || operand > 31)) {
            throw new CompileException(
                    position, "Operand must be between <0, 31>; it was " + operand
            );
        }
    }

    public static <T extends Number> T checkedParseNumber(String fileName, Token token, Function<Token, T> parser) {
        try {
            return parser.apply(token);
        } catch (NumberFormatException e) {
            throw new CompileException(Position.of(fileName, token), "Could not parse number: " + token.getText());
        }
    }
}
